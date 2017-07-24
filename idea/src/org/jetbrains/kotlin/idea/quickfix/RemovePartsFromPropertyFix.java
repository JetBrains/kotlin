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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.KotlinTypeKt;

public class RemovePartsFromPropertyFix extends KotlinQuickFixAction<KtProperty> {
    private final boolean removeInitializer;
    private final boolean removeGetter;
    private final boolean removeSetter;

    private RemovePartsFromPropertyFix(@NotNull KtProperty element, boolean removeInitializer, boolean removeGetter, boolean removeSetter) {
        super(element);
        this.removeInitializer = removeInitializer;
        this.removeGetter = removeGetter;
        this.removeSetter = removeSetter;
    }

    private RemovePartsFromPropertyFix(@NotNull KtProperty element) {
        this(element, element.hasInitializer(),
             element.getGetter() != null && element.getGetter().getBodyExpression() != null,
             element.getSetter() != null && element.getSetter().getBodyExpression() != null);
    }

    private static String partsToRemove(boolean getter, boolean setter, boolean initializer) {
        StringBuilder sb = new StringBuilder();
        if (getter) {
            sb.append("getter");
            if (setter && initializer) {
                sb.append(", ");
            }
            else if (setter || initializer) {
                sb.append(" and ");
            }
        }
        if (setter) {
            sb.append("setter");
            if (initializer) {
                sb.append(" and ");
            }
        }
        if (initializer) {
            sb.append("initializer");
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("remove.parts.from.property", partsToRemove(removeGetter, removeSetter, removeInitializer));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("remove.parts.from.property.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiFile file) {
        if (!super.isAvailable(project, editor, file)) return false;

        KotlinType type = QuickFixUtil.getDeclarationReturnType(getElement());
        return type != null && !KotlinTypeKt.isError(type);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull KtFile file) throws IncorrectOperationException {
        KotlinType type = QuickFixUtil.getDeclarationReturnType(getElement());
        KtProperty newElement = (KtProperty) getElement().copy();
        KtPropertyAccessor getter = newElement.getGetter();
        if (removeGetter && getter != null) {
            newElement.deleteChildInternal(getter.getNode());
        }
        KtPropertyAccessor setter = newElement.getSetter();
        if (removeSetter && setter != null) {
            newElement.deleteChildInternal(setter.getNode());
        }
        KtExpression initializer = newElement.getInitializer();
        KotlinType typeToAdd = null;
        if (removeInitializer && initializer != null) {
            PsiElement nameIdentifier = newElement.getNameIdentifier();
            assert nameIdentifier != null;
            PsiElement nextSibling = nameIdentifier.getNextSibling();
            assert nextSibling != null;
            newElement.deleteChildRange(nextSibling, initializer);

            if (newElement.getTypeReference() == null && type != null) {
                typeToAdd = type;
            }
        }
        newElement = (KtProperty) getElement().replace(newElement);
        if (typeToAdd != null) {
            SpecifyTypeExplicitlyIntention.Companion.addTypeAnnotation(editor, newElement, typeToAdd);
        }
    }

    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtProperty> createAction(@NotNull Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                assert element instanceof KtElement;
                KtProperty property = PsiTreeUtil.getParentOfType(element, KtProperty.class);
                if (property == null) return null;
                return new RemovePartsFromPropertyFix(property);
            }
        };
    }

    public static KotlinSingleIntentionActionFactory createLateInitFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtProperty> createAction(@NotNull Diagnostic diagnostic) {
                PsiElement element = Errors.INAPPLICABLE_LATEINIT_MODIFIER.cast(diagnostic).getPsiElement();
                KtProperty property = PsiTreeUtil.getParentOfType(element, KtProperty.class);
                if (property == null) return null;

                boolean hasInitializer = property.hasInitializer();
                boolean hasGetter = property.getGetter() != null && property.getGetter().getBodyExpression() != null;
                boolean hasSetter = property.getSetter() != null && property.getSetter().getBodyExpression() != null;
                if (!hasInitializer && !hasGetter && !hasSetter) return null;

                return new RemovePartsFromPropertyFix(property, hasInitializer, hasGetter, hasSetter);
            }
        };
    }

}
