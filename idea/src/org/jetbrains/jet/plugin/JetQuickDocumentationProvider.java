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
import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.libraries.JetDecompiledData;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.resolve.DescriptorRenderer;

/**
 * @author abreslav
 * @author Evgeny Gerashchenko
 */
public class JetQuickDocumentationProvider extends AbstractDocumentationProvider {
    private static String getText(PsiElement element, PsiElement originalElement, boolean mergeKotlinAndJava) {
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
                return render(declarationDescriptor, bindingContext, element, originalElement, mergeKotlinAndJava);
            }
            PsiElement psiElement = BindingContextUtils.resolveToDeclarationPsiElement(bindingContext, ref);
            if (psiElement != null) {
                declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiElement);
                if (declarationDescriptor != null) {
                    return render(declarationDescriptor, bindingContext, element, originalElement, mergeKotlinAndJava);
                }
            }
            return "Unresolved";
        }
        return null;
    }

    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return getText(element, originalElement, true);
    }

    @Override
    public String generateDoc(PsiElement element, PsiElement originalElement) {
        return getText(element, originalElement, false);
    }

    private static String render(@NotNull DeclarationDescriptor declarationDescriptor, @NotNull BindingContext bindingContext,
            PsiElement element, PsiElement originalElement, boolean mergeKotlinAndJava) {
        String renderedDecl = DescriptorRenderer.HTML.render(declarationDescriptor);
        if (isKotlinDeclaration(declarationDescriptor, bindingContext)) {
            return renderedDecl;
        } else {
            if (mergeKotlinAndJava) {
                return renderedDecl + "\nOriginal: " + XmlStringUtil.escapeString(
                        new JavaDocumentationProvider().getQuickNavigateInfo(element, originalElement));
            } else {
                return null;
            }
        }
    }

    private static boolean isKotlinDeclaration(DeclarationDescriptor descriptor, BindingContext bindingContext) {
        PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
        if (declaration == null) return false;
        if (JetLanguage.INSTANCE == declaration.getLanguage()) return true;
        ClsClassImpl clsClass = PsiTreeUtil.getParentOfType(declaration, ClsClassImpl.class);
        if (clsClass == null) return false;
        PsiClass delegate = clsClass.getUserData(ClsClassImpl.DELEGATE_KEY);
        if (delegate != null) {
            if (delegate instanceof ClsClassImpl) {
                clsClass = (ClsClassImpl) delegate;
            }
            else {
                return false;
            }
        }
        return JetDecompiledData.isKotlinFile((ClsFileImpl) clsClass.getContainingFile());
    }
}
