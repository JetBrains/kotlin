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

package org.jetbrains.kotlin.jvm.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.codegen.ClassFileFactory;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public abstract class AbstractCompileKotlinAgainstKotlinTest extends TestCaseWithTmpdir {
    private File aDir;
    private File bDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aDir = new File(tmpdir, "a");
        bDir = new File(tmpdir, "b");
        JetTestUtils.mkdirs(aDir);
        JetTestUtils.mkdirs(bDir);
    }

    public void doTest(@NotNull String fileName) throws Exception {
        compileA(new File(fileName));
        compileB(new File(fileName.replaceFirst("A\\.kt$", "B.kt")));
        invokeMain();
    }


    private void invokeMain() throws Exception {
        Method main = generatedClass().getMethod("main", String[].class);
        main.invoke(null, new Object[] {ArrayUtil.EMPTY_STRING_ARRAY});
    }

    protected void invokeBox() throws Exception {
        Method box = generatedClass().getMethod("box");
        String result = (String) box.invoke(null);
        assertEquals("OK", result);
    }

    @NotNull
    private Class<?> generatedClass() throws Exception {
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ bDir.toURI().toURL(), aDir.toURI().toURL() },
                ForTestCompileRuntime.runtimeJarClassLoader()
        );
        return classLoader.loadClass(PackageClassUtils.getPackageClassName(FqName.ROOT));
    }

    protected ClassFileFactory compileA(@NotNull File ktAFile) throws IOException {
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(),
                                                                                                            ConfigurationKind.ALL);
        return compileKotlin(ktAFile, aDir, jetCoreEnvironment, getTestRootDisposable());
    }

    protected ClassFileFactory compileB(@NotNull File ktBFile) throws IOException {
        CompilerConfiguration configurationWithADirInClasspath = JetTestUtils
                .compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), aDir);

        JetCoreEnvironment environment =
                JetCoreEnvironment.createForTests(getTestRootDisposable(), configurationWithADirInClasspath, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        return compileKotlin(ktBFile, bDir, environment, getTestRootDisposable());
    }

    private static ClassFileFactory compileKotlin(
            @NotNull File file, @NotNull File outputDir, @NotNull JetCoreEnvironment jetCoreEnvironment,
            @NotNull Disposable disposable
    ) throws IOException {

        String text = FileUtil.loadFile(file, true);

        JetFile psiFile = JetTestUtils.createFile(file.getName(), text, jetCoreEnvironment.getProject());

        ClassFileFactory outputFiles = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        OutputUtilsPackage.writeAllTo(outputFiles, outputDir);

        Disposer.dispose(disposable);
        return outputFiles;
    }
}
