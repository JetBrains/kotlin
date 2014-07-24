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
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.*;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.InlineTestUtil;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public void doBoxTestWithInlineCheck(@NotNull String firstFileName) throws Exception {
        List<String> inputFiles = new ArrayList<String>(2);
        inputFiles.add(firstFileName);
        inputFiles.add(firstFileName.substring(0, firstFileName.length() - "1.kt".length()) + "2.kt");

        ArrayList<OutputFile> files = doBoxTest(inputFiles);
        InlineTestUtil.checkNoCallsToInline(files);
    }

    @NotNull
    private ArrayList<OutputFile> doBoxTest(@NotNull List<String> files) throws Exception {
        Collections.sort(files);

        ClassFileFactory factory1 = null;
        ClassFileFactory factory2 = null;
        try {
            factory1 = (ClassFileFactory) compileA(new File(files.get(1)));
            factory2 = (ClassFileFactory) compileB(new File(files.get(0)));
            invokeBox();
        } catch (Throwable e) {
            String result = "";
            if (factory1 != null) {
                result += "FIRST: \n\n" + factory1.createText();
            }
            if (factory2 != null) {
                result += "\n\nSECOND: \n\n" + factory2.createText();
            }
            System.out.println(result);
            throw UtilsPackage.rethrow(e);
        }

        ArrayList<OutputFile> allGeneratedFiles = new ArrayList<OutputFile>(factory1.asList());
        allGeneratedFiles.addAll(factory2.asList());
        return allGeneratedFiles;
    }

    private void invokeMain() throws Exception {
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ aDir.toURI().toURL(), bDir.toURI().toURL(), ForTestCompileRuntime.runtimeJarForTests().toURI().toURL() },
                AbstractCompileKotlinAgainstKotlinTest.class.getClassLoader()
        );
        Class<?> clazz = classLoader.loadClass(PackageClassUtils.getPackageClassName(FqName.ROOT));
        Method main = clazz.getMethod("main", new Class[] {String[].class});
        main.invoke(null, new Object[] {ArrayUtil.EMPTY_STRING_ARRAY});
    }

    private void invokeBox() throws Exception {
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ bDir.toURI().toURL(), aDir.toURI().toURL(), ForTestCompileRuntime.runtimeJarForTests().toURI().toURL() },
                AbstractCompileKotlinAgainstKotlinTest.class.getClassLoader()
        );
        Class<?> clazz = classLoader.loadClass(PackageClassUtils.getPackageClassName(FqName.ROOT));
        Method box = clazz.getMethod("box", new Class[] {});
        String result = (String) box.invoke(null);
        assertEquals("OK", result);
    }

    private OutputFileCollection compileA(@NotNull File ktAFile) throws IOException {
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(),
                                                                                                            ConfigurationKind.ALL);
        return compileKotlin(ktAFile, aDir, jetCoreEnvironment, getTestRootDisposable());
    }

    private OutputFileCollection compileB(@NotNull File ktBFile) throws IOException {
        CompilerConfiguration configurationWithADirInClasspath = JetTestUtils
                .compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), aDir);

        return compileKotlin(ktBFile, bDir, JetCoreEnvironment.createForTests(getTestRootDisposable(), configurationWithADirInClasspath),
                      getTestRootDisposable());
    }

    private static OutputFileCollection compileKotlin(
            @NotNull File file, @NotNull File outputDir, @NotNull JetCoreEnvironment jetCoreEnvironment,
            @NotNull Disposable disposable
    ) throws IOException {

        String text = FileUtil.loadFile(file, true);

        JetFile psiFile = JetTestUtils.createFile(file.getName(), text, jetCoreEnvironment.getProject());

        OutputFileCollection outputFiles = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        OutputUtilsPackage.writeAllTo(outputFiles, outputDir);

        Disposer.dispose(disposable);
        return outputFiles;
    }
}
