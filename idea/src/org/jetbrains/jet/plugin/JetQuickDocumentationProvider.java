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

package org.jetbrains.jet.plugin;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.resolve.DescriptorRenderer;

/**
 * @author abreslav
 */
public class JetQuickDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        JetReferenceExpression ref;
        if (originalElement instanceof JetReferenceExpression) {
            ref = (JetReferenceExpression) originalElement;
        }
        else {
            ref = PsiTreeUtil.getParentOfType(originalElement, JetReferenceExpression.class);
        }
        if (ref != null) {
            BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) ref.getContainingFile())
                    .getBindingContext();
            DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, ref);
            if (declarationDescriptor != null) {
                return render(declarationDescriptor);
            }
            PsiElement psiElement = BindingContextUtils.resolveToDeclarationPsiElement(bindingContext, ref);
            if (psiElement != null) {
                declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiElement);
                if (declarationDescriptor != null) {
                    return render(declarationDescriptor);
                }
            }
            return "Unresolved";
        }

//        if (originalElement.getNode().getElementType() == JetTokens.IDENTIFIER) {
//            JetDeclaration declaration = PsiTreeUtil.getParentOfType(originalElement, JetDeclaration.class);
//            BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache((JetFile) element.getContainingFile(), ErrorHandler.DO_NOTHING);
//            DeclarationDescriptor declarationDescriptor = bindingContext.getDeclarationDescriptor(declaration);
//            if (declarationDescriptor != null) {
//                return render(declarationDescriptor);
//            }
//        }
        return "Not a reference";
    }

    @Override
    public String generateDoc(PsiElement element, PsiElement originalElement) {
        return getQuickNavigateInfo(element, originalElement);
    }

    private String render(DeclarationDescriptor declarationDescriptor) {
        return DescriptorRenderer.HTML.render(declarationDescriptor);
    }
}
