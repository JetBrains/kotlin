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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.expressions.TypeReconstructionUtil;

public abstract class AddStarProjectionsFix extends KotlinQuickFixAction<KtUserType> {

    private final int argumentCount;

    private AddStarProjectionsFix(@NotNull KtUserType element, int count) {
        super(element);
        argumentCount = count;
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("add.star.projections", TypeReconstructionUtil.getTypeNameAndStarProjectionsString("", argumentCount));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Add star projections";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        assert getElement().getTypeArguments().isEmpty();

        String typeString = TypeReconstructionUtil.getTypeNameAndStarProjectionsString(getElement().getText(), argumentCount);
        KtTypeElement replacement = KtPsiFactoryKt.KtPsiFactory(file).createType(typeString).getTypeElement();
        assert replacement != null : "No type element after parsing " + typeString;

        getElement().replace(replacement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static KotlinSingleIntentionActionFactory createFactoryForIsExpression() {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                DiagnosticWithParameters2<KtTypeReference, Integer, String> diagnosticWithParameters =
                        Errors.NO_TYPE_ARGUMENTS_ON_RHS.cast(diagnostic);
                KtTypeElement typeElement = diagnosticWithParameters.getPsiElement().getTypeElement();
                while (typeElement instanceof KtNullableType) {
                    typeElement = ((KtNullableType) typeElement).getInnerType();
                }
                if (!(typeElement instanceof KtUserType)) return null;
                Integer size = diagnosticWithParameters.getA();
                return new AddStarProjectionsFix((KtUserType) typeElement, size) {};
            }
        };
    }

    public static KotlinSingleIntentionActionFactory createFactoryForJavaClass() {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                DiagnosticWithParameters1<KtElement, Integer> diagnosticWithParameters = Errors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.cast(diagnostic);

                Integer size = diagnosticWithParameters.getA();

                KtUserType userType = QuickFixUtil.getParentElementOfType(diagnostic, KtUserType.class);
                if (userType == null) return null;

                return new AddStarProjectionsFix(userType, size) {
                    @Override
                    public boolean isAvailable(
                            @NotNull Project project, Editor editor, PsiFile file
                    ) {
                        // We are looking for the occurrence of Type in javaClass<Type>()
                        return super.isAvailable(project, editor, file) && isZeroTypeArguments() && isInsideJavaClassCall();
                    }

                    private boolean isZeroTypeArguments() {
                        return getElement().getTypeArguments().isEmpty();
                    }

                    private boolean isInsideJavaClassCall() {
                        PsiElement parent = getElement().getParent().getParent().getParent().getParent();
                        if (parent instanceof KtCallExpression) {
                            KtExpression calleeExpression = ((KtCallExpression) parent).getCalleeExpression();
                            if (calleeExpression instanceof KtSimpleNameExpression) {
                                KtSimpleNameExpression simpleNameExpression = (KtSimpleNameExpression) calleeExpression;
                                // Resolve is expensive so we use a heuristic here: the case is rare enough not to be annoying
                                return "javaClass".equals(simpleNameExpression.getReferencedName());
                            }
                        }
                        return false;
                    }
                };
            }
        };
    }
}
