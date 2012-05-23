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

package org.jetbrains.jet.cli.jvm.compiler;

import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import junit.framework.Assert;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.File;

/**
 * Compile Kotlin and then parse model from .class files.
 *
 * @author Stepan Koltsov
 */
public class ReadKotlinBinaryClassTest extends TestCaseWithTmpdir {

    private JetCoreEnvironment jetCoreEnvironment;

    private final File testFile;
    private final File txtFile;

    public ReadKotlinBinaryClassTest(@NotNull File testFile) {
        this.testFile = testFile;
        this.txtFile = new File(testFile.getPath().replaceFirst("\\.kt$", ".txt"));
        setName(testFile.getName());
    }

    @Override
    public void runTest() throws Exception {
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, CompilerSpecialMode.JDK_HEADERS);

        String text = FileUtil.loadFile(testFile);

        LightVirtualFile virtualFile = new LightVirtualFile(testFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        GenerationState state = GenerationUtils.compileFileGetGenerationStateForTest(psiFile, CompilerSpecialMode.JDK_HEADERS);

        ClassFileFactory classFileFactory = state.getFactory();

        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, tmpdir.getPath());
        
        NamespaceDescriptor namespaceFromSource = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, psiFile);

        Assert.assertEquals("test", namespaceFromSource.getName().getName());

        Disposer.dispose(myTestRootDisposable);


        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, CompilerSpecialMode.JDK_HEADERS);

        jetCoreEnvironment.addToClasspath(tmpdir);
        jetCoreEnvironment.addToClasspath(new File("out/production/runtime"));

        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(
                jetCoreEnvironment.getCompilerDependencies(), jetCoreEnvironment.getProject());
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        NamespaceDescriptor namespaceFromClass = javaDescriptorResolver.resolveNamespace(FqName.topLevel(Name.identifier("test")), DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
        
        NamespaceComparator.compareNamespaces(namespaceFromSource, namespaceFromClass, false, txtFile);
    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/readKotlinBinaryClass", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new ReadKotlinBinaryClassTest(file);
            }
        });
    }
    
}
