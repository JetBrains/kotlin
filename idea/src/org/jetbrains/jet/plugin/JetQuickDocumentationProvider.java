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

package org.jetbrains.jet.plugin;

import com.google.common.base.Predicate;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.KotlinLightMethod;
import org.jetbrains.jet.kdoc.lexer.KDocTokens;
import org.jetbrains.jet.kdoc.psi.api.KDoc;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

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
                BindingContext context = AnalyzerFacadeWithCache.getContextForElement(referenceExpression);
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
        BindingContext context = AnalyzerFacadeWithCache.getContextForElement(declaration);
        DeclarationDescriptor declarationDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

        assert declarationDescriptor != null;

        String renderedDecl = DescriptorRenderer.HTML_NAMES_WITH_SHORT_TYPES.render(declarationDescriptor);
        if (!quickNavigation) {
            renderedDecl = "<pre>" + DescriptorRenderer.HTML_NAMES_WITH_SHORT_TYPES.render(declarationDescriptor) + "</pre>";
        }

        KDoc comment = findElementKDoc(declaration);
        if (comment != null) {
            renderedDecl = renderedDecl + "<br/>" + kDocToHtml(comment);
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

    private static String getKDocContent(@NotNull KDoc kDoc) {
        StringBuilder builder = new StringBuilder();

        boolean contentStarted = false;
        boolean afterAsterisk = false;

        for (PsiElement element : kDoc.getChildren()) {
            IElementType type = element.getNode().getElementType();

            if (KDocTokens.CONTENT_TOKENS.contains(type)) {
                contentStarted = true;
                builder.append(afterAsterisk ? StringUtil.trimLeading(element.getText()) : element.getText());
                afterAsterisk = false;
            }

            if (type == KDocTokens.LEADING_ASTERISK || type == KDocTokens.START) {
                afterAsterisk = true;
            }

            if (contentStarted && element instanceof PsiWhiteSpace) {
                builder.append(StringUtil.repeat("\n", StringUtil.countNewLines(element.getText())));
            }
        }

        return builder.toString();
    }

    @Nullable
    private static KDoc findElementKDoc(@NotNull JetElement element) {
        return (KDoc) JetPsiUtil.findChildByType(element, JetTokens.DOC_COMMENT);
    }

    private static String kDocToHtml(@NotNull KDoc comment) {
        // TODO: Parse and show markdown comments as html
        String content = getKDocContent(comment);
        String htmlContent = StringUtil.replace(content, "\n", "<br/>")
                .replaceAll("(@param)\\s+(\\w+)", "@param - <i>$2</i>")
                .replaceAll("(@\\w+)", "<b>$1</b>");

        return "<p>" + htmlContent + "</p>";
    }
}
