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

import com.intellij.util.ArrayUtil;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.sequences.SequencesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.LanguageVersion;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.test.util.JUnit4Assertions;
import org.jetbrains.kotlin.test.util.KtTestUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.BytecodeTextUtilsKt.checkGeneratedTextAgainstExpectedOccurrences;
import static org.jetbrains.kotlin.codegen.BytecodeTextUtilsKt.readExpectedOccurrences;

public abstract class AbstractTopLevelMembersInvocationTest extends AbstractBytecodeTextTest {
    @Override
    public void doTest(@NotNull String filename) {
        File root = new File(filename);
        List<String> sourceFiles = SequencesKt.toList(SequencesKt.map(
                SequencesKt.filter(FilesKt.walkTopDown(root).maxDepth(1), File::isFile),
                this::relativePath
        ));

        File library = new File(root, "library");
        List<File> classPath =
                library.exists()
                ? Collections.singletonList(CompilerTestUtil.compileJvmLibrary(library))
                : Collections.emptyList();

        assert !sourceFiles.isEmpty() : getTestName(true) + " should contain at least one .kt file";
        Collections.sort(sourceFiles);

        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK,
                CollectionsKt.plus(classPath, KtTestUtil.getAnnotationsJar()), Collections.emptyList()
        );
        if (LanguageVersion.LATEST_STABLE.compareTo(LanguageVersion.KOTLIN_2_0) >= 0) {
            configuration.put(CommonConfigurationKeys.USE_FIR, true);
        }
        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(),
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        loadFiles(ArrayUtil.toStringArray(sourceFiles));

        List<OccurrenceInfo> expected = readExpectedOccurrences(KtTestUtil.getTestDataPathBase() + "/codegen/" + sourceFiles.get(0));
        String actual = generateToText();
        checkGeneratedTextAgainstExpectedOccurrences(actual, expected, TargetBackend.ANY, true, JUnit4Assertions.INSTANCE);
    }
}
