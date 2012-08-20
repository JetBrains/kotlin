/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import junit.framework.Assert;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.*;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.test.TestCaseWithTmpdir;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Stepan Koltsov
 */
public class CompileKotlinAgainstKotlinTest extends TestCaseWithTmpdir {

    private final File ktAFile;
    private final File ktBFile;

    public CompileKotlinAgainstKotlinTest(File ktAFile) {
        Assert.assertTrue(ktAFile.getName().endsWith("A.kt"));
        this.ktAFile = ktAFile;
        this.ktBFile = new File(ktAFile.getPath().replaceFirst("A\\.kt$", "B.kt"));
    }

    @Override
    public String getName() {
        return ktAFile.getName();
    }
    
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

    @Override
    protected void runTest() throws Throwable {
        compileA();
        compileB();

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ aDir.toURI().toURL(), bDir.toURI().toURL() },
                CompileKotlinAgainstKotlinTest.class.getClassLoader()
        );
        Class<?> clazz = classLoader.loadClass("bbb.namespace");
        Method main = clazz.getMethod("main", new Class[] { String[].class });
        main.invoke(null, new Object[] { new String[0] });
    }

    private void compileA() throws IOException {
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable);

        String text = FileUtil.loadFile(ktAFile);

        JetFile psiFile = JetTestUtils.createFile(ktAFile.getName(), text, jetCoreEnvironment.getProject());

        ClassFileFactory classFileFactory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, aDir);
        
        Disposer.dispose(myTestRootDisposable);
    }

    private void compileB() throws IOException {
        JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(myTestRootDisposable, CompileCompilerDependenciesTest
                .compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), aDir));

        String text = FileUtil.loadFile(ktBFile);

        JetFile psiFile = JetTestUtils.createFile(ktBFile.getName(), text, jetCoreEnvironment.getProject());

        ClassFileFactory classFileFactory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, bDir);

        Disposer.dispose(myTestRootDisposable);
    }

    public static Test suite() {
        class Filter implements FilenameFilter {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("A.kt");
            }
        }
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/compileKotlinAgainstKotlin", true, new Filter(), new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new CompileKotlinAgainstKotlinTest(file);
            }
        });
    }
}
