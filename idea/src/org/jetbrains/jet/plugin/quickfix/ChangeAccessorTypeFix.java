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
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;

public class ChangeAccessorTypeFix extends JetIntentionAction<JetPropertyAccessor> {
    public ChangeAccessorTypeFix(@NotNull JetPropertyAccessor element) {
        super(element);
    }

    @Nullable
    private JetType getPropertyType() {
        JetProperty property = PsiTreeUtil.getParentOfType(element, JetProperty.class);
        if (property == null) return null;
        return QuickFixUtil.getDeclarationReturnType(property);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        JetType type = getPropertyType();
        return super.isAvailable(project, editor, file) && type != null && !ErrorUtils.isErrorType(type);
    }

    @NotNull
    @Override
    public String getText() {
        JetType type = getPropertyType();
        return element.isGetter()
               ? JetBundle.message("change.getter.type", type)
               : JetBundle.message("change.setter.type", type);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.accessor.type");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetType type = getPropertyType();
        if (type == null) return;
        JetPropertyAccessor newElement = (JetPropertyAccessor) element.copy();
        JetTypeReference newTypeReference = JetPsiFactory.createType(project, type.toString());

        if (element.isGetter()) {
            JetTypeReference returnTypeReference = newElement.getReturnTypeReference();
            assert returnTypeReference != null;
            CodeEditUtil.replaceChild(newElement.getNode(), returnTypeReference.getNode(), newTypeReference.getNode());
        }
        else {
            JetParameter parameter = newElement.getParameter();
            assert parameter != null;
            JetTypeReference typeReference = parameter.getTypeReference();
            assert typeReference != null;
            CodeEditUtil.replaceChild(parameter.getNode(), typeReference.getNode(), newTypeReference.getNode());
        }
        element.replace(newElement);
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetPropertyAccessor> createAction(Diagnostic diagnostic) {
                JetPropertyAccessor accessor = QuickFixUtil.getParentElementOfType(diagnostic, JetPropertyAccessor.class);
                if (accessor == null) return null;
                return new ChangeAccessorTypeFix(accessor);
            }
        };
    }
}
