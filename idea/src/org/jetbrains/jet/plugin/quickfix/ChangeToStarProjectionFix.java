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
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetBinaryExpressionWithTypeRHS;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetTypeElement;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.types.expressions.TypeReconstructionUtil;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class ChangeToStarProjectionFix extends JetIntentionAction<JetTypeElement> {
    public ChangeToStarProjectionFix(@NotNull JetTypeElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        String stars = TypeReconstructionUtil.getTypeNameAndStarProjectionsString("", element.getTypeArgumentsAsTypes().size());
        return JetBundle.message("change.to.star.projection", stars);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.to.star.projection.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        for (JetTypeReference typeReference : element.getTypeArgumentsAsTypes()) {
            if (typeReference != null) {
                typeReference.replace(JetPsiFactory(file).createStar());
            }
        }
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetBinaryExpressionWithTypeRHS expression = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpressionWithTypeRHS.class);
                JetTypeReference typeReference;
                if (expression == null) {
                    typeReference = QuickFixUtil.getParentElementOfType(diagnostic, JetTypeReference.class);
                }
                else {
                    typeReference = expression.getRight();
                }
                if (typeReference == null) return null;
                JetTypeElement typeElement = typeReference.getTypeElement();
                assert typeElement != null;
                return new ChangeToStarProjectionFix(typeElement);
            }
        };
    }
}
