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

package org.jetbrains.jet.renderer;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetVisitorVoid;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DescriptorRendererTest extends JetLiteFixture {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    public void testGlobalProperties() throws IOException {
        doTest();
    }

    public void testGlobalFunctions() throws IOException {
        doTest();
    }

    public void testClasses() throws IOException {
        doTest();
    }

    public void testEnum() throws IOException {
        doTest();
    }

    public void testUnitType() throws IOException {
        doTest();
    }

    public void testFunctionTypes() throws IOException {
        doTest();
    }

    public void testErrorType() throws IOException {
        doTest();
    }

    public void testInheritedMembersVisibility() throws IOException {
        doTest();
    }

    public void testKeywordsInNames() throws IOException {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getTestDataPathBase() + "/renderer";
    }

    private void doTest() throws IOException {
        String fileName = getTestName(false) + ".kt";
        JetFile psiFile = createPsiFile(null, fileName, loadFile(fileName));
        AnalyzeExhaust analyzeExhaust =
                AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(psiFile, Collections.<AnalyzerScriptParameter>emptyList());
        final BindingContext bindingContext = analyzeExhaust.getBindingContext();
        final List<DeclarationDescriptor> descriptors = new ArrayList<DeclarationDescriptor>();

        FqName fqName = psiFile.getNamespaceHeader().getFqName();
        if (!fqName.isRoot()) {
            PackageViewDescriptor packageDescriptor = analyzeExhaust.getModuleDescriptor().getPackage(fqName);
            descriptors.add(packageDescriptor);
        }

        psiFile.acceptChildren(new JetVisitorVoid() {
            @Override
            public void visitJetElement(@NotNull JetElement element) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
                if (descriptor != null) {
                    descriptors.add(descriptor);
                    if (descriptor instanceof ClassDescriptor) {
                        descriptors.addAll(((ClassDescriptor) descriptor).getConstructors());
                    }
                }
                element.acceptChildren(this);
            }
        });

        StringBuilder renderedDescriptors = new StringBuilder();
        for (DeclarationDescriptor descriptor : descriptors) {
            if (renderedDescriptors.length() != 0) {
                renderedDescriptors.append("\n");
            }
            renderedDescriptors.append(DescriptorRenderer.TEXT.render(descriptor));
        }

        Document document = new DocumentImpl(psiFile.getText());
        assertSameLines(JetTestUtils.getLastCommentedLines(document), renderedDescriptors.toString());
    }
}
