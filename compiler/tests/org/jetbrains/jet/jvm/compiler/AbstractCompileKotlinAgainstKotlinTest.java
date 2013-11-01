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

package org.jetbrains.jet.jvm.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.test.TestCaseWithTmpdir;

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
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ aDir.toURI().toURL(), bDir.toURI().toURL() },
                AbstractCompileKotlinAgainstKotlinTest.class.getClassLoader()
        );
        Class<?> clazz = classLoader.loadClass(PackageClassUtils.getPackageClassName(FqName.ROOT));
        Method main = clazz.getMethod("main", new Class[] {String[].class});
        main.invoke(null, new Object[] {ArrayUtil.EMPTY_STRING_ARRAY});
    }

    private void compileA(@NotNull File ktAFile) throws IOException {
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(),
                                                                                                            ConfigurationKind.JDK_ONLY);
        compileKotlin(ktAFile, aDir, jetCoreEnvironment, getTestRootDisposable());
    }

    private void compileB(@NotNull File ktBFile) throws IOException {
        CompilerConfiguration configurationWithADirInClasspath = JetTestUtils
                .compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), aDir);
        compileKotlin(ktBFile, bDir, JetCoreEnvironment.createForTests(getTestRootDisposable(), configurationWithADirInClasspath),
                      getTestRootDisposable());
    }

    private static void compileKotlin(
            @NotNull File file, @NotNull File outputDir, @NotNull JetCoreEnvironment jetCoreEnvironment,
            @NotNull Disposable disposable
    ) throws IOException {

        String text = FileUtil.loadFile(file);

        JetFile psiFile = JetTestUtils.createFile(file.getName(), text, jetCoreEnvironment.getProject());

        ClassFileFactory classFileFactory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        OutputUtilsPackage.writeAllTo(classFileFactory, outputDir);

        Disposer.dispose(disposable);
    }
}
