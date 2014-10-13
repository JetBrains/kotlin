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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.util.IdeDescriptorRenderers;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class DeclarationUtils {
    private DeclarationUtils() {
    }

    private static void assertNotNull(Object value) {
        assert value != null : "Expression must be checked before applying transformation";
    }

    public static boolean checkSplitProperty(@NotNull JetProperty property) {
        return property.hasInitializer() && property.isLocal();
    }

    @Nullable
    private static JetType getPropertyTypeIfNeeded(@NotNull JetProperty property) {
        if (property.getTypeReference() != null) return null;

        JetType type = AnalyzerFacadeWithCache.getContextForElement(property).get(
                BindingContext.EXPRESSION_TYPE, property.getInitializer()
        );
        return type == null || type.isError() ? null : type;
    }

    // returns assignment which replaces initializer
    @NotNull
    public static JetBinaryExpression splitPropertyDeclaration(@NotNull JetProperty property) {
        PsiElement parent = property.getParent();
        assertNotNull(parent);

        //noinspection unchecked
        JetExpression initializer = property.getInitializer();
        assertNotNull(initializer);

        JetPsiFactory psiFactory = JetPsiFactory(property);
        //noinspection ConstantConditions, unchecked
        JetBinaryExpression newInitializer = psiFactory.createBinaryExpression(
                psiFactory.createSimpleName(property.getName()), "=", initializer
        );

        newInitializer = (JetBinaryExpression) parent.addAfter(newInitializer, property);
        parent.addAfter(psiFactory.createNewLine(), property);

        //noinspection ConstantConditions
        JetType inferredType = getPropertyTypeIfNeeded(property);

        String typeStr = inferredType != null
                         ? IdeDescriptorRenderers.SOURCE_CODE.renderType(inferredType)
                         : JetPsiUtil.getNullableText(property.getTypeReference());

        //noinspection ConstantConditions
        property = (JetProperty) property.replace(
                psiFactory.createProperty(property.getNameIdentifier().getText(), typeStr, property.isVar())
        );

        if (inferredType != null) {
            ShortenReferences.INSTANCE$.process(property.getTypeReference());
        }

        return newInitializer;
    }
}
