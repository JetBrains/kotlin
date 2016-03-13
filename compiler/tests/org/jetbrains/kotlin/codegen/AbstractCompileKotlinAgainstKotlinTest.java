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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public abstract class AbstractCompileKotlinAgainstKotlinTest extends CodegenTestCase {
    private File tmpdir;
    private File aDir;
    private File bDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tmpdir = KotlinTestUtils.tmpDirForTest(this);
        aDir = new File(tmpdir, "a");
        bDir = new File(tmpdir, "b");
        KotlinTestUtils.mkdirs(aDir);
        KotlinTestUtils.mkdirs(bDir);
    }

    @Override
    protected void doMultiFileTest(@NotNull File wholeFile, @NotNull List<TestFile> files, @Nullable File javaFilesDir) throws Exception {
        assert javaFilesDir == null : ".java files are not supported yet in this test";
        doTwoFileTest(files);
    }

    @NotNull
    protected Pair<ClassFileFactory, ClassFileFactory> doTwoFileTest(@NotNull List<TestFile> files) throws Exception {
        // Note that it may be beneficial to improve this test to handle many files, compiling them successively against all previous
        assert files.size() == 2 : "There should be exactly two files in this test";
        TestFile fileA = files.get(0);
        TestFile fileB = files.get(1);
        ClassFileFactory factoryA = compileA(fileA.name, fileA.content);
        ClassFileFactory factoryB = null;
        try {
            factoryB = compileB(fileB.name, fileB.content);
            invokeBox(PackagePartClassUtils.getFilePartShortName(new File(fileB.name).getName()));
        }
        catch (Throwable e) {
            String result = "FIRST: \n\n" + factoryA.createText();
            if (factoryB != null) {
                result += "\n\nSECOND: \n\n" + factoryB.createText();
            }
            System.out.println(result);
            throw ExceptionUtilsKt.rethrow(e);
        }
        return new Pair<ClassFileFactory, ClassFileFactory>(factoryA, factoryB);
    }

    protected void invokeBox(@NotNull String className) throws Exception {
        Method box = createGeneratedClassLoader().loadClass(className).getMethod("box");
        String result = (String) box.invoke(null);
        assertEquals("OK", result);
    }

    @NotNull
    private URLClassLoader createGeneratedClassLoader() throws Exception {
        return new URLClassLoader(
                new URL[]{ bDir.toURI().toURL(), aDir.toURI().toURL() },
                ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
        );
    }

    @NotNull
    protected ClassFileFactory compileA(@NotNull String fileName, @NotNull String content) throws IOException {
        KotlinCoreEnvironment environment =
                KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(), ConfigurationKind.ALL);
        return compileKotlin(fileName, content, aDir, environment, getTestRootDisposable());
    }

    @NotNull
    protected ClassFileFactory compileB(@NotNull String fileName, @NotNull String content) throws IOException {
        CompilerConfiguration configurationWithADirInClasspath = KotlinTestUtils
                .compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, KotlinTestUtils.getAnnotationsJar(), aDir);

        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configurationWithADirInClasspath, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        return compileKotlin(fileName, content, bDir, environment, getTestRootDisposable());
    }

    @NotNull
    private ClassFileFactory compileKotlin(
            @NotNull String fileName, @NotNull String content, @NotNull File outputDir, @NotNull KotlinCoreEnvironment environment,
            @NotNull Disposable disposable
    ) throws IOException {
        KtFile psiFile = KotlinTestUtils.createFile(fileName, content, environment.getProject());

        ModuleVisibilityManager.SERVICE.getInstance(environment.getProject()).addModule(new ModuleBuilder("module for test", tmpdir.getAbsolutePath(), "test"));

        ClassFileFactory outputFiles = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile, environment);

        OutputUtilsKt.writeAllTo(outputFiles, outputDir);

        Disposer.dispose(disposable);
        return outputFiles;
    }
}
