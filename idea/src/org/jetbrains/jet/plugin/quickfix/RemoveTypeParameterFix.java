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
import com.intellij.util.IncorrectOperationException;
import com.sun.tools.internal.xjc.reader.TypeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.plugin.JetBundle;

public class RemoveTypeParameterFix extends JetIntentionAction<JetUserType> {
    public RemoveTypeParameterFix(@NotNull JetUserType element) {
        super(element);
    }

    @NotNull
    private String getTypeName() {
        String typeName = element.getReferencedName();
        assert typeName != null;
        return typeName;
    }

    @NotNull
    private String getFixedType() {
        return TypeUtils.getTypeNameAndStarProjectionsString(getTypeName(), element.getTypeArguments().size());
    }

    @NotNull
    private String getCurrentType() {
        return element.getText();
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.type.parameter.action", getCurrentType(), getFixedType());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.type.parameter.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetTypeElement starProjection = JetPsiFactory.createType(project, getFixedType()).getTypeElement();
        super.element.replace(starProjection);
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetUserType> createAction(Diagnostic diagnostic) {
                //JetUserType userType = QuickFixUtil.getParentElementOfType(diagnostic, JetUserType.class);
                PsiElement psiElement = diagnostic.getPsiElement();
                if (psiElement == null || !(psiElement instanceof JetTypeReference)) return null;
                JetTypeReference typeReference = (JetTypeReference) psiElement;
                JetTypeElement typeElement = typeReference.getTypeElement();
                if (typeElement == null || !(typeElement instanceof  JetUserType)) return null;
                return new RemoveTypeParameterFix((JetUserType)typeElement);
            }
        };
    }
}
