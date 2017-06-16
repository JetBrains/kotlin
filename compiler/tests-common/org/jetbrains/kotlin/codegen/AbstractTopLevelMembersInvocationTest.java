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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.MockLibraryUtil;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTopLevelMembersInvocationTest extends AbstractBytecodeTextTest {

    private static final String LIBRARY = "library";

    @Override
    public void doTest(@NotNull String filename) throws Exception {
        File root = new File(filename);
        List<String> sourceFiles = new ArrayList<>(2);

        FileUtil.processFilesRecursively(root, file -> {
            if (file.getName().endsWith(".kt")) {
                sourceFiles.add(relativePath(file));
                return true;
            }
            return true;
        }, file -> !LIBRARY.equals(file.getName()));

        File library = new File(root, LIBRARY);
        List<File> classPath =
                library.exists()
                ? Collections.singletonList(MockLibraryUtil.compileJvmLibraryToJar(library.getPath(), LIBRARY))
                : Collections.emptyList();

        assert !sourceFiles.isEmpty() : getTestName(true) + " should contain at least one .kt file";
        Collections.sort(sourceFiles);

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(),
                KotlinTestUtils.newConfiguration(
                        ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK,
                        CollectionsKt.plus(classPath, KotlinTestUtils.getAnnotationsJar()), Collections.emptyList()
                ),
                EnvironmentConfigFiles.JVM_CONFIG_FILES);

        loadFiles(ArrayUtil.toStringArray(sourceFiles));

        List<OccurrenceInfo> expected = readExpectedOccurrences(KotlinTestUtils.getTestDataPathBase() + "/codegen/" + sourceFiles.get(0));
        String actual = generateToText();
        Companion.checkGeneratedTextAgainstExpectedOccurrences(actual, expected);
    }
}
