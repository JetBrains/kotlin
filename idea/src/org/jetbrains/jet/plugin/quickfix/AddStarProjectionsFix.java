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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters2;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.expressions.TypeReconstructionUtil;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public abstract class AddStarProjectionsFix extends JetIntentionAction<JetUserType> {

    private final int argumentCount;

    private AddStarProjectionsFix(@NotNull JetUserType element, int count) {
        super(element);
        argumentCount = count;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("add.star.projections", TypeReconstructionUtil.getTypeNameAndStarProjectionsString("", argumentCount));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Add star projections";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        assert element.getTypeArguments().isEmpty();

        String typeString = TypeReconstructionUtil.getTypeNameAndStarProjectionsString(element.getText(), argumentCount);
        JetTypeElement replacement = JetPsiFactory(file).createType(typeString).getTypeElement();
        assert replacement != null : "No type element after parsing " + typeString;

        element.replace(replacement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static JetSingleIntentionActionFactory createFactoryForIsExpression() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                DiagnosticWithParameters2<JetTypeReference, Integer, String> diagnosticWithParameters =
                        Errors.NO_TYPE_ARGUMENTS_ON_RHS.cast(diagnostic);
                JetTypeElement typeElement = diagnosticWithParameters.getPsiElement().getTypeElement();
                while (typeElement instanceof JetNullableType) {
                    typeElement = ((JetNullableType) typeElement).getInnerType();
                }
                if (!(typeElement instanceof JetUserType)) return null;
                Integer size = diagnosticWithParameters.getA();
                return new AddStarProjectionsFix((JetUserType) typeElement, size) {};
            }
        };
    }

    public static JetSingleIntentionActionFactory createFactoryForJavaClass() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                DiagnosticWithParameters1<JetElement, Integer> diagnosticWithParameters = Errors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.cast(diagnostic);

                Integer size = diagnosticWithParameters.getA();

                JetUserType userType = QuickFixUtil.getParentElementOfType(diagnostic, JetUserType.class);
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
                        return element.getTypeArguments().isEmpty();
                    }

                    private boolean isInsideJavaClassCall() {
                        PsiElement parent = element.getParent().getParent().getParent().getParent();
                        if (parent instanceof JetCallExpression) {
                            JetExpression calleeExpression = ((JetCallExpression) parent).getCalleeExpression();
                            if (calleeExpression instanceof JetSimpleNameExpression) {
                                JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) calleeExpression;
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
