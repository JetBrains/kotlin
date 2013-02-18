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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction;

public class RemovePartsFromPropertyFix extends JetIntentionAction<JetProperty> {
    private final boolean removeInitializer;
    private final boolean removeGetter;
    private final boolean removeSetter;

    private RemovePartsFromPropertyFix(@NotNull JetProperty element, boolean removeInitializer, boolean removeGetter, boolean removeSetter) {
        super(element);
        this.removeInitializer = removeInitializer;
        this.removeGetter = removeGetter;
        this.removeSetter = removeSetter;
    }

    private RemovePartsFromPropertyFix(@NotNull JetProperty element) {
        this(element, element.getInitializer() != null,
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
        return JetBundle.message("remove.parts.from.property", partsToRemove(removeGetter, removeSetter, removeInitializer));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.parts.from.property.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        JetType type = QuickFixUtil.getDeclarationReturnType(element);
        return super.isAvailable(project, editor, file) && type != null && !ErrorUtils.isErrorType(type);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!(file instanceof JetFile)) return;
        JetType type = QuickFixUtil.getDeclarationReturnType(element);
        JetProperty newElement = (JetProperty) element.copy();
        JetPropertyAccessor getter = newElement.getGetter();
        if (removeGetter && getter != null) {
            newElement.deleteChildInternal(getter.getNode());
        }
        JetPropertyAccessor setter = newElement.getSetter();
        if (removeSetter && setter != null) {
            newElement.deleteChildInternal(setter.getNode());
        }
        JetExpression initializer = newElement.getInitializer();
        JetType typeToAdd = null;
        if (removeInitializer && initializer != null) {
            PsiElement nameIdentifier = newElement.getNameIdentifier();
            assert nameIdentifier != null;
            PsiElement nextSibling = nameIdentifier.getNextSibling();
            assert nextSibling != null;
            newElement.deleteChildRange(nextSibling, initializer);

            if (newElement.getTypeRef() == null && type != null) {
                typeToAdd = type;
            }
        }
        element = (JetProperty) element.replace(newElement);
        if (typeToAdd != null) {
            SpecifyTypeExplicitlyAction.addTypeAnnotation(project, editor, element, typeToAdd);
        }
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetProperty> createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                assert element instanceof JetElement;
                JetProperty property = PsiTreeUtil.getParentOfType(element, JetProperty.class);
                if (property == null) return null;
                return new RemovePartsFromPropertyFix(property);
            }
        };
    }
}
