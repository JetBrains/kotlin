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
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl;
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.kotlin.resolve.BindingContext.PROCESSED;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;

public class ExpressionTypingUtils {

    @NotNull
    public static ReceiverValue normalizeReceiverValueForVisibility(@NotNull ReceiverValue receiverValue, @NotNull BindingContext trace) {
        if (receiverValue instanceof ExpressionReceiver) {
            JetExpression expression = ((ExpressionReceiver) receiverValue).getExpression();
            JetReferenceExpression referenceExpression = null;
            if (expression instanceof JetThisExpression) {
                referenceExpression = ((JetThisExpression) expression).getInstanceReference();
            }
            else if (expression instanceof JetConstructorDelegationReferenceExpression) {
                referenceExpression = (JetReferenceExpression) expression;
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
    public static ExpressionReceiver getExpressionReceiver(@NotNull JetExpression expression, @Nullable JetType type) {
        if (type == null) return null;
        return new ExpressionReceiver(expression, type);
    }

    @Nullable
    public static ExpressionReceiver getExpressionReceiver(
            @NotNull ExpressionTypingFacade facade,
            @NotNull JetExpression expression,
            ExpressionTypingContext context
    ) {
        return getExpressionReceiver(expression, facade.getTypeInfo(expression, context).getType());
    }

    @NotNull
    public static ExpressionReceiver safeGetExpressionReceiver(
            @NotNull ExpressionTypingFacade facade,
            @NotNull JetExpression expression,
            ExpressionTypingContext context
    ) {
        JetType type = safeGetType(facade.safeGetTypeInfo(expression, context));
        return new ExpressionReceiver(expression, type);
    }

    @NotNull
    public static JetType safeGetType(@NotNull JetTypeInfo typeInfo) {
        JetType type = typeInfo.getType();
        assert type != null : "safeGetType should be invoked on safe JetTypeInfo; safeGetTypeInfo should return @NotNull type";
        return type;
    }

    @NotNull
    public static WritableScopeImpl newWritableScopeImpl(ExpressionTypingContext context, @NotNull String scopeDebugName) {
        WritableScopeImpl scope = new WritableScopeImpl(
                context.scope, context.scope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(context.trace), scopeDebugName);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        return scope;
    }

    public static JetExpression createFakeExpressionOfType(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull String argumentName,
            @NotNull JetType argumentType
    ) {
        JetExpression fakeExpression = JetPsiFactory(project).createExpression(argumentName);
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
            @NotNull final JetElement expressionToWatch,
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
            @Nullable JetExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull ExpressionTypingInternals facade
    ) {
        return expression != null
               ? facade.getTypeInfo(expression, context)
               : TypeInfoFactoryPackage.noTypeInfo(context);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public static boolean isBinaryExpressionDependentOnExpectedType(@NotNull JetBinaryExpression expression) {
        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
        return (operationType == JetTokens.IDENTIFIER || OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)
                || operationType == JetTokens.ELVIS);
    }

    public static boolean isUnaryExpressionDependentOnExpectedType(@NotNull JetUnaryExpression expression) {
        return expression.getOperationReference().getReferencedNameElementType() == JetTokens.EXCLEXCL;
    }

    public static boolean isExclExclExpression(@Nullable JetExpression expression) {
        return expression instanceof JetUnaryExpression
               && ((JetUnaryExpression) expression).getOperationReference().getReferencedNameElementType() == JetTokens.EXCLEXCL;
    }

    @NotNull
    public static List<JetType> getValueParametersTypes(@NotNull List<ValueParameterDescriptor> valueParameters) {
        List<JetType> parameterTypes = new ArrayList<JetType>(valueParameters.size());
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

    public static boolean dependsOnExpectedType(@Nullable JetExpression expression) {
        JetExpression expr = JetPsiUtil.deparenthesize(expression, false);
        if (expr == null) return false;

        if (expr instanceof JetBinaryExpressionWithTypeRHS) {
            return false;
        }
        if (expr instanceof JetBinaryExpression) {
            return isBinaryExpressionDependentOnExpectedType((JetBinaryExpression) expr);
        }
        if (expr instanceof JetUnaryExpression) {
            return isUnaryExpressionDependentOnExpectedType((JetUnaryExpression) expr);
        }
        return true;
    }

    private ExpressionTypingUtils() {
    }

}
