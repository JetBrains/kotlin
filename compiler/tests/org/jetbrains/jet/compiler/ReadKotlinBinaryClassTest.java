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

package org.jetbrains.jet.compiler;

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
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.plugin.JetLanguage;
import org.junit.Assert;

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
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);

        String text = FileUtil.loadFile(testFile);

        LightVirtualFile virtualFile = new LightVirtualFile(testFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        GenerationState state = new GenerationState(jetCoreEnvironment.getProject(), ClassBuilderFactories.binaries(false));
        AnalyzingUtils.checkForSyntacticErrors(psiFile);
        BindingContext bindingContext = state.compile(psiFile);

        ClassFileFactory classFileFactory = state.getFactory();

        CompileEnvironment.writeToOutputDirectory(classFileFactory, tmpdir.getPath());
        
        NamespaceDescriptor namespaceFromSource = (NamespaceDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiFile);

        Assert.assertEquals("test", namespaceFromSource.getName());

        Disposer.dispose(myTestRootDisposable);


        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);

        jetCoreEnvironment.addToClasspath(tmpdir);
        jetCoreEnvironment.addToClasspath(new File("out/production/runtime"));

        JavaDescriptorResolver javaDescriptorResolver = new InjectorForJavaSemanticServices(jetCoreEnvironment.getProject()).getJavaDescriptorResolver();
        NamespaceDescriptor namespaceFromClass = javaDescriptorResolver.resolveNamespace(FqName.topLevel("test"), DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
        
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
