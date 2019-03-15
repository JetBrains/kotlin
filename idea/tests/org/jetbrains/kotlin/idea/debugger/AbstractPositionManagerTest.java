/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger;

import com.google.common.collect.Lists;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.jvm.functions.Function1;
import kotlin.sequences.SequencesKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.codegen.ClassBuilderFactories;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseKt;
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractPositionManagerTest extends KotlinLightCodeInsightFixtureTestCase {
    // Breakpoint is given as a line comment on a specific line, containing the regexp to match the name of the class where that line
    // can be found. This pattern matches against these line comments and saves the class name in the first group
    private static final Pattern BREAKPOINT_PATTERN = Pattern.compile("^.*//\\s*(.+)\\s*$");

    @NotNull
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/debugger/positionManager/";
    }

    @Override
    public void setUp() {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase());
    }

    private DebugProcessImpl debugProcess;

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    @NotNull
    private static KotlinPositionManager createPositionManager(
            @NotNull DebugProcess process,
            @NotNull List<KtFile> files,
            @NotNull GenerationState state
    ) {
        KotlinPositionManager positionManager = (KotlinPositionManager) new KotlinPositionManagerFactory().createPositionManager(process);
        assertNotNull(positionManager);

        for (KtFile file : files) {
            KotlinDebuggerCaches.Companion.addTypeMapper(file, state.getTypeMapper());
        }

        return positionManager;
    }

    protected void doTest(@NotNull String fileName) throws Exception {
        if (fileName.endsWith(".kt")) {
            String path = getPath(fileName);
            myFixture.configureByFile(path);
        }
        else {
            String path = getPath(fileName);
            SequencesKt.forEach(FilesKt.walkTopDown(new File(path)), new Function1<File, Unit>() {
                @Override
                public Unit invoke(File file) {
                    String fileName = file.getName();
                    String path = getPath(fileName);
                    myFixture.configureByFile(path);
                    return null;
                }
            });
        }

        performTest();
    }

    @NotNull
    private static String getPath(@NotNull String fileName) {
        return StringsKt.substringAfter(fileName, PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE.substring(1), fileName);
    }

    private void performTest() {
        Project project = getProject();
        List<KtFile> files = new ArrayList<>(KotlinLightCodeInsightFixtureTestCaseKt.allKotlinFiles(project));
        if (files.isEmpty()) return;

        List<Breakpoint> breakpoints = Lists.newArrayList();
        for (KtFile file : files) {
            breakpoints.addAll(extractBreakpointsInfo(file, file.getText()));
        }

        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK);
        // TODO: delete this once IDEVirtualFileFinder supports loading .kotlin_builtins files
        configuration.put(JVMConfigurationKeys.ADD_BUILT_INS_FROM_COMPILER_TO_DEPENDENCIES, true);

        GenerationState state =
                GenerationUtils.compileFiles(files, configuration, ClassBuilderFactories.TEST, scope -> PackagePartProvider.Empty.INSTANCE);

        Map<String, ReferenceType> referencesByName = getReferenceMap(state.getFactory());

        debugProcess = createDebugProcess(referencesByName);

        PositionManager positionManager = createPositionManager(debugProcess, files, state);

        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                for (Breakpoint breakpoint : breakpoints) {
                    assertBreakpointIsHandledCorrectly(breakpoint, positionManager);
                }
            }
            catch (NoDataException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }
        });

    }

    @Override
    public void tearDown() {
        if (debugProcess != null) {
            debugProcess.stop(true);
            debugProcess.dispose();
            debugProcess = null;
        }
        super.tearDown();
    }

    private static Collection<Breakpoint> extractBreakpointsInfo(KtFile file, String fileContent) {
        Collection<Breakpoint> breakpoints = Lists.newArrayList();
        String[] lines = StringUtil.convertLineSeparators(fileContent).split("\n");

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = BREAKPOINT_PATTERN.matcher(lines[i]);
            if (matcher.matches()) {
                breakpoints.add(new Breakpoint(file, i, matcher.group(1)));
            }
        }

        return breakpoints;
    }

    private static Map<String, ReferenceType> getReferenceMap(OutputFileCollection outputFiles) {
        return new SmartMockReferenceTypeContext(outputFiles).getReferenceTypesByName();
    }

    private DebugProcessEvents createDebugProcess(Map<String, ReferenceType> referencesByName) {
        return new DebugProcessEvents(getProject()) {
            private VirtualMachineProxyImpl virtualMachineProxy;

            @NotNull
            @Override
            public VirtualMachineProxyImpl getVirtualMachineProxy() {
                if (virtualMachineProxy == null) {
                    virtualMachineProxy = new MockVirtualMachineProxy(this, referencesByName);
                }
                return virtualMachineProxy;
            }

            @NotNull
            @Override
            public GlobalSearchScope getSearchScope() {
                return GlobalSearchScope.allScope(getProject());
            }
        };
    }

    private static void assertBreakpointIsHandledCorrectly(Breakpoint breakpoint, PositionManager positionManager) throws NoDataException {
        SourcePosition position = SourcePosition.createFromLine(breakpoint.file, breakpoint.lineNumber);
        List<ReferenceType> classes = positionManager.getAllClasses(position);
        assertNotNull(classes);
        assertFalse("Classes not found for line " + (breakpoint.lineNumber + 1) + ", expected " + breakpoint.classNameRegexp,
                    classes.isEmpty());

        if (classes.stream().noneMatch(clazz -> clazz.name().matches(breakpoint.classNameRegexp))) {
            throw new AssertionError("Breakpoint class '" + breakpoint.classNameRegexp +
                                     "' from line " + (breakpoint.lineNumber + 1) + " was not found in the PositionManager classes names: " +
                                     classes.stream().map(ReferenceType::name).collect(Collectors.joining(",")));
        }

        ReferenceType typeWithFqName = classes.get(0);
        Location location = new MockLocation(typeWithFqName, breakpoint.file.getName(), breakpoint.lineNumber + 1);

        SourcePosition actualPosition = positionManager.getSourcePosition(location);
        assertNotNull(actualPosition);
        assertEquals(position.getFile(), actualPosition.getFile());
        assertEquals(position.getLine(), actualPosition.getLine());
    }

    private static class Breakpoint {
        private final KtFile file;
        private final int lineNumber; // 0-based
        private final String classNameRegexp;

        private Breakpoint(KtFile file, int lineNumber, String classNameRegexp) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.classNameRegexp = classNameRegexp;
        }
    }

    private static class MockVirtualMachineProxy extends VirtualMachineProxyImpl {
        private final Map<String, ReferenceType> referencesByName;

        private MockVirtualMachineProxy(DebugProcessEvents debugProcess, Map<String, ReferenceType> referencesByName) {
            super(debugProcess, new MockVirtualMachine());
            this.referencesByName = referencesByName;
        }

        @Override
        public List<ReferenceType> allClasses() {
            return new ArrayList<>(referencesByName.values());
        }

        @Override
        public List<ReferenceType> classesByName(String name) {
            return CollectionsKt.listOfNotNull(referencesByName.get(name));
        }
    }
}
