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

package org.jetbrains.kotlin.checkers;

import com.google.common.io.Files;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetLiteFixture;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class KotlinMultiFileTestWithWithJava<M, F> extends JetLiteFixture {
    protected class ModuleAndDependencies {
        final M module;
        final List<String> dependencies;

        ModuleAndDependencies(M module, List<String> dependencies) {
            this.module = module;
            this.dependencies = dependencies;
        }
    }

    protected static boolean writeSourceFile(@NotNull String fileName, @NotNull String content, @NotNull File targetDir) {
        try {
            File sourceFile = new File(targetDir, fileName);
            JetTestUtils.mkdirs(sourceFile.getParentFile());
            Files.write(content, sourceFile, Charset.forName("utf-8"));
            return true;
        }
        catch (Exception e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        File javaFilesDir = createJavaFilesDir();
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_AND_ANNOTATIONS,
                TestJdkKind.MOCK_JDK,
                Arrays.asList(JetTestUtils.getAnnotationsJar()),
                Arrays.asList(javaFilesDir)
        );
        File kotlinSourceRoot = getKotlinSourceRoot();
        if (kotlinSourceRoot != null) {
            configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, kotlinSourceRoot.getPath());
        }
        return createEnvironment(getTestRootDisposable(), configuration);
    }

    @NotNull
    protected JetCoreEnvironment createEnvironment(@NotNull Disposable disposable, @NotNull CompilerConfiguration configuration) {
        return JetCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    @Nullable
    protected File getKotlinSourceRoot() {
        return null;
    }

    protected File createJavaFilesDir() {
        return createTmpDir("java-files");
    }

    protected static File createTmpDir(String dirName) {
        File dir = new File(FileUtil.getTempDirectory(), dirName);
        try {
            JetTestUtils.mkdirs(dir);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
        return dir;
    }

    protected void doTest(String filePath) throws Exception {
        File file = new File(filePath);
        final File javaFilesDir = createJavaFilesDir();
        final File kotlinFilesDir = getKotlinSourceRoot();

        String expectedText = JetTestUtils.doLoadFile(file);

        final Map<String, ModuleAndDependencies> modules = new HashMap<String, ModuleAndDependencies>();

        List<F> testFiles =
                JetTestUtils.createTestFiles(file.getName(), expectedText, new JetTestUtils.TestFileFactory<M, F>() {
                    @Override
                    public F createFile(
                            @Nullable M module,
                            @NotNull String fileName,
                            @NotNull String text,
                            @NotNull Map<String, String> directives
                    ) {
                        if (fileName.endsWith(".java")) {
                            writeSourceFile(fileName, text, javaFilesDir);
                        }

                        if (fileName.endsWith(".kt") && kotlinFilesDir != null) {
                            writeSourceFile(fileName, text, kotlinFilesDir);
                        }

                        return createTestFile(module, fileName, text, directives);
                    }

                    @Override
                    public M createModule(@NotNull String name, @NotNull List<String> dependencies) {
                        M module = createTestModule(name);
                        ModuleAndDependencies oldValue = modules.put(name, new ModuleAndDependencies(module, dependencies));
                        assert oldValue == null : "Module " + name + " declared more than once";

                        return module;
                    }
                });

        doMultiFileTest(file, modules, testFiles);
    }

    protected abstract M createTestModule(@NotNull String name);

    protected abstract F createTestFile(M module, String fileName, String text, Map<String, String> directives);

    protected abstract void doMultiFileTest(File file, Map<String, ModuleAndDependencies> modules, List<F> files) throws Exception;
}
