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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters2;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetTypeElement;
import org.jetbrains.jet.lang.psi.JetUserType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.plugin.JetBundle;

public class AddStarProjectionsFix extends JetIntentionAction<JetUserType> {

    private final int argumentCount;

    private AddStarProjectionsFix(@NotNull JetUserType element, int count) {
        super(element);
        argumentCount = count;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("add.star.projections", TypeUtils.getTypeNameAndStarProjectionsString("", argumentCount));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Add star projections";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        assert element.getTypeArguments().isEmpty();

        String typeString = TypeUtils.getTypeNameAndStarProjectionsString(element.getText(), argumentCount);
        JetTypeElement replacement = JetPsiFactory.createType(project, typeString).getTypeElement();
        assert replacement != null : "No type element after parsing " + typeString;

        element.replace(replacement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                assert diagnostic.getFactory() == Errors.NO_TYPE_ARGUMENTS_ON_RHS_OF_IS_EXPRESSION;
                @SuppressWarnings("unchecked")
                DiagnosticWithParameters2<JetUserType, Integer, String> diagnosticWithParameters =
                        (DiagnosticWithParameters2<JetUserType, Integer, String>) diagnostic;
                JetUserType userType = QuickFixUtil.getParentElementOfType(diagnostic, JetUserType.class);
                if (userType == null) return null;
                Integer size = diagnosticWithParameters.getA();
                return new AddStarProjectionsFix(userType, size);
            }
        };
    }
}
