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

import com.intellij.openapi.util.io.FileUtil;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.ContentRootsKt;
import org.jetbrains.kotlin.script.StandardScriptDefinition;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class KotlinMultiFileTestWithJava<M, F> extends KotlinTestWithEnvironment {
    private File javaFilesDir;
    private File kotlinSourceRoot;

    public class ModuleAndDependencies {
        final M module;
        final List<String> dependencies;

        ModuleAndDependencies(M module, List<String> dependencies) {
            this.module = module;
            this.dependencies = dependencies;
        }
    }

    @Override
    protected KotlinCoreEnvironment createEnvironment() throws Exception {
        // TODO: do not create temporary directory for tests without Java sources
        javaFilesDir = KotlinTestUtils.tmpDir("java-files");
        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(
                getConfigurationKind(),
                getTestJdkKind(),
                CollectionsKt.plus(Collections.singletonList(KotlinTestUtils.getAnnotationsJar()), getExtraClasspath()),
                Collections.singletonList(javaFilesDir)
        );
        configuration.add(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY, StandardScriptDefinition.INSTANCE);
        if (isKotlinSourceRootNeeded()) {
            kotlinSourceRoot = KotlinTestUtils.tmpDir("kotlin-src");
            ContentRootsKt.addKotlinSourceRoot(configuration, kotlinSourceRoot.getPath());
        }
        return KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, getEnvironmentConfigFiles());
    }

    @Override
    protected void removeEnvironment() throws Exception {
        if (javaFilesDir != null) FileUtil.delete(javaFilesDir);
        if (kotlinSourceRoot != null) FileUtil.delete(kotlinSourceRoot);
    }

    @NotNull
    protected ConfigurationKind getConfigurationKind() {
        return ConfigurationKind.MOCK_RUNTIME;
    }

    @NotNull
    protected TestJdkKind getTestJdkKind() {
        return TestJdkKind.MOCK_JDK;
    }

    @NotNull
    protected List<File> getExtraClasspath() {
        return Collections.emptyList();
    }

    @NotNull
    protected List<String> getEnvironmentConfigFiles() {
        return EnvironmentConfigFiles.JVM_CONFIG_FILES;
    }

    protected boolean isKotlinSourceRootNeeded() {
        return false;
    }

    protected void doTest(String filePath) throws Exception {
        File file = new File(filePath);

        String expectedText = KotlinTestUtils.doLoadFile(file);

        final Map<String, ModuleAndDependencies> modules = new HashMap<String, ModuleAndDependencies>();

        List<F> testFiles =
                KotlinTestUtils.createTestFiles(file.getName(), expectedText, new KotlinTestUtils.TestFileFactory<M, F>() {
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

                        if (fileName.endsWith(".kt") && kotlinSourceRoot != null) {
                            writeSourceFile(fileName, text, kotlinSourceRoot);
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

                    private void writeSourceFile(@NotNull String fileName, @NotNull String content, @NotNull File targetDir) {
                        File file = new File(targetDir, fileName);
                        KotlinTestUtils.mkdirs(file.getParentFile());
                        FilesKt.writeText(file, content, Charsets.UTF_8);
                    }
                });

        doMultiFileTest(file, modules, testFiles);
    }

    protected abstract M createTestModule(@NotNull String name);

    protected abstract F createTestFile(M module, String fileName, String text, Map<String, String> directives);

    protected abstract void doMultiFileTest(File file, Map<String, ModuleAndDependencies> modules, List<F> files) throws Exception;
}
