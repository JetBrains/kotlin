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

package org.jetbrains.kotlin.idea.intentions.declarations;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.JetType;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.kotlin.psi.PsiPackage.createExpressionByPattern;

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

        JetExpression initializer = property.getInitializer();
        JetType type = initializer != null ? ResolvePackage.analyze(property, BodyResolveMode.FULL).getType(initializer) : null;
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
        JetExpression newInitializer = createExpressionByPattern(psiFactory, "$0 = $1", property.getName(), initializer);

        newInitializer = (JetBinaryExpression) parent.addAfter(newInitializer, property);
        parent.addAfter(psiFactory.createNewLine(), property);

        Project project = newInitializer.getProject();
        PsiFile file = parent.getContainingFile();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(PsiDocumentManager.getInstance(project).getDocument(file));
        CodeStyleManager.getInstance(project).adjustLineIndent(file, newInitializer.getTextRange());

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
            ShortenReferences.DEFAULT.process(property.getTypeReference());
        }

        return (JetBinaryExpression) newInitializer;
    }
}
