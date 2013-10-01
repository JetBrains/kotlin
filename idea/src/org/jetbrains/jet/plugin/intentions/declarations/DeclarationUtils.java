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

package org.jetbrains.jet.plugin.intentions.declarations;

import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.util.JetPsiMatcher;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collections;

public class DeclarationUtils {
    private DeclarationUtils() {
    }

    private static void assertNotNull(Object value) {
        assert value != null : "Expression must be checked before applying transformation";
    }

    public static boolean checkSplitProperty(@NotNull JetProperty property) {
        return property.getInitializer() != null && property.isLocal();
    }

    public static final Predicate<PsiElement> SKIP_DELIMITERS = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return input == null
                   || input instanceof PsiWhiteSpace || input instanceof PsiComment
                   || input.getNode().getElementType() == JetTokens.SEMICOLON;
        }
    };

    @Nullable
    public static Pair<JetProperty, JetBinaryExpression> checkAndGetPropertyAndInitializer(@NotNull PsiElement element) {
        JetProperty property = null;
        JetExpression initializer = null;

        if (element instanceof JetProperty) {
            PsiElement nextElement = JetPsiUtil.skipSiblingsForwardByPredicate(element, SKIP_DELIMITERS);
            if (nextElement instanceof JetExpression) {
                property = (JetProperty) element;
                initializer = (JetExpression) nextElement;
            }
        }

        if (property == null) return null;
        if (property.getInitializer() != null) return null;
        if (!JetPsiUtil.isOrdinaryAssignment(initializer)) return null;

        JetBinaryExpression assignment = (JetBinaryExpression) initializer;

        if (!(assignment.getLeft() instanceof JetSimpleNameExpression)) return null;
        if (assignment.getRight() == null) return null;
        //noinspection ConstantConditions
        if (!JetPsiMatcher.checkIdentifierMatch(property.getNameIdentifier().getText(), assignment.getLeft().getText())) return null;

        return new Pair<JetProperty, JetBinaryExpression>(property, assignment);
    }

    @Nullable
    private static JetType getPropertyTypeIfNeeded(@NotNull JetProperty property, @NotNull JetFile file) {
        if (property.getTypeRef() != null) return null;

        JetType type = AnalyzerFacadeWithCache.analyzeFileWithCache(file).getBindingContext().get(
                BindingContext.EXPRESSION_TYPE,
                property.getInitializer()
        );

        return type == null || type.isError() ? null : type;
    }

    // returns assignment which replaces initializer
    @NotNull
    public static JetBinaryExpression splitPropertyDeclaration(@NotNull JetProperty property, @NotNull JetFile file) {
        Project project = property.getProject();

        PsiElement parent = property.getParent();
        assertNotNull(parent);

        //noinspection unchecked
        JetExpression initializer = property.getInitializer();
        assertNotNull(initializer);

        //noinspection ConstantConditions, unchecked
        JetBinaryExpression newInitializer = JetPsiFactory.createBinaryExpression(
                project, JetPsiFactory.createSimpleName(project, property.getName()), "=", initializer
        );

        newInitializer = (JetBinaryExpression) parent.addAfter(newInitializer, property);
        parent.addAfter(JetPsiFactory.createNewLine(project), property);

        //noinspection ConstantConditions
        JetType inferredType = getPropertyTypeIfNeeded(property, file);

        String typeStr = inferredType != null
                         ? DescriptorRenderer.TEXT.renderType(inferredType)
                         : JetPsiUtil.getNullableText(property.getTypeRef());

        //noinspection ConstantConditions
        property = (JetProperty) property.replace(
                JetPsiFactory.createProperty(project, property.getNameIdentifier().getText(), typeStr, property.isVar())
        );

        if (inferredType != null) {
            ReferenceToClassesShortening.compactReferenceToClasses(Collections.singletonList(property.getTypeRef()));
        }

        return newInitializer;
    }

    // Returns joined property
    @NotNull
    public static JetProperty joinPropertyDeclarationWithInitializer(
            @NotNull Pair<JetProperty, JetBinaryExpression> propertyAndInitializer
    ) {
        JetProperty property = propertyAndInitializer.first;
        assertNotNull(property);

        JetBinaryExpression assignment = propertyAndInitializer.second;
        assertNotNull(assignment);

        //noinspection ConstantConditions
        JetProperty newProperty = JetPsiFactory.createProperty(
                property.getProject(),
                property.getNameIdentifier().getText(),
                JetPsiUtil.getNullableText(property.getTypeRef()),
                property.isVar(),
                JetPsiUtil.getNullableText(assignment.getRight())
        );

        property.getParent().deleteChildRange(property.getNextSibling(), assignment);
        return (JetProperty) property.replace(newProperty);
    }

    // Returns joined property
    @NotNull
    public static JetProperty joinPropertyDeclarationWithInitializer(@NotNull PsiElement element) {
        Pair<JetProperty, JetBinaryExpression> propertyAndInitializer = checkAndGetPropertyAndInitializer(element);
        assertNotNull(propertyAndInitializer);

        //noinspection ConstantConditions
        return joinPropertyDeclarationWithInitializer(propertyAndInitializer);
    }
}
