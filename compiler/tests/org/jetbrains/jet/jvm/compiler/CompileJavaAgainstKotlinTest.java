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
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.junit.Assert;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @author Stepan Koltsov
 *
 * @see WriteSignatureTest
 */
public class CompileJavaAgainstKotlinTest extends TestCaseWithTmpdir {

    private final File ktFile;
    private final File javaFile;
    private JetCoreEnvironment jetCoreEnvironment;

    public CompileJavaAgainstKotlinTest(File ktFile) {
        this.ktFile = ktFile;
        Assert.assertTrue(ktFile.getName().endsWith(".kt"));
        this.javaFile = new File(ktFile.getPath().replaceFirst("\\.kt", ".java"));
    }

    @Override
    public String getName() {
        return ktFile.getName();
    }

    @Override
    protected void runTest() throws Throwable {
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable);


        String text = FileUtil.loadFile(ktFile);

        LightVirtualFile virtualFile = new LightVirtualFile(ktFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        ClassFileFactory classFileFactory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, tmpdir);

        Disposer.dispose(myTestRootDisposable);

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(Collections.singleton(javaFile));
            List<String> options = Arrays.asList(
                    "-classpath", tmpdir.getPath() + System.getProperty("path.separator") + "out/production/stdlib",
                    "-d", tmpdir.getPath()
                );
            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, null, options, null, javaFileObjectsFromFiles);

            Assert.assertTrue(task.call());
        } finally {
            fileManager.close();
        }
    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/compileJavaAgainstKotlin", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new CompileJavaAgainstKotlinTest(file);
            }
        });

    }

}
