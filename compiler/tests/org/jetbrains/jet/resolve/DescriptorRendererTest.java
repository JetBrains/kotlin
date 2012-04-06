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

package org.jetbrains.jet.resolve;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetVisitorVoid;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzeExhaust;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Evgeny Gerashchenko
 * @since 4/6/12
 */
public class DescriptorRendererTest extends JetLiteFixture {
    public void testGlobalProperties() throws IOException {
        doTest();
    }

    public void testGlobalFunctions() throws IOException {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getTestDataPathBase() + "/renderer";
    }

    private void doTest() throws IOException {
        String fileName = getTestName(false) + ".kt";
        JetFile psiFile = createPsiFile(fileName, loadFile(fileName));
        AnalyzeExhaust analyzeExhaust =
                AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration((JetFile)psiFile, JetControlFlowDataTraceFactory.EMPTY);
        final BindingContext bindingContext = analyzeExhaust.getBindingContext();
        final List<DeclarationDescriptor> descriptors = new ArrayList<DeclarationDescriptor>();
        psiFile.acceptChildren(new JetVisitorVoid() {
            @Override
            public void visitJetElement(JetElement element) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
                if (descriptor != null) {
                    descriptors.add(descriptor);
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
