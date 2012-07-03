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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.junit.Assert;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @author Stepan Koltsov
 */
public class ReadJavaBinaryClassTest extends TestCaseWithTmpdir {
    
    private final File ktFile;
    private final File javaFile;
    private final File txtFile;

    public ReadJavaBinaryClassTest(@NotNull File javaFile) {
        this.javaFile = javaFile;
        Assert.assertTrue(javaFile.getName().endsWith(".java"));
        this.ktFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".kt"));
        this.txtFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".txt"));
        setName(javaFile.getName());
    }


    @Override
    public void runTest() throws Exception {
        NamespaceDescriptor nsa = compileKotlin();
        NamespaceDescriptor nsb = compileJava();
        NamespaceComparator.compareNamespaces(nsa, nsb, false, txtFile);
    }

    @NotNull
    private NamespaceDescriptor compileKotlin() throws Exception {
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, CompilerSpecialMode.JDK_HEADERS);

        String text = FileUtil.loadFile(ktFile);

        LightVirtualFile virtualFile = new LightVirtualFile(ktFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
                psiFile, Collections.<AnalyzerScriptParameter>emptyList(),
                jetCoreEnvironment.getCompilerDependencies().getCompilerSpecialMode())
                    .getBindingContext();
        return bindingContext.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, FqName.topLevel(Name.identifier("test")));
    }

    private NamespaceDescriptor compileJava() throws Exception {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(Collections.singleton(javaFile));
            List<String> options = Arrays.asList(
                    "-classpath", "out/production/runtime" + File.pathSeparator + JetTestUtils.getAnnotationsJar().getPath(),
                    "-d", tmpdir.getPath()
            );
            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, null, options, null, javaFileObjectsFromFiles);

            Assert.assertTrue(task.call());
        } finally {
            fileManager.close();
        }

        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, CompilerSpecialMode.JDK_HEADERS);

        jetCoreEnvironment.addToClasspath(tmpdir);
        jetCoreEnvironment.addToClasspath(new File("out/production/runtime"));

        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(
                jetCoreEnvironment.getCompilerDependencies(), jetCoreEnvironment.getProject());
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        return javaDescriptorResolver.resolveNamespace(FqName.topLevel(Name.identifier("test")), DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    public static Test suite() {
        class JavaFilter implements FilenameFilter {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".java");
            }
        }

        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/readJavaBinaryClass", true, new JavaFilter(), new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new ReadJavaBinaryClassTest(file);
            }
        });
    }

}
