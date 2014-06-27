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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.*;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTopLevelMembersInvocationTest extends AbstractBytecodeTextTest {

    private static final String LIBRARY = "library";

    @Override
    public void doTest(@NotNull String filename) throws Exception {
        File root = new File(filename);
        final List<String> sourceFiles = new ArrayList<String>(2);

        FileUtil.processFilesRecursively(root, new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.getName().endsWith(".kt")) {
                    sourceFiles.add(relativePath(file));
                    return true;
                }
                return true;
            }
        }, new Processor<File>() {
            @Override
            public boolean process(File file) {
                return !LIBRARY.equals(file.getName());
            }
        });

        File library = new File(root, LIBRARY);
        List<File> classPath = library.exists() ?
                               Collections.singletonList(MockLibraryUtil.compileLibraryToJar(library.getPath(), false)) :
                               Collections.<File>emptyList();

        assert !sourceFiles.isEmpty() : getTestName(true) + " should contain at least one .kt file";
        Collections.sort(sourceFiles);

        myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, Arrays.asList(JetTestUtils.getAnnotationsJar()), classPath));

        loadFiles(ArrayUtil.toStringArray(sourceFiles));

        List<OccurrenceInfo> expected =
                readExpectedOccurrences(JetTestCaseBuilder.getTestDataPathBase() + "/codegen/" + sourceFiles.get(0));
        countAndCompareActualOccurrences(expected);
    }
}
