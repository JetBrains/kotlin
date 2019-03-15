/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.config.ContentRootsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.parsing.KotlinParserDefinition;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase;

import java.io.File;
import java.util.*;

import static org.jetbrains.kotlin.script.ScriptTestUtilKt.loadScriptingPlugin;

public abstract class KotlinMultiFileTestWithJava<M, F> extends KtUsefulTestCase {
    protected File javaFilesDir;
    private File kotlinSourceRoot;
    protected String coroutinesPackage;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // TODO: do not create temporary directory for tests without Java sources
        javaFilesDir = KotlinTestUtils.tmpDir("java-files");
        if (isKotlinSourceRootNeeded()) {
            kotlinSourceRoot = KotlinTestUtils.tmpDir("kotlin-src");
        }
        coroutinesPackage = "";
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
    protected Boolean isScriptingNeeded(@NotNull File file) {
        return file.getName().endsWith(KotlinParserDefinition.STD_SCRIPT_EXT);
    }

    @NotNull
    protected KotlinCoreEnvironment createEnvironment(@NotNull File file) {
        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(
                getConfigurationKind(),
                getTestJdkKind(file),
                getClasspath(file),
                isJavaSourceRootNeeded() ? Collections.singletonList(javaFilesDir) : Collections.emptyList()
        );
        if (isScriptingNeeded(file)) {
            loadScriptingPlugin(configuration);
        }
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

        if (InTextDirectivesUtils.isDirectiveDefined(expectedText, "// SKIP_JAVAC")) return;

        Map<String, ModuleAndDependencies> modules = new HashMap<>();
        List<F> testFiles = createTestFiles(file, expectedText, modules);

        doMultiFileTest(file, modules, testFiles);
    }

    protected void doTestWithCoroutinesPackageReplacement(String filePath, String coroutinesPackage) throws Exception {
        File file = new File(filePath);
        String expectedText = KotlinTestUtils.doLoadFile(file);
        expectedText = expectedText.replace("COROUTINES_PACKAGE", coroutinesPackage);
        this.coroutinesPackage = coroutinesPackage;
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
        }, coroutinesPackage);
    }

}
