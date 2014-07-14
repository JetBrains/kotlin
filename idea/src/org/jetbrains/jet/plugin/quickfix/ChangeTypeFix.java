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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class ChangeTypeFix extends JetIntentionAction<JetTypeReference> {
    private final String renderedType;

    public ChangeTypeFix(@NotNull JetTypeReference element, JetType type) {
        super(element);
        renderedType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.type", element.getText(), renderedType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        element.replace(JetPsiFactory(file).createType(renderedType));
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForExpectedParameterTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                DiagnosticWithParameters1<JetParameter, JetType> diagnosticWithParameters = Errors.EXPECTED_PARAMETER_TYPE_MISMATCH.cast(diagnostic);
                JetTypeReference typeReference = diagnosticWithParameters.getPsiElement().getTypeReference();
                assert typeReference != null : "EXPECTED_PARAMETER_TYPE_MISMATCH reported on parameter without explicitly declared type";
                return new ChangeTypeFix(typeReference, diagnosticWithParameters.getA());
            }
        };
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForExpectedReturnTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                DiagnosticWithParameters1<JetTypeReference, JetType> diagnosticWithParameters = Errors.EXPECTED_RETURN_TYPE_MISMATCH.cast(diagnostic);
                return new ChangeTypeFix(diagnosticWithParameters.getPsiElement(), diagnosticWithParameters.getA());
            }
        };
    }
}
