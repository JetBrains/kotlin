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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.rendering.Renderers;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemCompleter;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import javax.inject.Inject;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage.getCalleeExpressionIfAny;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.jet.lang.types.TypeUtils.noExpectedType;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.createFakeExpressionOfType;

public class DelegatedPropertyResolver {
   
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    @NotNull
    private CallResolver callResolver;

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setCallResolver(@NotNull CallResolver callResolver) {
        this.callResolver = callResolver;
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
                propertyDescriptor, delegateExpression, delegateType, trace, scope, isGet);
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
            boolean isGet
    ) {
        PropertyAccessorDescriptor accessor = isGet ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
        assert accessor != null : "Delegated property should have getter/setter " + propertyDescriptor + " " + delegateExpression.getText();

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices, trace, scope,
                DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE);

        boolean hasThis = propertyDescriptor.getReceiverParameter() != null || propertyDescriptor.getExpectedThisObject() != null;

        List<JetExpression> arguments = Lists.newArrayList();
        JetPsiFactory psiFactory = JetPsiFactory(delegateExpression);
        arguments.add(psiFactory.createExpression(hasThis ? "this" : "null"));

        arguments.add(psiFactory.createExpression(KotlinBuiltIns.getInstance().getPropertyMetadataImpl().getName().asString() +
                                               "(\"" +
                                               propertyDescriptor.getName().asString() +
                                               "\")"));

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
        final JetType expectedType = property.getTypeRef() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
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
                        propertyDescriptor, delegateExpression, returnType, traceToResolveConventionMethods, accessorScope, true);

                if (conventionMethodFound(getMethodResults)) {
                    FunctionDescriptor descriptor = getMethodResults.getResultingDescriptor();
                    JetType returnTypeOfGetMethod = descriptor.getReturnType();
                    if (returnTypeOfGetMethod != null) {
                        constraintSystem.addSupertypeConstraint(expectedType, returnTypeOfGetMethod, ConstraintPosition.FROM_COMPLETER);
                    }
                    addConstraintForThisValue(constraintSystem, descriptor);
                }
                if (!propertyDescriptor.isVar()) return;

                // For the case: 'val v by d' (no declared type).
                // When we add a constraint for 'set' method for delegated expression 'd' we use a type of the declared variable 'v'.
                // But if the type isn't known yet, the constraint shouldn't be added (we try to infer the type of 'v' here as well).
                if (propertyDescriptor.getReturnType() instanceof DeferredType) return;

                OverloadResolutionResults<FunctionDescriptor> setMethodResults =
                        getDelegatedPropertyConventionMethod(
                                propertyDescriptor, delegateExpression, returnType, traceToResolveConventionMethods, accessorScope, false);

                if (conventionMethodFound(setMethodResults)) {
                    FunctionDescriptor descriptor = setMethodResults.getResultingDescriptor();
                    List<ValueParameterDescriptor> valueParameters = descriptor.getValueParameters();
                    if (valueParameters.size() == 3) {
                        ValueParameterDescriptor valueParameterForThis = valueParameters.get(2);

                        if (!noExpectedType(expectedType)) {
                            constraintSystem.addSubtypeConstraint(
                                    expectedType, valueParameterForThis.getType(), ConstraintPosition.FROM_COMPLETER);
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
                ReceiverParameterDescriptor receiverParameter = propertyDescriptor.getReceiverParameter();
                ReceiverParameterDescriptor thisObject = propertyDescriptor.getExpectedThisObject();
                JetType typeOfThis =
                        receiverParameter != null ? receiverParameter.getType() :
                        thisObject != null ? thisObject.getType() :
                        KotlinBuiltIns.getInstance().getNullableNothingType();

                List<ValueParameterDescriptor> valueParameters = resultingDescriptor.getValueParameters();
                if (valueParameters.isEmpty()) return;
                ValueParameterDescriptor valueParameterForThis = valueParameters.get(0);

                constraintSystem.addSubtypeConstraint(typeOfThis, valueParameterForThis.getType(), ConstraintPosition.FROM_COMPLETER);
            }
        };
    }
}
