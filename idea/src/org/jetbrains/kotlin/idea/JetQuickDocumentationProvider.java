/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea;

import com.google.common.base.Predicate;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.KotlinLightMethod;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.kdoc.KdocPackage;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetPackageDirective;
import org.jetbrains.kotlin.psi.JetReferenceExpression;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;

public class JetQuickDocumentationProvider extends AbstractDocumentationProvider {
    private static final Predicate<PsiElement> SKIP_WHITESPACE_AND_EMPTY_PACKAGE = new Predicate<PsiElement>() {
        @Override
        public boolean apply(PsiElement input) {
            // Skip empty package because there can be comments before it
            // Skip whitespaces
            return (input instanceof JetPackageDirective && input.getChildren().length == 0) || input instanceof PsiWhiteSpace;
        }
    };

    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return getText(element, originalElement, true);
    }

    @Override
    public String generateDoc(PsiElement element, PsiElement originalElement) {
        return getText(element, originalElement, false);
    }

    private static String getText(PsiElement element, PsiElement originalElement, boolean quickNavigation) {
        if (element instanceof JetDeclaration) {
            return renderKotlinDeclaration((JetDeclaration) element, quickNavigation);
        }
        else if (element instanceof KotlinLightMethod) {
            return renderKotlinDeclaration(((KotlinLightMethod) element).getOrigin(), quickNavigation);
        }

        if (quickNavigation) {
            JetReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(originalElement, JetReferenceExpression.class, false);
            if (referenceExpression != null) {
                BindingContext context = ResolvePackage.analyze(referenceExpression);
                DeclarationDescriptor declarationDescriptor = context.get(BindingContext.REFERENCE_TARGET, referenceExpression);
                if (declarationDescriptor != null) {
                    return mixKotlinToJava(declarationDescriptor, element, originalElement);
                }
            }
        }
        else {
            // This element was resolved to non-kotlin element, it will be rendered with own provider
        }

        return null;
    }

    private static String renderKotlinDeclaration(JetDeclaration declaration, boolean quickNavigation) {
        BindingContext context = ResolvePackage.analyze(declaration);
        DeclarationDescriptor declarationDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

        assert declarationDescriptor != null;

        String renderedDecl = DescriptorRenderer.HTML_NAMES_WITH_SHORT_TYPES.render(declarationDescriptor);
        if (!quickNavigation) {
            renderedDecl = "<pre>" + DescriptorRenderer.HTML_NAMES_WITH_SHORT_TYPES.render(declarationDescriptor) + "</pre>";
        }

        KDocTag comment = KdocPackage.findKDoc(declarationDescriptor);
        if (comment != null) {
            renderedDecl = renderedDecl + "<br/>" + org.jetbrains.kotlin.idea.kdoc.KdocPackage.renderKDoc(comment);
        }

        return renderedDecl;
    }

    private static String mixKotlinToJava(
            @NotNull DeclarationDescriptor declarationDescriptor,
            PsiElement element, PsiElement originalElement
    ) {
        String originalInfo = new JavaDocumentationProvider().getQuickNavigateInfo(element, originalElement);
        if (originalInfo != null) {
            String renderedDecl = DescriptorRenderer.HTML_NAMES_WITH_SHORT_TYPES.render(declarationDescriptor);
            return renderedDecl + "<br/>Java declaration:<br/>" + originalInfo;
        }

        return null;
    }
}
