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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.rendering.Renderers;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemCompleter;
import org.jetbrains.kotlin.resolve.calls.inference.TypeVariableKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.scopes.ScopeUtils;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.validation.OperatorValidator;
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.FakeCallResolver;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.psi.KtPsiFactoryKt.KtPsiFactory;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.FROM_COMPLETER;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.createFakeExpressionOfType;

public class DelegatedPropertyResolver {

    public static final Name PROPERTY_DELEGATED_FUNCTION_NAME = Name.identifier("propertyDelegated");
    public static final Name GETTER_NAME = Name.identifier("getValue");
    public static final Name SETTER_NAME = Name.identifier("setValue");

    public static final Name OLD_GETTER_NAME = Name.identifier("get");
    public static final Name OLD_SETTER_NAME = Name.identifier("set");

    @NotNull private final ExpressionTypingServices expressionTypingServices;
    @NotNull private final FakeCallResolver fakeCallResolver;
    @NotNull private final KotlinBuiltIns builtIns;
    @NotNull private final SymbolUsageValidator symbolUsageValidator;

    public DelegatedPropertyResolver(
            @NotNull SymbolUsageValidator symbolUsageValidator,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull FakeCallResolver fakeCallResolver,
            @NotNull ExpressionTypingServices expressionTypingServices
    ) {
        this.symbolUsageValidator = symbolUsageValidator;
        this.builtIns = builtIns;
        this.fakeCallResolver = fakeCallResolver;
        this.expressionTypingServices = expressionTypingServices;
    }

    @Nullable
    public KotlinType getDelegatedPropertyGetMethodReturnType(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression delegateExpression,
            @NotNull KotlinType delegateType,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope delegateFunctionsScope
    ) {
        resolveDelegatedPropertyConventionMethod(propertyDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, true);
        ResolvedCall<FunctionDescriptor> resolvedCall =
                trace.getBindingContext().get(DELEGATED_PROPERTY_RESOLVED_CALL, propertyDescriptor.getGetter());
        return resolvedCall != null ? resolvedCall.getResultingDescriptor().getReturnType() : null;
    }

    public void resolveDelegatedPropertyGetMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression delegateExpression,
            @NotNull KotlinType delegateType,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope delegateFunctionsScope
    ) {
        KotlinType returnType = getDelegatedPropertyGetMethodReturnType(
                propertyDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope);
        KotlinType propertyType = propertyDescriptor.getType();

        /* Do not check return type of get() method of delegate for properties with DeferredType because property type is taken from it */
        if (!(propertyType instanceof DeferredType) && returnType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(returnType, propertyType)) {
            Call call = trace.getBindingContext().get(DELEGATED_PROPERTY_CALL, propertyDescriptor.getGetter());
            assert call != null : "Call should exists for " + propertyDescriptor.getGetter();
            trace.report(DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH
                                 .on(delegateExpression, renderCall(call, trace.getBindingContext()), propertyDescriptor.getType(), returnType));
        }
    }

    public void resolveDelegatedPropertySetMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression delegateExpression,
            @NotNull KotlinType delegateType,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope delegateFunctionsScope
    ) {
        resolveDelegatedPropertyConventionMethod(propertyDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, false);
    }

    @NotNull
    private static KtExpression createExpressionForProperty(@NotNull KtPsiFactory psiFactory) {
        return psiFactory.createExpression("null as " + KotlinBuiltIns.FQ_NAMES.kProperty.asSingleFqName().asString() + "<*>");
    }

    public void resolveDelegatedPropertyPDMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression delegateExpression,
            @NotNull KotlinType delegateType,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope delegateFunctionsScope
    ) {
        TemporaryBindingTrace traceToResolvePDMethod = TemporaryBindingTrace.create(trace, "Trace to resolve propertyDelegated method in delegated property");
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                traceToResolvePDMethod, delegateFunctionsScope,
                DataFlowInfoFactory.EMPTY, TypeUtils.NO_EXPECTED_TYPE);

        KtPsiFactory psiFactory = KtPsiFactory(delegateExpression);
        List<KtExpression> arguments = Collections.singletonList(createExpressionForProperty(psiFactory));
        ExpressionReceiver receiver = ExpressionReceiver.Companion.create(delegateExpression, delegateType, trace.getBindingContext());

        Pair<Call, OverloadResolutionResults<FunctionDescriptor>> resolutionResult =
                fakeCallResolver.makeAndResolveFakeCallInContext(receiver, context, arguments, PROPERTY_DELEGATED_FUNCTION_NAME, delegateExpression);

        Call call = resolutionResult.getFirst();
        OverloadResolutionResults<FunctionDescriptor> functionResults = resolutionResult.getSecond();

        if (!functionResults.isSuccess()) {
            String expectedFunction = renderCall(call, traceToResolvePDMethod.getBindingContext());
            if (functionResults.isIncomplete() || functionResults.isSingleResult() ||
                functionResults.getResultCode() == OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES) {
                trace.report(DELEGATE_PD_METHOD_NONE_APPLICABLE.on(delegateExpression, expectedFunction, functionResults.getResultingCalls()));
            } else if (functionResults.isAmbiguity()) {
                trace.report(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY
                                     .on(delegateExpression, expectedFunction, functionResults.getResultingCalls()));
            }
            return;
        }

        trace.record(DELEGATED_PROPERTY_PD_RESOLVED_CALL, propertyDescriptor, functionResults.getResultingCall());
    }

    /* Resolve getValue() or setValue() methods from delegate */
    private void resolveDelegatedPropertyConventionMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression delegateExpression,
            @NotNull KotlinType delegateType,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope delegateFunctionsScope,
            boolean isGet
    ) {
        PropertyAccessorDescriptor accessor = isGet ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
        assert accessor != null : "Delegated property should have getter/setter " + propertyDescriptor + " " + delegateExpression.getText();

        if (trace.getBindingContext().get(DELEGATED_PROPERTY_CALL, accessor) != null) return;

        OverloadResolutionResults<FunctionDescriptor> functionResults = getDelegatedPropertyConventionMethod(
                propertyDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, isGet, true);
        Call call = trace.getBindingContext().get(DELEGATED_PROPERTY_CALL, accessor);
        assert call != null : "'getDelegatedPropertyConventionMethod' didn't record a call";

        if (!functionResults.isSuccess()) {
            String expectedFunction = renderCall(call, trace.getBindingContext());
            if (functionResults.isSingleResult() || functionResults.isIncomplete() ||
                     functionResults.getResultCode() == OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES) {
                trace.report(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE
                                     .on(delegateExpression, expectedFunction, functionResults.getResultingCalls()));
            }
            else if (functionResults.isAmbiguity()) {
                trace.report(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY
                                             .on(delegateExpression, expectedFunction, functionResults.getResultingCalls()));
            }
            else {
                trace.report(DELEGATE_SPECIAL_FUNCTION_MISSING.on(delegateExpression, expectedFunction, delegateType));
            }
            return;
        }

        FunctionDescriptor resultingDescriptor = functionResults.getResultingDescriptor();

        ResolvedCall<FunctionDescriptor> resultingCall = functionResults.getResultingCall();
        PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor);
        if (declaration instanceof KtProperty) {
            KtProperty property = (KtProperty) declaration;
            KtPropertyDelegate delegate = property.getDelegate();
            if (delegate != null) {
                PsiElement byKeyword = delegate.getByKeywordNode().getPsi();

                if (!resultingDescriptor.isOperator()) {
                    OperatorValidator.Companion.report(byKeyword, resultingDescriptor, trace);
                }

                symbolUsageValidator.validateCall(resultingCall, resultingCall.getResultingDescriptor(), trace, byKeyword);
            }
        }
        trace.record(DELEGATED_PROPERTY_RESOLVED_CALL, accessor, resultingCall);
    }

    /* Resolve getValue() or setValue() methods from delegate */
    public OverloadResolutionResults<FunctionDescriptor> getDelegatedPropertyConventionMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression delegateExpression,
            @NotNull KotlinType delegateType,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope delegateFunctionsScope,
            boolean isGet,
            boolean isComplete
    ) {
        PropertyAccessorDescriptor accessor = isGet ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
        assert accessor != null : "Delegated property should have getter/setter " + propertyDescriptor + " " + delegateExpression.getText();

        KotlinType expectedType = isComplete && isGet && !(propertyDescriptor.getType() instanceof DeferredType)
                               ? propertyDescriptor.getType() : TypeUtils.NO_EXPECTED_TYPE;

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace, delegateFunctionsScope,
                DataFlowInfoFactory.EMPTY, expectedType);

        boolean hasThis = propertyDescriptor.getExtensionReceiverParameter() != null || propertyDescriptor.getDispatchReceiverParameter() != null;

        List<KtExpression> arguments = Lists.newArrayList();
        KtPsiFactory psiFactory = KtPsiFactory(delegateExpression);
        arguments.add(psiFactory.createExpression(hasThis ? "this" : "null"));
        arguments.add(createExpressionForProperty(psiFactory));

        if (!isGet) {
            KtReferenceExpression fakeArgument = (KtReferenceExpression) createFakeExpressionOfType(delegateExpression.getProject(), trace,
                                                                                                      "fakeArgument" + arguments.size(),
                                                                                                    propertyDescriptor.getType());
            arguments.add(fakeArgument);
            List<ValueParameterDescriptor> valueParameters = accessor.getValueParameters();
            trace.record(REFERENCE_TARGET, fakeArgument, valueParameters.get(0));
        }

        Name functionName = isGet ? GETTER_NAME : SETTER_NAME;
        ExpressionReceiver receiver = ExpressionReceiver.Companion.create(delegateExpression, delegateType, trace.getBindingContext());

        Pair<Call, OverloadResolutionResults<FunctionDescriptor>> resolutionResult =
                fakeCallResolver.makeAndResolveFakeCallInContext(receiver, context, arguments, functionName, delegateExpression);

        OverloadResolutionResults<FunctionDescriptor> resolutionResults = resolutionResult.getSecond();

        // Resolve get/set is getValue/setValue was not found. Temporary, for code migration
        if (!resolutionResults.isSuccess() && !resolutionResults.isAmbiguity()) {
            Name oldFunctionName = isGet ? OLD_GETTER_NAME : OLD_SETTER_NAME;
            Pair<Call, OverloadResolutionResults<FunctionDescriptor>> additionalResolutionResult =
                    fakeCallResolver.makeAndResolveFakeCallInContext(receiver, context, arguments, oldFunctionName, delegateExpression);
            if (additionalResolutionResult.getSecond().isSuccess()) {
                FunctionDescriptor resultingDescriptor = additionalResolutionResult.getSecond().getResultingDescriptor();

                PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor);
                if (declaration instanceof KtProperty) {
                    KtProperty property = (KtProperty) declaration;
                    KtPropertyDelegate delegate = property.getDelegate();
                    if (delegate != null) {
                        PsiElement byKeyword = delegate.getByKeywordNode().getPsi();

                        trace.report(DELEGATE_RESOLVED_TO_DEPRECATED_CONVENTION.on(
                                byKeyword, resultingDescriptor, delegateType, functionName.asString()));
                    }
                }

                trace.record(BindingContext.DELEGATED_PROPERTY_CALL, accessor, additionalResolutionResult.getFirst());
                return additionalResolutionResult.getSecond();
            }
        }

        trace.record(BindingContext.DELEGATED_PROPERTY_CALL, accessor, resolutionResult.getFirst());
        return resolutionResults;
    }

    private static String renderCall(@NotNull Call call, @NotNull BindingContext context) {
        KtExpression calleeExpression = call.getCalleeExpression();
        assert calleeExpression != null : "CalleeExpression should exists for fake call of convention method";
        StringBuilder builder = new StringBuilder(calleeExpression.getText());
        builder.append("(");
        List<KotlinType> argumentTypes = Lists.newArrayList();
        for (ValueArgument argument : call.getValueArguments()) {
            argumentTypes.add(context.getType(argument.getArgumentExpression()));

        }
        builder.append(Renderers.RENDER_COLLECTION_OF_TYPES.render(argumentTypes));
        builder.append(")");
        return builder.toString();
    }

    @NotNull
    public KotlinType resolveDelegateExpression(
            @NotNull KtExpression delegateExpression,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull LexicalScope scopeForDelegate,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        TemporaryBindingTrace traceToResolveDelegatedProperty = TemporaryBindingTrace.create(trace, "Trace to resolve delegated property");
        KtExpression calleeExpression = CallUtilKt.getCalleeExpressionIfAny(delegateExpression);
        ConstraintSystemCompleter completer = createConstraintSystemCompleter(
                property, propertyDescriptor, delegateExpression, scopeForDelegate, trace);
        if (calleeExpression != null) {
            traceToResolveDelegatedProperty.record(CONSTRAINT_SYSTEM_COMPLETER, calleeExpression, completer);
        }
        KotlinType delegateType = expressionTypingServices.safeGetType(scopeForDelegate, delegateExpression, NO_EXPECTED_TYPE,
                                                                       dataFlowInfo, traceToResolveDelegatedProperty);
        traceToResolveDelegatedProperty.commit(new TraceEntryFilter() {
            @Override
            public boolean accept(@Nullable WritableSlice<?, ?> slice, @Nullable Diagnostic diagnostic, Object key) {
                return slice != CONSTRAINT_SYSTEM_COMPLETER;
            }
        }, true);
        return delegateType;
    }

    @NotNull
    private ConstraintSystemCompleter createConstraintSystemCompleter(
            @NotNull KtProperty property,
            @NotNull final PropertyDescriptor propertyDescriptor,
            @NotNull final KtExpression delegateExpression,
            @NotNull LexicalScope scopeForDelegate,
            @NotNull final BindingTrace trace
    ) {
        final LexicalScope delegateFunctionsScope = ScopeUtils.makeScopeForDelegateConventionFunctions(scopeForDelegate, propertyDescriptor);
        final KotlinType expectedType = property.getTypeReference() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
        return new ConstraintSystemCompleter() {
            @Override
            public void completeConstraintSystem(
                    @NotNull ConstraintSystem.Builder constraintSystem, @NotNull ResolvedCall<?> resolvedCall
            ) {
                KotlinType returnType = resolvedCall.getCandidateDescriptor().getReturnType();
                if (returnType == null) return;

                TypeSubstitutor typeVariableSubstitutor =
                        constraintSystem.getTypeVariableSubstitutors().get(TypeVariableKt.toHandle(resolvedCall.getCall()));
                assert typeVariableSubstitutor != null : "No substitutor in the system for call: " + resolvedCall.getCall();

                TemporaryBindingTrace traceToResolveConventionMethods =
                        TemporaryBindingTrace.create(trace, "Trace to resolve delegated property convention methods");
                OverloadResolutionResults<FunctionDescriptor>
                        getMethodResults = getDelegatedPropertyConventionMethod(
                                propertyDescriptor, delegateExpression, returnType, traceToResolveConventionMethods, delegateFunctionsScope,
                                true, false
                        );

                if (conventionMethodFound(getMethodResults)) {
                    FunctionDescriptor descriptor = getMethodResults.getResultingDescriptor();
                    KotlinType returnTypeOfGetMethod = descriptor.getReturnType();
                    if (returnTypeOfGetMethod != null && !TypeUtils.noExpectedType(expectedType)) {
                        KotlinType returnTypeInSystem = typeVariableSubstitutor.substitute(returnTypeOfGetMethod, Variance.INVARIANT);
                        if (returnTypeInSystem != null) {
                            constraintSystem.addSubtypeConstraint(returnTypeInSystem, expectedType, FROM_COMPLETER.position());
                        }
                    }
                    addConstraintForThisValue(constraintSystem, typeVariableSubstitutor, descriptor);
                }
                if (!propertyDescriptor.isVar()) return;

                // For the case: 'val v by d' (no declared type).
                // When we add a constraint for 'set' method for delegated expression 'd' we use a type of the declared variable 'v'.
                // But if the type isn't known yet, the constraint shouldn't be added (we try to infer the type of 'v' here as well).
                if (propertyDescriptor.getReturnType() instanceof DeferredType) return;

                OverloadResolutionResults<FunctionDescriptor>
                        setMethodResults = getDelegatedPropertyConventionMethod(
                                propertyDescriptor, delegateExpression, returnType, traceToResolveConventionMethods, delegateFunctionsScope,
                                false, false
                        );

                if (conventionMethodFound(setMethodResults)) {
                    FunctionDescriptor descriptor = setMethodResults.getResultingDescriptor();
                    List<ValueParameterDescriptor> valueParameters = descriptor.getValueParameters();
                    if (valueParameters.size() == 3) {
                        ValueParameterDescriptor valueParameterForThis = valueParameters.get(2);

                        if (!noExpectedType(expectedType)) {
                            constraintSystem.addSubtypeConstraint(
                                    expectedType,
                                    typeVariableSubstitutor.substitute(valueParameterForThis.getType(), Variance.INVARIANT),
                                    FROM_COMPLETER.position()
                            );
                        }
                        addConstraintForThisValue(constraintSystem, typeVariableSubstitutor, descriptor);
                    }
                }
            }

            private boolean conventionMethodFound(@NotNull OverloadResolutionResults<FunctionDescriptor> results) {
                return results.isSuccess() ||
                       (results.isSingleResult() &&
                        results.getResultCode() == OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH);
            }

            private void addConstraintForThisValue(
                    ConstraintSystem.Builder constraintSystem,
                    TypeSubstitutor typeVariableSubstitutor,
                    FunctionDescriptor resultingDescriptor
            ) {
                ReceiverParameterDescriptor extensionReceiver = propertyDescriptor.getExtensionReceiverParameter();
                ReceiverParameterDescriptor dispatchReceiver = propertyDescriptor.getDispatchReceiverParameter();
                KotlinType typeOfThis =
                        extensionReceiver != null ? extensionReceiver.getType() :
                        dispatchReceiver != null ? dispatchReceiver.getType() :
                        builtIns.getNullableNothingType();

                List<ValueParameterDescriptor> valueParameters = resultingDescriptor.getValueParameters();
                if (valueParameters.isEmpty()) return;
                ValueParameterDescriptor valueParameterForThis = valueParameters.get(0);

                constraintSystem.addSubtypeConstraint(
                        typeOfThis,
                        typeVariableSubstitutor.substitute(valueParameterForThis.getType(), Variance.INVARIANT),
                        FROM_COMPLETER.position()
                );
            }
        };
    }
}
