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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.test.generator.SimpleTestClassModel;
import org.jetbrains.jet.test.generator.TestGenerator;
import org.jetbrains.jet.test.util.NamespaceComparator;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Stepan Koltsov
 */
public abstract class AbstractReadJavaBinaryClassTest extends TestCaseWithTmpdir {

    public void doTest(@NotNull String javaFileName) throws Exception {
        Assert.assertTrue("A java file expected: " + javaFileName, javaFileName.endsWith(".java"));
        File javaFile = new File(javaFileName);
        File ktFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".kt"));
        File txtFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".txt"));
        NamespaceDescriptor nsa = compileKotlin(ktFile);
        NamespaceDescriptor nsb = LoadJavaDescriptorUtil.compileJava(Collections.singletonList(javaFile), tmpdir, myTestRootDisposable);
        NamespaceComparator.compareNamespaces(nsa, nsb, NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT, txtFile);
    }

    @NotNull
    private NamespaceDescriptor compileKotlin(File ktFile) throws Exception {
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, ConfigurationKind.JDK_ONLY);

        String text = FileUtil.loadFile(ktFile);

        LightVirtualFile virtualFile = new LightVirtualFile(ktFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(
                virtualFile, JetLanguage.INSTANCE, true, false);

        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
                psiFile, Collections.<AnalyzerScriptParameter>emptyList(), BuiltinsScopeExtensionMode.ALL).getBindingContext();
        return bindingContext.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, FqName.topLevel(Name.identifier("test")));
    }

    public static void main(String[] args) throws IOException {
        String aPackage = "org.jetbrains.jet.jvm.compiler";
        String extension = "java";
        new TestGenerator(
                "compiler/tests/",
                aPackage,
                "ReadJavaBinaryClassTestGenerated",
                AbstractReadJavaBinaryClassTest.class,
                Arrays.asList(
                        new SimpleTestClassModel(new File("compiler/testData/readJavaBinaryClass"), true, extension, "doTest")
                ),
                AbstractReadJavaBinaryClassTest.class
        ).generateAndSave();
    }
}
