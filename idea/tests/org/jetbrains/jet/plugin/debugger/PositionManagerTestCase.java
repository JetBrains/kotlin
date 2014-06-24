/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.testFramework.PsiTestUtil;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.stubindex.JetAllPackagesIndex;
import org.jetbrains.jet.utils.UtilsPackage;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PositionManagerTestCase extends MultiFileTestCase {

    // Breakpoint is given as a line comment on a specific line, containing the name of the class, where that line can be found.
    // This pattern matches against these line comments and saves the class name in the first group
    private static final Pattern BREAKPOINT_PATTERN = Pattern.compile("^.*//\\s*([a-zA-Z0-9._/$]*)\\s*$");

    @NotNull
    protected abstract String getTestDataPath();

    @NotNull
    protected abstract PositionManager createPositionManager(DebugProcess process, List<JetFile> files, GenerationState state);

    protected void doTest() {
        String path = getTestRoot() + getTestName(true) + ".kt";
        try {
            configureByFile(path);
        }
        catch (Exception e) {
            UtilsPackage.rethrow(e);
        }
        performTest();
    }

    protected void doMultiTest() {
        String path = getTestDataPath() + getTestRoot() + getTestName(true);
        try {
            VirtualFile rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, path, myFilesToDelete, false);
            prepareProject(rootDir);
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        }
        catch (Exception e) {
            UtilsPackage.rethrow(e);
        }
        performTest();
    }

    private void performTest() {
        Project project = getProject();
        List<JetFile> files = new ArrayList<JetFile>(
                JetAllPackagesIndex.getInstance().get(FqName.ROOT.asString(), project, GlobalSearchScope.allScope(project))
        );

        final List<Breakpoint> breakpoints = Lists.newArrayList();
        for (JetFile file : files) {
            breakpoints.addAll(extractBreakpointsInfo(file, file.getText()));
        }

        GenerationState state = GenerationUtils.compileManyFilesGetGenerationStateForTest(project, files);

        Map<String, ReferenceType> referencesByName = getReferenceMap(state.getFactory());

        DebugProcess debugProcess = createDebugProcess(referencesByName);

        final PositionManager positionManager = createPositionManager(debugProcess, files, state);

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                try {
                    for (Breakpoint breakpoint : breakpoints) {
                        assertBreakpointIsHandledCorrectly(breakpoint, positionManager);
                    }
                }
                catch (NoDataException e) {
                    UtilsPackage.rethrow(e);
                }
            }
        });
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
        DebugProcessEvents events = new DebugProcessEvents(getProject()) {
            private VirtualMachineProxyImpl virtualMachineProxy;

            @Override
            public VirtualMachineProxyImpl getVirtualMachineProxy() {
                if (virtualMachineProxy == null) {
                    virtualMachineProxy = new MockVirtualMachineProxy(this, referencesByName);
                }
                return virtualMachineProxy;
            }
        };

        return events;
    }

    private static void assertBreakpointIsHandledCorrectly(Breakpoint breakpoint, PositionManager positionManager) throws NoDataException {
        SourcePosition position = SourcePosition.createFromLine(breakpoint.file, breakpoint.lineNumber);
        List<ReferenceType> classes = positionManager.getAllClasses(position);
        assertNotNull(classes);
        assertEquals(1, classes.size());
        ReferenceType type = classes.get(0);
        if (!breakpoint.className.contains("$src$")) // don't want to deal with hashCodes in test
            assertEquals(breakpoint.className, type.name());
        else
            assertTrue(type.name().startsWith(breakpoint.className));

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
        private final String className;

        private Breakpoint(JetFile file, int lineNumber, String className) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.className = className;
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
            ReferenceType ref = referencesByName.get(name);
            if (ref == null) {
                return Collections.emptyList();
            }
            else {
                return Collections.singletonList(ref);
            }
        }
    }
}
