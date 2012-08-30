/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.PlatformTestCase;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author udalov
 */
public abstract class PositionManagerTestCase extends PlatformTestCase {

    // Breakpoint is given as a line comment on a specific line, containing the name of the class, where that line can be found.
    // This pattern matches against these line comments and saves the class name in the first group
    private static final Pattern BREAKPOINT_PATTERN = Pattern.compile("^.*//\\s*([a-zA-Z0-9._/$]*)\\s*$");

    @NotNull
    protected abstract String getTestDataPath();

    @NotNull
    protected abstract PositionManager createPositionManager(DebugProcess process, List<JetFile> files, GenerationState state);

    @Override
    protected final void setUp() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    PositionManagerTestCase.super.setUp();
                }
                catch (Exception e) {
                    ExceptionUtils.rethrow(e);
                }
            }
        });
    }

    @Override
    protected final void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    PositionManagerTestCase.super.tearDown();
                }
                catch (Exception e) {
                    ExceptionUtils.rethrow(e);
                }
            }
        });
    }

    @Override
    protected void runBareRunnable(Runnable runnable) {
        runnable.run();
    }

    protected void doTest() {
        doTest(getTestDataPath() + "/" + getTestName(true) + ".kt");
    }

    protected void doMultiTest(String... filenames) {
        for (int i = 0; i < filenames.length; i++) {
            filenames[i] = getTestDataPath() + "/" + filenames[i];
        }
        doTest(filenames);
    }

    private void doTest(String... filenames) {
        final List<JetFile> files = Lists.newArrayList();
        final List<Breakpoint> breakpoints = Lists.newArrayList();

        for (String filename : filenames) {
            File file = new File(filename);
            String fileContent;
            try {
                fileContent = FileUtil.loadFile(file);
            }
            catch (IOException e) {
                throw ExceptionUtils.rethrow(e);
            }
            final JetFile jetFile = JetTestUtils.createFile(file.getAbsolutePath(), fileContent, getProject());

            files.add(jetFile);
            breakpoints.addAll(extractBreakpointsInfo(jetFile, fileContent));
        }

        GenerationState state = GenerationUtils.compileManyFilesGetGenerationStateForTest(getProject(), files);

        Map<String, ReferenceType> referencesByName = getReferenceMap(state.getFactory());

        final DebugProcess debugProcess = createDebugProcess(referencesByName);

        final PositionManager positionManager = createPositionManager(debugProcess, files, state);

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                try {
                    for (Breakpoint breakpoint : breakpoints) {
                        SourcePosition position = SourcePosition.createFromLine(breakpoint.file, breakpoint.lineNumber);
                        assertPositionIsValid(position, breakpoint.className, positionManager);
                    }
                }
                catch (NoDataException e) {
                    ExceptionUtils.rethrow(e);
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
                // Line breakpoint numbers are 1-based
                breakpoints.add(new Breakpoint(file, i + 1, matcher.group(1)));
            }
        }

        return breakpoints;
    }

    private static Map<String, ReferenceType> getReferenceMap(ClassFileFactory classFileFactory) {
        Map<String, ReferenceType> referencesByName = Maps.newHashMap();
        for (String classFileName : classFileFactory.files()) {
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

    private static void assertPositionIsValid(SourcePosition position, String className, PositionManager positionManager) throws NoDataException {
        List<ReferenceType> classes = positionManager.getAllClasses(position);
        assertNotNull(classes);
        assertEquals(1, classes.size());
        ReferenceType type = classes.get(0);
        assertEquals(className, type.name());
    }

    private static class Breakpoint {
        private final JetFile file;
        private final int lineNumber;
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
