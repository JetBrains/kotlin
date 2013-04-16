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

package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.rendering.Renderers;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.ExpressionPosition;
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

import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.psi.JetPsiFactory.createExpression;
import static org.jetbrains.jet.lang.psi.JetPsiFactory.createSimpleName;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.createFakeExpressionOfType;

public class DelegatedPropertyUtils {

    @Nullable
    public static JetType getDelegatedPropertyGetMethodReturnType(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope
    ) {
        resolveDelegatedPropertyConventionMethod(propertyDescriptor, delegateExpression, delegateType, expressionTypingServices, trace,
                                                 scope, true);
        return getDelegateGetMethodReturnType(trace.getBindingContext(), propertyDescriptor);
    }

    @Nullable
    private static JetType getDelegateGetMethodReturnType(@NotNull BindingContext context, @NotNull PropertyDescriptor descriptor) {
        ResolvedCall<FunctionDescriptor> resolvedCall =
                context.get(DELEGATED_PROPERTY_RESOLVED_CALL, descriptor.getGetter());
        return resolvedCall != null ? resolvedCall.getResultingDescriptor().getReturnType() : null;
    }

    public static void resolveDelegatedPropertyGetMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope
    ) {
        resolveDelegatedPropertyConventionMethod(propertyDescriptor, delegateExpression, delegateType, expressionTypingServices, trace,
                                                 scope, true);
        JetType returnType = getDelegateGetMethodReturnType(trace.getBindingContext(), propertyDescriptor);
        JetType propertyType = propertyDescriptor.getType();
        if (propertyType instanceof DeferredType) {
            assert ((DeferredType) propertyType).isComputed() : "Property type should be computed when resolving delegate convention method";
        }

        if (returnType != null && !JetTypeChecker.INSTANCE.isSubtypeOf(returnType, propertyType)) {
            Call call = trace.getBindingContext().get(DELEGATED_PROPERTY_CALL, propertyDescriptor.getGetter());
            assert call != null : "Call should exists for " + propertyDescriptor.getGetter();
            trace.report(DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH
                                 .on(delegateExpression, renderCall(call, trace.getBindingContext()), propertyDescriptor.getType(), returnType));
        }
    }

    public static void resolveDelegatedPropertySetMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope
    ) {
        resolveDelegatedPropertyConventionMethod(propertyDescriptor, delegateExpression, delegateType, expressionTypingServices, trace,
                                                 scope, false);
    }

    /* Resolve get() or set() methods from delegate */
    private static void resolveDelegatedPropertyConventionMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetType delegateType,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            boolean isGet
    ) {
        PropertyAccessorDescriptor accessor = isGet ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
        assert accessor != null : "Delegated property should have getter/setter " + propertyDescriptor + " " + delegateExpression.getText();

        Call call = trace.getBindingContext().get(DELEGATED_PROPERTY_CALL, accessor);
        if (call != null) return;

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices, trace, scope,
                DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE, ExpressionPosition.FREE);
        Project project = context.expressionTypingServices.getProject();

        boolean hasThis = propertyDescriptor.getReceiverParameter() != null || propertyDescriptor.getExpectedThisObject() != null;

        List<JetExpression> arguments = Lists.newArrayList();
        arguments.add(createExpression(project, hasThis ? "this" : "null"));
        arguments.add(createExpression(project, "\"" + propertyDescriptor.getName().getName() + "\""));
        if (!isGet) {
            JetReferenceExpression fakeArgument = createFakeExpressionOfType(context.expressionTypingServices.getProject(), trace,
                                                                             "fakeArgument" + arguments.size(),
                                                                             propertyDescriptor.getType());
            arguments.add(fakeArgument);
            List<ValueParameterDescriptor> valueParameters = accessor.getValueParameters();
            context.trace.record(REFERENCE_TARGET, fakeArgument, valueParameters.get(0));
        }

        Name functionName = Name.identifier(isGet ? "get" : "set");
        JetReferenceExpression fakeCalleeExpression = createSimpleName(project, functionName.getName());

        ExpressionReceiver receiver = new ExpressionReceiver(delegateExpression, delegateType);
        call = CallMaker.makeCallWithExpressions(fakeCalleeExpression, receiver, null, fakeCalleeExpression, arguments, Call.CallType.DEFAULT);
        context.trace.record(BindingContext.DELEGATED_PROPERTY_CALL, accessor, call);

        OverloadResolutionResults<FunctionDescriptor> functionResults = context.resolveCallWithGivenName(call, fakeCalleeExpression, functionName);

        if (!functionResults.isSuccess()) {
            String expectedFunction = renderCall(call, trace.getBindingContext());
            if (functionResults.isIncomplete()) {
                context.trace.report(DELEGATE_SPECIAL_FUNCTION_MISSING.on(delegateExpression, expectedFunction, delegateType));
            }
            else if (functionResults.isSingleResult() ||
                     functionResults.getResultCode() == OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES) {
                context.trace.report(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE
                                             .on(delegateExpression, expectedFunction, functionResults.getResultingCalls()));
            }
            else if (functionResults.isAmbiguity()) {
                context.trace.report(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY
                                             .on(delegateExpression, expectedFunction, functionResults.getResultingCalls()));
            }
            else {
                context.trace.report(DELEGATE_SPECIAL_FUNCTION_MISSING.on(delegateExpression, expectedFunction, delegateType));
            }
            return;
        }

        context.trace.record(DELEGATED_PROPERTY_RESOLVED_CALL, accessor, functionResults.getResultingCall());
    }

    private static String renderCall(@NotNull Call call, @NotNull BindingContext context) {
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

    private DelegatedPropertyUtils() {
    }
}
