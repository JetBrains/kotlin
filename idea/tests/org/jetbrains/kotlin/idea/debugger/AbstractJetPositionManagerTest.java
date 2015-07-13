/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.PsiTestUtil;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractJetPositionManagerTest extends KotlinMultiFileTestCase {
    // Breakpoint is given as a line comment on a specific line, containing the regexp to match the name of the class where that line
    // can be found. This pattern matches against these line comments and saves the class name in the first group
    private static final Pattern BREAKPOINT_PATTERN = Pattern.compile("^.*//\\s*(.+)\\s*$");

    @NotNull
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    private DebugProcessImpl debugProcess;

    @NotNull
    @Override
    protected String getTestRoot() {
        return "/debugger/positionManager/";
    }

    @NotNull
    private static JetPositionManager createPositionManager(
            @NotNull DebugProcess process,
            @NotNull List<JetFile> files,
            @NotNull GenerationState state
    ) {
        JetPositionManager positionManager = (JetPositionManager) new JetPositionManagerFactory().createPositionManager(process);
        assertNotNull(positionManager);

        for (JetFile file : files) {
            positionManager.addTypeMapper(file, state.getTypeMapper());
        }

        return positionManager;
    }

    protected void doTest(@NotNull String fileName) throws Exception {
        if (fileName.endsWith(".kt")) {
            String path = KotlinPackage.substringAfter(fileName, PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE.substring(1), fileName);
            configureByFile(path);
        }
        else {
            VirtualFile rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, fileName, myFilesToDelete, false);
            prepareProject(rootDir);
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        }

        performTest();
    }

    private void performTest() {
        Project project = getProject();
        List<JetFile> files = new ArrayList<JetFile>(PluginJetFilesProvider.allFilesInProject(project));

        final List<Breakpoint> breakpoints = Lists.newArrayList();
        for (JetFile file : files) {
            breakpoints.addAll(extractBreakpointsInfo(file, file.getText()));
        }

        GenerationState state = GenerationUtils.compileManyFilesGetGenerationStateForTest(project, files);

        Map<String, ReferenceType> referencesByName = getReferenceMap(state.getFactory());

        debugProcess = createDebugProcess(referencesByName);

        final PositionManager positionManager = createPositionManager(debugProcess, files, state);

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Breakpoint breakpoint : breakpoints) {
                        assertBreakpointIsHandledCorrectly(breakpoint, positionManager);
                    }
                }
                catch (NoDataException e) {
                    throw UtilsPackage.rethrow(e);
                }
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

    private static Collection<Breakpoint> extractBreakpointsInfo(JetFile file, String fileContent) {
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
        Map<String, ReferenceType> referencesByName = Maps.newHashMap();
        for (OutputFile outputFile : outputFiles.asList()) {
            String classFileName = outputFile.getRelativePath();
            String name = classFileName.substring(0, classFileName.lastIndexOf('.'));
            referencesByName.put(name, new MockReferenceType(name));
        }
        return referencesByName;
    }

    private DebugProcessEvents createDebugProcess(final Map<String, ReferenceType> referencesByName) {
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
        };
    }

    private static void assertBreakpointIsHandledCorrectly(Breakpoint breakpoint, PositionManager positionManager) throws NoDataException {
        SourcePosition position = SourcePosition.createFromLine(breakpoint.file, breakpoint.lineNumber);
        List<ReferenceType> classes = positionManager.getAllClasses(position);
        assertNotNull(classes);
        assertEquals(1, classes.size());
        ReferenceType type = classes.get(0);
        assertTrue("Type name " + type.name() + " doesn't match " + breakpoint.classNameRegexp + " for line " + (breakpoint.lineNumber + 1),
                   type.name().matches(breakpoint.classNameRegexp));

        // JDI names are of form "package.Class$InnerClass"
        ReferenceType typeWithFqName = new MockReferenceType(type.name().replace('/', '.'));
        Location location = new MockLocation(typeWithFqName, breakpoint.file.getName(), breakpoint.lineNumber + 1);

        SourcePosition actualPosition = positionManager.getSourcePosition(location);
        assertNotNull(actualPosition);
        assertEquals(position.getFile(), actualPosition.getFile());
        assertEquals(position.getLine(), actualPosition.getLine());
    }

    private static class Breakpoint {
        private final JetFile file;
        private final int lineNumber; // 0-based
        private final String classNameRegexp;

        private Breakpoint(JetFile file, int lineNumber, String classNameRegexp) {
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
            return new ArrayList<ReferenceType>(referencesByName.values());
        }

        @Override
        public List<ReferenceType> classesByName(String name) {
            return UtilsPackage.emptyOrSingletonList(referencesByName.get(name));
        }
    }
}
