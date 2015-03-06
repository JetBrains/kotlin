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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.rendering.Renderers;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemCompleter;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.types.DeferredType;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import javax.inject.Inject;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getCalleeExpressionIfAny;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.FROM_COMPLETER;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.createFakeExpressionOfType;

public class DelegatedPropertyResolver {
   
    private ExpressionTypingServices expressionTypingServices;
    private CallResolver callResolver;
    private KotlinBuiltIns builtIns;

    public static final Name PROPERTY_DELEGATED_FUNCTION_NAME = Name.identifier("propertyDelegated");

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setCallResolver(@NotNull CallResolver callResolver) {
        this.callResolver = callResolver;
    }

    @Inject
    public void setBuiltIns(@NotNull KotlinBuiltIns builtIns) {
        this.builtIns = builtIns;
    }

    @Nullable
    public JetType getDelegatedPropertyGetMethodReturnType(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope
    ) {
        resolveDelegatedPropertyConventionMethod(propertyDescriptor, delegateExpression, delegateType, trace, scope, true);
        ResolvedCall<FunctionDescriptor> resolvedCall =
                trace.getBindingContext().get(DELEGATED_PROPERTY_RESOLVED_CALL, propertyDescriptor.getGetter());
        return resolvedCall != null ? resolvedCall.getResultingDescriptor().getReturnType() : null;
    }

    public void resolveDelegatedPropertyGetMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope
    ) {
        JetType returnType = getDelegatedPropertyGetMethodReturnType(
                propertyDescriptor, delegateExpression, delegateType, trace, scope);
        JetType propertyType = propertyDescriptor.getType();

        /* Do not check return type of get() method of delegate for properties with DeferredType because property type is taken from it */
        if (!(propertyType instanceof DeferredType) && returnType != null && !JetTypeChecker.DEFAULT.isSubtypeOf(returnType, propertyType)) {
            Call call = trace.getBindingContext().get(DELEGATED_PROPERTY_CALL, propertyDescriptor.getGetter());
            assert call != null : "Call should exists for " + propertyDescriptor.getGetter();
            trace.report(DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH
                                 .on(delegateExpression, renderCall(call, trace.getBindingContext()), propertyDescriptor.getType(), returnType));
        }
    }

    public void resolveDelegatedPropertySetMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope
    ) {
        resolveDelegatedPropertyConventionMethod(propertyDescriptor, delegateExpression, delegateType, trace, scope, false);
    }

    @NotNull
    private JetExpression createExpressionForPropertyMetadata(
            @NotNull JetPsiFactory psiFactory,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        return psiFactory.createExpression(builtIns.getPropertyMetadataImpl().getName().asString() +
                                           "(\"" +
                                           propertyDescriptor.getName().asString() +
                                           "\"): " +
                                           builtIns.getPropertyMetadata().getName().asString());
    }

    public void resolveDelegatedPropertyPDMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope
    ) {
        TemporaryBindingTrace traceToResolvePDMethod = TemporaryBindingTrace.create(trace, "Trace to resolve propertyDelegated method in delegated property");
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices, traceToResolvePDMethod, scope,
                DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE);

        List<JetExpression> arguments = Lists.newArrayList();
        JetPsiFactory psiFactory = JetPsiFactory(delegateExpression);
        arguments.add(createExpressionForPropertyMetadata(psiFactory, propertyDescriptor));
        JetReferenceExpression fakeCalleeExpression = psiFactory.createSimpleName(PROPERTY_DELEGATED_FUNCTION_NAME.asString());
        ExpressionReceiver receiver = new ExpressionReceiver(delegateExpression, delegateType);
        Call call = CallMaker.makeCallWithExpressions(fakeCalleeExpression, receiver, null, fakeCalleeExpression, arguments, Call.CallType.DEFAULT);

        OverloadResolutionResults<FunctionDescriptor> functionResults =
                callResolver.resolveCallWithGivenName(context, call, fakeCalleeExpression, PROPERTY_DELEGATED_FUNCTION_NAME);

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

    /* Resolve get() or set() methods from delegate */
    private void resolveDelegatedPropertyConventionMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            boolean isGet
    ) {
        PropertyAccessorDescriptor accessor = isGet ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
        assert accessor != null : "Delegated property should have getter/setter " + propertyDescriptor + " " + delegateExpression.getText();

        if (trace.getBindingContext().get(DELEGATED_PROPERTY_CALL, accessor) != null) return;

        OverloadResolutionResults<FunctionDescriptor> functionResults = getDelegatedPropertyConventionMethod(
                propertyDescriptor, delegateExpression, delegateType, trace, scope, isGet, true);
        Call call = trace.getBindingContext().get(DELEGATED_PROPERTY_CALL, accessor);
        assert call != null : "'getDelegatedPropertyConventionMethod' didn't record a call";

        if (!functionResults.isSuccess()) {
            String expectedFunction = renderCall(call, trace.getBindingContext());
            if (functionResults.isIncomplete()) {
                trace.report(DELEGATE_SPECIAL_FUNCTION_MISSING.on(delegateExpression, expectedFunction, delegateType));
            }
            else if (functionResults.isSingleResult() ||
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

        trace.record(DELEGATED_PROPERTY_RESOLVED_CALL, accessor, functionResults.getResultingCall());
    }

    /* Resolve get() or set() methods from delegate */
    public OverloadResolutionResults<FunctionDescriptor> getDelegatedPropertyConventionMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            boolean isGet,
            boolean isComplete
    ) {
        PropertyAccessorDescriptor accessor = isGet ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
        assert accessor != null : "Delegated property should have getter/setter " + propertyDescriptor + " " + delegateExpression.getText();

        JetType expectedType = isComplete && isGet && !(propertyDescriptor.getType() instanceof DeferredType)
                               ? propertyDescriptor.getType() : TypeUtils.NO_EXPECTED_TYPE;

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices, trace, scope,
                DataFlowInfo.EMPTY, expectedType);

        boolean hasThis = propertyDescriptor.getExtensionReceiverParameter() != null || propertyDescriptor.getDispatchReceiverParameter() != null;

        List<JetExpression> arguments = Lists.newArrayList();
        JetPsiFactory psiFactory = JetPsiFactory(delegateExpression);
        arguments.add(psiFactory.createExpression(hasThis ? "this" : "null"));

        arguments.add(createExpressionForPropertyMetadata(psiFactory, propertyDescriptor));

        if (!isGet) {
            JetReferenceExpression fakeArgument = (JetReferenceExpression) createFakeExpressionOfType(expressionTypingServices.getProject(), trace,
                                                                             "fakeArgument" + arguments.size(),
                                                                             propertyDescriptor.getType());
            arguments.add(fakeArgument);
            List<ValueParameterDescriptor> valueParameters = accessor.getValueParameters();
            trace.record(REFERENCE_TARGET, fakeArgument, valueParameters.get(0));
        }

        Name functionName = Name.identifier(isGet ? "get" : "set");
        JetReferenceExpression fakeCalleeExpression = psiFactory.createSimpleName(functionName.asString());

        ExpressionReceiver receiver = new ExpressionReceiver(delegateExpression, delegateType);
        Call call = CallMaker.makeCallWithExpressions(fakeCalleeExpression, receiver, null, fakeCalleeExpression, arguments, Call.CallType.DEFAULT);
        trace.record(BindingContext.DELEGATED_PROPERTY_CALL, accessor, call);

        return callResolver.resolveCallWithGivenName(context, call, fakeCalleeExpression, functionName);
    }

    private String renderCall(@NotNull Call call, @NotNull BindingContext context) {
        JetExpression calleeExpression = call.getCalleeExpression();
        assert calleeExpression != null : "CalleeExpression should exists for fake call of convention method";
        StringBuilder builder = new StringBuilder(calleeExpression.getText());
        builder.append("(");
        List<JetType> argumentTypes = Lists.newArrayList();
        for (ValueArgument argument : call.getValueArguments()) {
            argumentTypes.add(context.get(EXPRESSION_TYPE, argument.getArgumentExpression()));

        }
        builder.append(Renderers.RENDER_COLLECTION_OF_TYPES.render(argumentTypes));
        builder.append(")");
        return builder.toString();
    }

    @Nullable
    public JetType resolveDelegateExpression(
            @NotNull JetExpression delegateExpression,
            @NotNull JetProperty jetProperty,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope propertyDeclarationInnerScope,
            @NotNull JetScope accessorScope,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        TemporaryBindingTrace traceToResolveDelegatedProperty = TemporaryBindingTrace.create(trace, "Trace to resolve delegated property");
        JetExpression calleeExpression = getCalleeExpressionIfAny(delegateExpression);
        ConstraintSystemCompleter completer = createConstraintSystemCompleter(
                jetProperty, propertyDescriptor, delegateExpression, accessorScope, trace);
        if (calleeExpression != null) {
            traceToResolveDelegatedProperty.record(CONSTRAINT_SYSTEM_COMPLETER, calleeExpression, completer);
        }
        JetType delegateType = expressionTypingServices.safeGetType(propertyDeclarationInnerScope, delegateExpression, NO_EXPECTED_TYPE,
                                                                    dataFlowInfo, traceToResolveDelegatedProperty);
        traceToResolveDelegatedProperty.commit(new TraceEntryFilter() {
            @Override
            public boolean accept(@Nullable WritableSlice<?, ?> slice, Object key) {
                return slice != CONSTRAINT_SYSTEM_COMPLETER;
            }
        }, true);
        return delegateType;
    }

    @NotNull
    private ConstraintSystemCompleter createConstraintSystemCompleter(
            @NotNull JetProperty property,
            @NotNull final PropertyDescriptor propertyDescriptor,
            @NotNull final JetExpression delegateExpression,
            @NotNull final JetScope accessorScope,
            @NotNull final BindingTrace trace
    ) {
        final JetType expectedType = property.getTypeReference() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
        return new ConstraintSystemCompleter() {
            @Override
            public void completeConstraintSystem(
                    @NotNull ConstraintSystem constraintSystem, @NotNull ResolvedCall<?> resolvedCall
            ) {
                JetType returnType = resolvedCall.getCandidateDescriptor().getReturnType();
                if (returnType == null) return;

                TemporaryBindingTrace traceToResolveConventionMethods =
                        TemporaryBindingTrace.create(trace, "Trace to resolve delegated property convention methods");
                OverloadResolutionResults<FunctionDescriptor>
                        getMethodResults = getDelegatedPropertyConventionMethod(
                                propertyDescriptor, delegateExpression, returnType, traceToResolveConventionMethods, accessorScope,
                                true, false
                        );

                if (conventionMethodFound(getMethodResults)) {
                    FunctionDescriptor descriptor = getMethodResults.getResultingDescriptor();
                    JetType returnTypeOfGetMethod = descriptor.getReturnType();
                    if (returnTypeOfGetMethod != null) {
                        constraintSystem.addSupertypeConstraint(expectedType, returnTypeOfGetMethod, FROM_COMPLETER.position());
                    }
                    addConstraintForThisValue(constraintSystem, descriptor);
                }
                if (!propertyDescriptor.isVar()) return;

                // For the case: 'val v by d' (no declared type).
                // When we add a constraint for 'set' method for delegated expression 'd' we use a type of the declared variable 'v'.
                // But if the type isn't known yet, the constraint shouldn't be added (we try to infer the type of 'v' here as well).
                if (propertyDescriptor.getReturnType() instanceof DeferredType) return;

                OverloadResolutionResults<FunctionDescriptor>
                        setMethodResults = getDelegatedPropertyConventionMethod(
                                propertyDescriptor, delegateExpression, returnType, traceToResolveConventionMethods, accessorScope,
                                false, false
                        );

                if (conventionMethodFound(setMethodResults)) {
                    FunctionDescriptor descriptor = setMethodResults.getResultingDescriptor();
                    List<ValueParameterDescriptor> valueParameters = descriptor.getValueParameters();
                    if (valueParameters.size() == 3) {
                        ValueParameterDescriptor valueParameterForThis = valueParameters.get(2);

                        if (!noExpectedType(expectedType)) {
                            constraintSystem.addSubtypeConstraint(
                                    expectedType, valueParameterForThis.getType(), FROM_COMPLETER.position());
                        }
                        addConstraintForThisValue(constraintSystem, descriptor);
                    }
                }
            }

            private boolean conventionMethodFound(@NotNull OverloadResolutionResults<FunctionDescriptor> results) {
                return results.isSuccess() ||
                       (results.isSingleResult() &&
                        results.getResultCode() == OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH);
            }

            private void addConstraintForThisValue(ConstraintSystem constraintSystem, FunctionDescriptor resultingDescriptor) {
                ReceiverParameterDescriptor extensionReceiver = propertyDescriptor.getExtensionReceiverParameter();
                ReceiverParameterDescriptor dispatchReceiver = propertyDescriptor.getDispatchReceiverParameter();
                JetType typeOfThis =
                        extensionReceiver != null ? extensionReceiver.getType() :
                        dispatchReceiver != null ? dispatchReceiver.getType() :
                        builtIns.getNullableNothingType();

                List<ValueParameterDescriptor> valueParameters = resultingDescriptor.getValueParameters();
                if (valueParameters.isEmpty()) return;
                ValueParameterDescriptor valueParameterForThis = valueParameters.get(0);

                constraintSystem.addSubtypeConstraint(typeOfThis, valueParameterForThis.getType(), FROM_COMPLETER.position());
            }
        };
    }
}
