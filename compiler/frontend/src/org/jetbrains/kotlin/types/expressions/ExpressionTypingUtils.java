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

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.dataClassUtils.DataClassUtilsPackage;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl;
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.JetTypeInfo;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;

public class ExpressionTypingUtils {

    private final ExpressionTypingServices expressionTypingServices;
    private final CallResolver callResolver;
    private final KotlinBuiltIns builtIns;

    public ExpressionTypingUtils(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull CallResolver resolver,
            @NotNull KotlinBuiltIns builtIns
    ) {
        this.expressionTypingServices = expressionTypingServices;
        this.callResolver = resolver;
        this.builtIns = builtIns;
    }

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
    protected static ExpressionReceiver getExpressionReceiver(@NotNull JetExpression expression, @Nullable JetType type) {
        if (type == null) return null;
        return new ExpressionReceiver(expression, type);
    }

    @Nullable
    protected static ExpressionReceiver getExpressionReceiver(@NotNull ExpressionTypingFacade facade, @NotNull JetExpression expression, ExpressionTypingContext context) {
        return getExpressionReceiver(expression, facade.getTypeInfo(expression, context).getType());
    }

    @NotNull
    protected static ExpressionReceiver safeGetExpressionReceiver(@NotNull ExpressionTypingFacade facade, @NotNull JetExpression expression, ExpressionTypingContext context) {
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

    public boolean ensureBooleanResult(JetExpression operationSign, Name name, JetType resultType, ExpressionTypingContext context) {
        return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
    }

    private boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName, ExpressionTypingContext context) {
        if (resultType != null) {
            // TODO : Relax?
            if (!builtIns.isBooleanOrSubtype(resultType)) {
                context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, subjectName, builtIns.getBooleanType(), resultType));
                return false;
            }
        }
        return true;
    }

    @NotNull
    public JetType getDefaultType(IElementType constantType) {
        if (constantType == JetNodeTypes.INTEGER_CONSTANT) {
            return builtIns.getIntType();
        }
        else if (constantType == JetNodeTypes.FLOAT_CONSTANT) {
            return builtIns.getDoubleType();
        }
        else if (constantType == JetNodeTypes.BOOLEAN_CONSTANT) {
            return builtIns.getBooleanType();
        }
        else if (constantType == JetNodeTypes.CHARACTER_CONSTANT) {
            return builtIns.getCharType();
        }
        else if (constantType == JetNodeTypes.NULL) {
            return builtIns.getNullableNothingType();
        }
        else {
            throw new IllegalArgumentException("Unsupported constant type: " + constantType);
        }
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(
            @NotNull ExpressionTypingContext context,
            @NotNull ReceiverValue receiver,
            @NotNull Name name,
            @NotNull JetType... argumentTypes
    ) {
        TemporaryBindingTrace traceWithFakeArgumentInfo = TemporaryBindingTrace.create(context.trace, "trace to store fake argument for",
                                                                                       name);
        List<JetExpression> fakeArguments = Lists.newArrayList();
        for (JetType type : argumentTypes) {
            fakeArguments.add(createFakeExpressionOfType(expressionTypingServices.getProject(), traceWithFakeArgumentInfo,
                                                         "fakeArgument" + fakeArguments.size(), type));
        }
        return makeAndResolveFakeCall(receiver, context.replaceBindingTrace(traceWithFakeArgumentInfo), fakeArguments, name).getSecond();
    }

    public static JetExpression createFakeExpressionOfType(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull String argumentName,
            @NotNull JetType argumentType
    ) {
        JetExpression fakeExpression = JetPsiFactory(project).createExpression(argumentName);
        trace.record(EXPRESSION_TYPE, fakeExpression, argumentType);
        trace.record(PROCESSED, fakeExpression);
        return fakeExpression;
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(
            @NotNull ExpressionTypingContext context,
            @NotNull ReceiverValue receiver,
            @NotNull Name name
    ) {
        return resolveFakeCall(receiver, context, Collections.<JetExpression>emptyList(), name);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(
            @NotNull ReceiverValue receiver,
            @NotNull ExpressionTypingContext context,
            @NotNull List<JetExpression> valueArguments,
            @NotNull Name name
    ) {
        return makeAndResolveFakeCall(receiver, context, valueArguments, name).getSecond();
    }

    @NotNull
    public Pair<Call, OverloadResolutionResults<FunctionDescriptor>> makeAndResolveFakeCall(
            @NotNull ReceiverValue receiver,
            @NotNull ExpressionTypingContext context,
            @NotNull List<JetExpression> valueArguments,
            @NotNull Name name
    ) {
        final JetReferenceExpression fake = JetPsiFactory(expressionTypingServices.getProject()).createSimpleName("fake");
        TemporaryBindingTrace fakeTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve fake call for", name);
        Call call = CallMaker.makeCallWithExpressions(fake, receiver, null, fake, valueArguments);
        OverloadResolutionResults<FunctionDescriptor> results =
                callResolver.resolveCallWithGivenName(context.replaceBindingTrace(fakeTrace), call, fake, name);
        if (results.isSuccess()) {
            fakeTrace.commit(new TraceEntryFilter() {
                @Override
                public boolean accept(@Nullable WritableSlice<?, ?> slice, Object key) {
                    // excluding all entries related to fake expression
                    return key != fake;
                }
            }, true);
        }
        return Pair.create(call, results);
    }

    public void defineLocalVariablesFromMultiDeclaration(
            @NotNull WritableScope writableScope,
            @NotNull JetMultiDeclaration multiDeclaration,
            @NotNull ReceiverValue receiver,
            @NotNull JetExpression reportErrorsOn,
            @NotNull ExpressionTypingContext context
    ) {
        int componentIndex = 1;
        for (JetMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
            Name componentName = DataClassUtilsPackage.createComponentName(componentIndex);
            componentIndex++;

            JetType expectedType = getExpectedTypeForComponent(context, entry);
            OverloadResolutionResults<FunctionDescriptor> results =
                    resolveFakeCall(context.replaceExpectedType(expectedType), receiver, componentName);

            JetType componentType = null;
            if (results.isSuccess()) {
                context.trace.record(COMPONENT_RESOLVED_CALL, entry, results.getResultingCall());
                componentType = results.getResultingDescriptor().getReturnType();
                if (componentType != null && !noExpectedType(expectedType)
                       && !JetTypeChecker.DEFAULT.isSubtypeOf(componentType, expectedType)) {

                    context.trace.report(
                            COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.on(reportErrorsOn, componentName, componentType, expectedType));
                }
            }
            else if (results.isAmbiguity()) {
                context.trace.report(COMPONENT_FUNCTION_AMBIGUITY.on(reportErrorsOn, componentName, results.getResultingCalls()));
            }
            else {
                context.trace.report(COMPONENT_FUNCTION_MISSING.on(reportErrorsOn, componentName, receiver.getType()));
            }
            if (componentType == null) {
                componentType = ErrorUtils.createErrorType(componentName + "() return type");
            }
            VariableDescriptor variableDescriptor = expressionTypingServices.getDescriptorResolver().
                resolveLocalVariableDescriptorWithType(writableScope, entry, componentType, context.trace);

            VariableDescriptor olderVariable = writableScope.getLocalVariable(variableDescriptor.getName());
            checkVariableShadowing(context, variableDescriptor, olderVariable);

            writableScope.addVariableDescriptor(variableDescriptor);
        }
    }

    public static void checkVariableShadowing(@NotNull ExpressionTypingContext context, @NotNull VariableDescriptor variableDescriptor, VariableDescriptor oldDescriptor) {
        if (oldDescriptor != null && isLocal(variableDescriptor.getContainingDeclaration(), oldDescriptor)) {
            PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
            if (declaration != null) {
                context.trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().asString()));
            }
        }
    }

    @NotNull
    private JetType getExpectedTypeForComponent(ExpressionTypingContext context, JetMultiDeclarationEntry entry) {
        JetTypeReference entryTypeRef = entry.getTypeReference();
        if (entryTypeRef != null) {
            return expressionTypingServices.getTypeResolver().resolveType(context.scope, entryTypeRef, context.trace, true);
        }
        else {
            return TypeUtils.NO_EXPECTED_TYPE;
        }
    }

    public static ObservableBindingTrace makeTraceInterceptingTypeMismatch(@NotNull BindingTrace trace, @NotNull final JetElement expressionToWatch, @NotNull final boolean[] mismatchFound) {
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
               : JetTypeInfo.create(null, context.dataFlowInfo);
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
}
