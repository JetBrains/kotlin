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
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.ContentRootsKt;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.script.StandardScriptDefinition;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase;

import java.io.File;
import java.util.*;

public abstract class KotlinMultiFileTestWithJava<M, F> extends KtUsefulTestCase {
    protected File javaFilesDir;
    private File kotlinSourceRoot;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // TODO: do not create temporary directory for tests without Java sources
        javaFilesDir = KotlinTestUtils.tmpDir("java-files");
        if (isKotlinSourceRootNeeded()) {
            kotlinSourceRoot = KotlinTestUtils.tmpDir("kotlin-src");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (javaFilesDir != null) FileUtil.delete(javaFilesDir);
        if (kotlinSourceRoot != null) FileUtil.delete(kotlinSourceRoot);
        super.tearDown();
    }

    public class ModuleAndDependencies {
        final M module;
        final List<String> dependencies;
        final List<String> friends;

        ModuleAndDependencies(M module, List<String> dependencies, List<String> friends) {
            this.module = module;
            this.dependencies = dependencies;
            this.friends = friends;
        }
    }

    @NotNull
    protected KotlinCoreEnvironment createEnvironment(@NotNull File file) {
        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(
                getConfigurationKind(),
                getTestJdkKind(file),
                getClasspath(file),
                isJavaSourceRootNeeded() ? Collections.singletonList(javaFilesDir) : Collections.emptyList()
        );
        configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, StandardScriptDefinition.INSTANCE);
        if (isKotlinSourceRootNeeded()) {
            ContentRootsKt.addKotlinSourceRoot(configuration, kotlinSourceRoot.getPath());
        }

        performCustomConfiguration(configuration);
        return KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, getEnvironmentConfigFiles());
    }

    protected boolean isJavaSourceRootNeeded() {
        return true;
    }

    protected void performCustomConfiguration(@NotNull CompilerConfiguration configuration) {

    }

    @NotNull
    protected ConfigurationKind getConfigurationKind() {
        return ConfigurationKind.JDK_ONLY;
    }

    @NotNull
    protected TestJdkKind getTestJdkKind(@NotNull File file) {
        return InTextDirectivesUtils.isDirectiveDefined(FilesKt.readText(file, Charsets.UTF_8), "FULL_JDK")
               ? TestJdkKind.FULL_JDK
               : TestJdkKind.MOCK_JDK;
    }

    private List<File> getClasspath(File file) {
        List<File> result = new ArrayList<>();
        result.add(KotlinTestUtils.getAnnotationsJar());
        result.addAll(getExtraClasspath());

        boolean loadAndroidAnnotations = InTextDirectivesUtils.isDirectiveDefined(
                FilesKt.readText(file, Charsets.UTF_8), "ANDROID_ANNOTATIONS"
        );

        if (loadAndroidAnnotations) {
            result.add(ForTestCompileRuntime.androidAnnotationsForTests());
        }

        return result;
    }

    @NotNull
    protected List<File> getExtraClasspath() {
        return Collections.emptyList();
    }

    @NotNull
    protected EnvironmentConfigFiles getEnvironmentConfigFiles() {
        return EnvironmentConfigFiles.JVM_CONFIG_FILES;
    }

    protected boolean isKotlinSourceRootNeeded() {
        return false;
    }

    protected void doTest(String filePath) throws Exception {
        File file = new File(filePath);
        String expectedText = KotlinTestUtils.doLoadFile(file);
        Map<String, ModuleAndDependencies> modules = new HashMap<>();
        List<F> testFiles = createTestFiles(file, expectedText, modules);

        doMultiFileTest(file, modules, testFiles);
    }

    protected abstract M createTestModule(@NotNull String name);

    protected abstract F createTestFile(M module, String fileName, String text, Map<String, String> directives);

    protected abstract void doMultiFileTest(File file, Map<String, ModuleAndDependencies> modules, List<F> files) throws Exception;

    protected List<F> createTestFiles(File file, String expectedText, Map<String, ModuleAndDependencies> modules) {
        return KotlinTestUtils.createTestFiles(file.getName(), expectedText, new KotlinTestUtils.TestFileFactory<M, F>() {
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

                if ((fileName.endsWith(".kt") || fileName.endsWith(".kts")) && kotlinSourceRoot != null) {
                    writeSourceFile(fileName, text, kotlinSourceRoot);
                }

                return createTestFile(module, fileName, text, directives);
            }

            @Override
            public M createModule(@NotNull String name, @NotNull List<String> dependencies, @NotNull List<String> friends) {
                M module = createTestModule(name);
                ModuleAndDependencies oldValue = modules.put(name, new ModuleAndDependencies(module, dependencies, friends));
                assert oldValue == null : "Module " + name + " declared more than once";

                return module;
            }

            private void writeSourceFile(@NotNull String fileName, @NotNull String content, @NotNull File targetDir) {
                File file = new File(targetDir, fileName);
                KotlinTestUtils.mkdirs(file.getParentFile());
                FilesKt.writeText(file, content, Charsets.UTF_8);
            }
        });
    }

}
