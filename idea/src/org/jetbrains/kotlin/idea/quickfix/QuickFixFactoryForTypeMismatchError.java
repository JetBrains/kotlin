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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.util.UtilPackage;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

//TODO: should use change signature to deal with cases of multiple overridden descriptors
public class QuickFixFactoryForTypeMismatchError extends JetIntentionActionsFactory {
    private final static Logger LOG = Logger.getInstance(QuickFixFactoryForTypeMismatchError.class);

    @NotNull
    @Override
    protected List<IntentionAction> doCreateActions(@NotNull Diagnostic diagnostic) {
        List<IntentionAction> actions = new LinkedList<IntentionAction>();

        BindingContext context = ResolvePackage.analyzeFully((JetFile) diagnostic.getPsiFile());

        PsiElement diagnosticElement = diagnostic.getPsiElement();
        if (!(diagnosticElement instanceof JetExpression)) {
            LOG.error("Unexpected element: " + diagnosticElement.getText());
            return Collections.emptyList();
        }

        JetExpression expression = (JetExpression) diagnosticElement;

        JetType expectedType;
        JetType expressionType;
        if (diagnostic.getFactory() == Errors.TYPE_MISMATCH) {
            DiagnosticWithParameters2<JetExpression, JetType, JetType> diagnosticWithParameters = Errors.TYPE_MISMATCH.cast(diagnostic);
            expectedType = diagnosticWithParameters.getA();
            expressionType = diagnosticWithParameters.getB();
        }
        else if (diagnostic.getFactory() == Errors.NULL_FOR_NONNULL_TYPE) {
            DiagnosticWithParameters1<JetConstantExpression, JetType> diagnosticWithParameters =
                    Errors.NULL_FOR_NONNULL_TYPE.cast(diagnostic);
            expectedType = diagnosticWithParameters.getA();
            expressionType = UtilPackage.makeNullable(expectedType);
        }
        else if (diagnostic.getFactory() == Errors.CONSTANT_EXPECTED_TYPE_MISMATCH) {
            DiagnosticWithParameters2<JetConstantExpression, String, JetType> diagnosticWithParameters =
                    Errors.CONSTANT_EXPECTED_TYPE_MISMATCH.cast(diagnostic);
            expectedType = diagnosticWithParameters.getB();
            expressionType = context.get(BindingContext.EXPRESSION_TYPE, expression);
            if (expressionType == null) {
                LOG.error("No type inferred: " + expression.getText());
                return Collections.emptyList();
            }
        }
        else {
            LOG.error("Unexpected diagnostic: " + DefaultErrorMessages.render(diagnostic));
            return Collections.emptyList();
        }

        // We don't want to cast a cast or type-asserted expression:
        if (!(expression instanceof JetBinaryExpressionWithTypeRHS) && !(expression.getParent() instanceof  JetBinaryExpressionWithTypeRHS)) {
            actions.add(new CastExpressionFix(expression, expectedType));
        }

        // Property initializer type mismatch property type:
        JetProperty property = PsiTreeUtil.getParentOfType(expression, JetProperty.class);
        if (property != null) {
            JetPropertyAccessor getter = property.getGetter();
            if (QuickFixUtil.canEvaluateTo(property.getInitializer(), expression) ||
                (getter != null && QuickFixUtil.canFunctionOrGetterReturnExpression(property.getGetter(), expression))) {
                actions.add(new ChangeVariableTypeFix(property, expressionType));
            }
        }

        // Mismatch in returned expression:
        JetFunction function = PsiTreeUtil.getParentOfType(expression, JetFunction.class, true);
        if (function != null && QuickFixUtil.canFunctionOrGetterReturnExpression(function, expression)) {
            actions.add(new ChangeFunctionReturnTypeFix(function, expressionType));
        }

        // Fixing overloaded operators:
        if (expression instanceof JetOperationExpression) {
            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(expression, context);
            if (resolvedCall != null) {
                JetFunction declaration = getFunctionDeclaration(resolvedCall);
                if (declaration != null) {
                    actions.add(new ChangeFunctionReturnTypeFix(declaration, expectedType));
                }
            }
        }
        if (expression.getParent() instanceof JetBinaryExpression) {
            JetBinaryExpression parentBinary = (JetBinaryExpression) expression.getParent();
            if (parentBinary.getRight() == expression) {
                ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(parentBinary, context);
                if (resolvedCall != null) {
                    JetFunction declaration = getFunctionDeclaration(resolvedCall);
                    if (declaration != null) {
                        JetParameter binaryOperatorParameter = declaration.getValueParameters().get(0);
                        actions.add(new ChangeParameterTypeFix(binaryOperatorParameter, expressionType));
                    }
                }
            }
        }

        // Change function return type when TYPE_MISMATCH is reported on call expression:
        if (expression instanceof JetCallExpression) {
            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(expression, context);
            if (resolvedCall != null) {
                JetFunction declaration = getFunctionDeclaration(resolvedCall);
                if (declaration != null) {
                    actions.add(new ChangeFunctionReturnTypeFix(declaration, expectedType));
                }
            }
        }

        ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilPackage.getParentResolvedCall(expression, context, true);
        if (resolvedCall != null) {
            // to fix 'type mismatch' on 'if' branches
            // todo: the same with 'when'
            JetExpression parentIf = QuickFixUtil.getParentIfForBranch(expression);
            JetExpression argumentExpression = (parentIf != null) ? parentIf : expression;
            ValueArgument valueArgument = CallUtilPackage.getValueArgumentForExpression(resolvedCall.getCall(), argumentExpression);
            if (valueArgument != null) {
                JetParameter correspondingParameter = QuickFixUtil.getParameterDeclarationForValueArgument(resolvedCall, valueArgument);
                JetType valueArgumentType = diagnostic.getFactory() == Errors.NULL_FOR_NONNULL_TYPE
                                            ? expressionType
                                            : context.get(BindingContext.EXPRESSION_TYPE, valueArgument.getArgumentExpression());
                if (correspondingParameter != null && valueArgumentType != null) {
                    actions.add(new ChangeParameterTypeFix(correspondingParameter, valueArgumentType));
                }
            }
        }
        return actions;
    }

    @Nullable
    private static JetFunction getFunctionDeclaration(@NotNull ResolvedCall<?> resolvedCall) {
        PsiElement result = QuickFixUtil.safeGetDeclaration(resolvedCall.getResultingDescriptor());
        if (result instanceof JetFunction) {
            return (JetFunction) result;
        }
        return null;
    }
}
