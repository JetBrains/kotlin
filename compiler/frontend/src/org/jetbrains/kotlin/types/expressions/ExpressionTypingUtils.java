/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.impl.FunctionExpressionDescriptor;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.ObservableBindingTrace;
import org.jetbrains.kotlin.resolve.OverloadChecker;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.TraceBasedLocalRedeclarationChecker;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import static org.jetbrains.kotlin.diagnostics.Errors.TYPE_INFERENCE_ERRORS;
import static org.jetbrains.kotlin.resolve.BindingContext.PROCESSED;

public class ExpressionTypingUtils {

    @Nullable
    public static ExpressionReceiver getExpressionReceiver(
            @NotNull ExpressionTypingFacade facade,
            @NotNull KtExpression expression,
            ExpressionTypingContext context
    ) {
        KotlinType type = facade.getTypeInfo(expression, context).getType();
        if (type == null) return null;
        return ExpressionReceiver.Companion.create(expression, type, context.trace.getBindingContext());
    }

    @NotNull
    public static ExpressionReceiver safeGetExpressionReceiver(
            @NotNull ExpressionTypingFacade facade,
            @NotNull KtExpression expression,
            ExpressionTypingContext context
    ) {
        KotlinType type = safeGetType(facade.safeGetTypeInfo(expression, context));
        return ExpressionReceiver.Companion.create(expression, type, context.trace.getBindingContext());
    }

    @NotNull
    public static KotlinType safeGetType(@NotNull KotlinTypeInfo typeInfo) {
        KotlinType type = typeInfo.getType();
        assert type != null : "safeGetType should be invoked on safe KotlinTypeInfo; safeGetTypeInfo should return @NotNull type";
        return type;
    }

    @NotNull
    public static LexicalWritableScope newWritableScopeImpl(
            @NotNull ExpressionTypingContext context,
            @NotNull LexicalScopeKind scopeKind,
            @NotNull OverloadChecker overloadChecker
    ) {
        return new LexicalWritableScope(context.scope, context.scope.getOwnerDescriptor(), false,
                                        new TraceBasedLocalRedeclarationChecker(context.trace, overloadChecker), scopeKind);
    }

    public static KtExpression createFakeExpressionOfType(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull String argumentName,
            @NotNull KotlinType argumentType
    ) {
        KtExpression fakeExpression = KtPsiFactoryKt.KtPsiFactory(project, false).createExpression(argumentName);
        trace.recordType(fakeExpression, argumentType);
        trace.record(PROCESSED, fakeExpression);
        return fakeExpression;
    }

    @SuppressWarnings("deprecation")
    public static void checkVariableShadowing(
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            @NotNull VariableDescriptor variableDescriptor
    ) {
        VariableDescriptor oldDescriptor = ScopeUtilsKt.findLocalVariable(scope, variableDescriptor.getName());
        if (oldDescriptor == null) return;

        DeclarationDescriptor variableContainingDeclaration = variableDescriptor.getContainingDeclaration();
        if (!isLocal(variableContainingDeclaration, oldDescriptor)) return;

        if (variableDescriptor instanceof ParameterDescriptor) {
            if (!isFunctionLiteral(variableContainingDeclaration)) {
                return;
            }

            // parameter of lambda
            if (variableContainingDeclaration.getContainingDeclaration() != oldDescriptor.getContainingDeclaration()) {
                return;
            }
        }

        PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
        if (declaration != null) {
            if (declaration instanceof KtDestructuringDeclarationEntry && declaration.getParent().getParent() instanceof KtParameter) {
                // foo { a, (a, b) -> } -- do not report NAME_SHADOWING on the second 'a', because REDECLARATION must be reported here
                PsiElement oldElement = DescriptorToSourceUtils.descriptorToDeclaration(oldDescriptor);

                if (oldElement != null && oldElement.getParent().equals(declaration.getParent().getParent().getParent())) return;
            }
            trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().asString()));
        }
    }

    public static ObservableBindingTrace makeTraceInterceptingTypeMismatch(
            @NotNull BindingTrace trace,
            @NotNull KtElement expressionToWatch,
            @NotNull boolean[] mismatchFound
    ) {
        return new ObservableBindingTrace(trace) {

            @Override
            public void report(@NotNull Diagnostic diagnostic) {
                DiagnosticFactory<?> factory = diagnostic.getFactory();
                if (Errors.TYPE_MISMATCH_ERRORS.contains(factory) && diagnostic.getPsiElement() == expressionToWatch) {
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
    public static KotlinTypeInfo getTypeInfoOrNullType(
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

    public static boolean isFunctionLiteral(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof AnonymousFunctionDescriptor;
    }

    public static boolean isLocalFunction(@Nullable DeclarationDescriptor descriptor) {
        if (descriptor != null && descriptor.getClass() == SimpleFunctionDescriptorImpl.class) {
            return ((SimpleFunctionDescriptorImpl) descriptor).getVisibility() == Visibilities.LOCAL;
        }
        return false;
    }

    public static boolean isFunctionExpression(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof FunctionExpressionDescriptor;
    }
}
