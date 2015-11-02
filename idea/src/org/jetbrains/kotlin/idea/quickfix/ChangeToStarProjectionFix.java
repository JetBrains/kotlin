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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.expressions.TypeReconstructionUtil;

public class ChangeToStarProjectionFix extends KotlinQuickFixAction<KtTypeElement> {
    public ChangeToStarProjectionFix(@NotNull KtTypeElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        String stars = TypeReconstructionUtil.getTypeNameAndStarProjectionsString("", getElement().getTypeArgumentsAsTypes().size());
        return KotlinBundle.message("change.to.star.projection", stars);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("change.to.star.projection.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        for (KtTypeReference typeReference : getElement().getTypeArgumentsAsTypes()) {
            if (typeReference != null) {
                typeReference.replace(KtPsiFactoryKt.KtPsiFactory(file).createStar());
            }
        }
    }

    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                KtBinaryExpressionWithTypeRHS expression = QuickFixUtil
                        .getParentElementOfType(diagnostic, KtBinaryExpressionWithTypeRHS.class);
                KtTypeReference typeReference;
                if (expression == null) {
                    typeReference = QuickFixUtil.getParentElementOfType(diagnostic, KtTypeReference.class);
                }
                else {
                    typeReference = expression.getRight();
                }
                if (typeReference == null) return null;
                KtTypeElement typeElement = typeReference.getTypeElement();
                assert typeElement != null;
                return new ChangeToStarProjectionFix(typeElement);
            }
        };
    }
}
