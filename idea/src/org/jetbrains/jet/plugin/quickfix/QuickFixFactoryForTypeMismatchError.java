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
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters2;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.LinkedList;
import java.util.List;

public class QuickFixFactoryForTypeMismatchError implements JetIntentionActionsFactory {
    @NotNull
    @Override
    public List<IntentionAction> createActions(Diagnostic diagnostic) {
        List<IntentionAction> actions = new LinkedList<IntentionAction>();

        assert diagnostic.getFactory() == Errors.TYPE_MISMATCH;
        @SuppressWarnings("unchecked")
        DiagnosticWithParameters2<JetExpression, JetType, JetType> diagnosticWithParameters =
                (DiagnosticWithParameters2<JetExpression, JetType, JetType>) diagnostic;
        JetExpression expression = diagnosticWithParameters.getPsiElement();
        JetType expectedType = diagnosticWithParameters.getA();
        JetType expressionType = diagnosticWithParameters.getB();
        BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) diagnostic.getPsiFile()).getBindingContext();

        // We don't want to cast a cast or type-asserted expression:
        if (!(expression instanceof JetBinaryExpressionWithTypeRHS) && !(expression.getParent() instanceof  JetBinaryExpressionWithTypeRHS)) {
            actions.add(new CastExpressionFix(expression, expectedType));
        }

        // Property initializer type mismatch property type:
        JetProperty property = PsiTreeUtil.getParentOfType(expression, JetProperty.class);
        if (property != null && QuickFixUtil.canEvaluateTo(property.getInitializer(), expression)) {
            actions.add(new ChangeVariableTypeFix(property, expressionType));
        }

        // Mismatch in returned expression:
        JetFunction function = PsiTreeUtil.getParentOfType(expression, JetFunction.class, true);
        if (function != null && QuickFixUtil.canFunctionReturnExpression(function, expression)) {
            actions.add(new ChangeFunctionReturnTypeFix(function, expressionType));
        }

        // Change type of a function parameter in case TYPE_MISMATCH is reported on expression passed as value argument of call.
        // 1) When an argument is a dangling function literal:
        JetFunctionLiteralExpression functionLiteralExpression =
                QuickFixUtil.getParentElementOfType(diagnostic, JetFunctionLiteralExpression.class);
        if (functionLiteralExpression != null && functionLiteralExpression.getBodyExpression() == expression) {
            JetParameter correspondingParameter =
                    QuickFixUtil.getFunctionParameterCorrespondingToFunctionLiteralPassedOutsideArgumentList(functionLiteralExpression);
            JetType functionLiteralExpressionType = context.get(BindingContext.EXPRESSION_TYPE, functionLiteralExpression);
            if (correspondingParameter != null && functionLiteralExpressionType != null) {
                actions.add(new ChangeFunctionParameterTypeFix(correspondingParameter, functionLiteralExpressionType));
            }
        }
        // 2) When an argument is passed inside value argument list:
        else {
            JetValueArgument valueArgument = QuickFixUtil.getParentElementOfType(diagnostic, JetValueArgument.class);
            if (valueArgument != null && valueArgument.getArgumentExpression() == expression) {
                JetParameter correspondingParameter = QuickFixUtil.getFunctionParameterCorrespondingToValueArgumentPassedInCall(valueArgument);
                JetType valueArgumentType = context.get(BindingContext.EXPRESSION_TYPE, valueArgument.getArgumentExpression());
                if (correspondingParameter != null && valueArgumentType != null) {
                    actions.add(new ChangeFunctionParameterTypeFix(correspondingParameter, valueArgumentType));
                }
            }
        }
        return actions;
    }
}
