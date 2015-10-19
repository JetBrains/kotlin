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

package org.jetbrains.kotlin.types.expressions;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KtType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.PROCESSED;

public class ExpressionTypingUtils {

    @NotNull
    public static ReceiverValue normalizeReceiverValueForVisibility(@NotNull ReceiverValue receiverValue, @NotNull BindingContext trace) {
        if (receiverValue instanceof ExpressionReceiver) {
            KtExpression expression = ((ExpressionReceiver) receiverValue).getExpression();
            KtReferenceExpression referenceExpression = null;
            if (expression instanceof KtThisExpression) {
                referenceExpression = ((KtThisExpression) expression).getInstanceReference();
            }
            else if (expression instanceof KtConstructorDelegationReferenceExpression) {
                referenceExpression = (KtReferenceExpression) expression;
            }

            if (referenceExpression != null) {
                DeclarationDescriptor descriptor = trace.get(BindingContext.REFERENCE_TARGET, referenceExpression);
                if (descriptor instanceof ClassDescriptor) {
                    return new ClassReceiver((ClassDescriptor) descriptor.getOriginal());
                }
            }
        }
        return receiverValue;
    }

    @Nullable
    public static ExpressionReceiver getExpressionReceiver(@NotNull KtExpression expression, @Nullable KtType type) {
        if (type == null) return null;
        return new ExpressionReceiver(expression, type);
    }

    @Nullable
    public static ExpressionReceiver getExpressionReceiver(
            @NotNull ExpressionTypingFacade facade,
            @NotNull KtExpression expression,
            ExpressionTypingContext context
    ) {
        return getExpressionReceiver(expression, facade.getTypeInfo(expression, context).getType());
    }

    @NotNull
    public static ExpressionReceiver safeGetExpressionReceiver(
            @NotNull ExpressionTypingFacade facade,
            @NotNull KtExpression expression,
            ExpressionTypingContext context
    ) {
        KtType type = safeGetType(facade.safeGetTypeInfo(expression, context));
        return new ExpressionReceiver(expression, type);
    }

    @NotNull
    public static KtType safeGetType(@NotNull JetTypeInfo typeInfo) {
        KtType type = typeInfo.getType();
        assert type != null : "safeGetType should be invoked on safe JetTypeInfo; safeGetTypeInfo should return @NotNull type";
        return type;
    }

    @NotNull
    public static LexicalWritableScope newWritableScopeImpl(ExpressionTypingContext context, @NotNull String scopeDebugName) {
        LexicalWritableScope scope = new LexicalWritableScope(
                context.scope, context.scope.getOwnerDescriptor(), false, null, new TraceBasedRedeclarationHandler(context.trace), scopeDebugName);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        return scope;
    }

    public static KtExpression createFakeExpressionOfType(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull String argumentName,
            @NotNull KtType argumentType
    ) {
        KtExpression fakeExpression = KtPsiFactoryKt.KtPsiFactory(project).createExpression(argumentName);
        trace.recordType(fakeExpression, argumentType);
        trace.record(PROCESSED, fakeExpression);
        return fakeExpression;
    }

    public static void checkVariableShadowing(
            @NotNull ExpressionTypingContext context,
            @NotNull VariableDescriptor variableDescriptor,
            @Nullable VariableDescriptor oldDescriptor
    ) {
        if (oldDescriptor != null && isLocal(variableDescriptor.getContainingDeclaration(), oldDescriptor)) {
            PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
            if (declaration != null) {
                context.trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().asString()));
            }
        }
    }

    public static ObservableBindingTrace makeTraceInterceptingTypeMismatch(
            @NotNull BindingTrace trace,
            @NotNull final KtElement expressionToWatch,
            @NotNull final boolean[] mismatchFound
    ) {
        return new ObservableBindingTrace(trace) {

            @Override
            public void report(@NotNull Diagnostic diagnostic) {
                DiagnosticFactory<?> factory = diagnostic.getFactory();
                if ((factory == TYPE_MISMATCH || factory == CONSTANT_EXPECTED_TYPE_MISMATCH || factory == NULL_FOR_NONNULL_TYPE)
                    && diagnostic.getPsiElement() == expressionToWatch) {
                    mismatchFound[0] = true;
                }
                if (TYPE_INFERENCE_ERRORS.contains(factory) &&
                    PsiTreeUtil.isAncestor(expressionToWatch, diagnostic.getPsiElement(), false)) {
                    mismatchFound[0] = true;
                }
                super.report(diagnostic);
            }
        };
    }

    @NotNull
    public static JetTypeInfo getTypeInfoOrNullType(
            @Nullable KtExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull ExpressionTypingInternals facade
    ) {
        return expression != null
               ? facade.getTypeInfo(expression, context)
               : TypeInfoFactoryKt.noTypeInfo(context);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public static boolean isBinaryExpressionDependentOnExpectedType(@NotNull KtBinaryExpression expression) {
        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
        return (operationType == KtTokens.IDENTIFIER || OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)
                || operationType == KtTokens.ELVIS);
    }

    public static boolean isUnaryExpressionDependentOnExpectedType(@NotNull KtUnaryExpression expression) {
        return expression.getOperationReference().getReferencedNameElementType() == KtTokens.EXCLEXCL;
    }

    public static boolean isExclExclExpression(@Nullable KtExpression expression) {
        return expression instanceof KtUnaryExpression
               && ((KtUnaryExpression) expression).getOperationReference().getReferencedNameElementType() == KtTokens.EXCLEXCL;
    }

    @NotNull
    public static List<KtType> getValueParametersTypes(@NotNull List<ValueParameterDescriptor> valueParameters) {
        List<KtType> parameterTypes = new ArrayList<KtType>(valueParameters.size());
        for (ValueParameterDescriptor parameter : valueParameters) {
            parameterTypes.add(parameter.getType());
        }
        return parameterTypes;
    }

    /**
     * The primary case for local extensions is the following:
     *
     * I had a locally declared extension function or a local variable of function type called foo
     * And I called it on my x
     * Now, someone added function foo() to the class of x
     * My code should not change
     *
     * thus
     *
     * local extension prevail over members (and members prevail over all non-local extensions)
     */
    public static boolean isLocal(DeclarationDescriptor containerOfTheCurrentLocality, DeclarationDescriptor candidate) {
        if (candidate instanceof ValueParameterDescriptor) {
            return true;
        }
        DeclarationDescriptor parent = candidate.getContainingDeclaration();
        if (!(parent instanceof FunctionDescriptor)) {
            return false;
        }
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) parent;
        DeclarationDescriptor current = containerOfTheCurrentLocality;
        while (current != null) {
            if (current == functionDescriptor) {
                return true;
            }
            current = current.getContainingDeclaration();
        }
        return false;
    }

    public static boolean dependsOnExpectedType(@Nullable KtExpression expression) {
        KtExpression expr = KtPsiUtil.deparenthesize(expression);
        if (expr == null) return false;

        if (expr instanceof KtBinaryExpressionWithTypeRHS) {
            return false;
        }
        if (expr instanceof KtBinaryExpression) {
            return isBinaryExpressionDependentOnExpectedType((KtBinaryExpression) expr);
        }
        if (expr instanceof KtUnaryExpression) {
            return isUnaryExpressionDependentOnExpectedType((KtUnaryExpression) expr);
        }
        return true;
    }

    private ExpressionTypingUtils() {
    }

}
