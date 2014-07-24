/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory1;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import javax.inject.Inject;
import java.util.Collections;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.isBoolean;

public class ForLoopConventionsChecker {

    private Project project;
    private ExpressionTypingServices expressionTypingServices;
    private ExpressionTypingUtils expressionTypingUtils;

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @Inject
    public void setExpressionTypingUtils(@NotNull ExpressionTypingUtils expressionTypingUtils) {
        this.expressionTypingUtils = expressionTypingUtils;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    public boolean isVariableIterable(@NotNull VariableDescriptor variableDescriptor, @NotNull JetScope scope) {
        JetExpression expression = JetPsiFactory(project).createExpression("fake");
        ExpressionReceiver expressionReceiver = new ExpressionReceiver(expression, variableDescriptor.getType());
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices,
                new BindingTraceContext(),
                scope,
                DataFlowInfo.EMPTY,
                TypeUtils.NO_EXPECTED_TYPE
        );
        return checkIterableConvention(expressionReceiver, context) != null;
    }

    @Nullable
    /*package*/ JetType checkIterableConvention(@NotNull ExpressionReceiver loopRange, ExpressionTypingContext context) {
        JetExpression loopRangeExpression = loopRange.getExpression();

        // Make a fake call loopRange.iterator(), and try to resolve it
        Name iterator = Name.identifier("iterator");
        Pair<Call, OverloadResolutionResults<FunctionDescriptor>> calls =
                expressionTypingUtils.makeAndResolveFakeCall(loopRange, context, Collections.<JetExpression>emptyList(), iterator);
        Call iteratorCall = calls.getFirst();
        OverloadResolutionResults<FunctionDescriptor> iteratorResolutionResults = calls.getSecond();

        if (iteratorResolutionResults.isSuccess()) {
            ResolvedCall<FunctionDescriptor> iteratorResolvedCall = iteratorResolutionResults.getResultingCall();
            context.trace.record(LOOP_RANGE_ITERATOR_RESOLVED_CALL, loopRangeExpression, iteratorResolvedCall);

            FunctionDescriptor iteratorFunction = iteratorResolvedCall.getResultingDescriptor();
            JetType iteratorType = iteratorFunction.getReturnType();
            JetType hasNextType = checkConventionForIterator(context, loopRangeExpression, iteratorType, "hasNext",
                                                             HAS_NEXT_FUNCTION_AMBIGUITY, HAS_NEXT_MISSING, HAS_NEXT_FUNCTION_NONE_APPLICABLE,
                                                             LOOP_RANGE_HAS_NEXT_RESOLVED_CALL);
            if (hasNextType != null && !isBoolean(hasNextType)) {
                context.trace.report(HAS_NEXT_FUNCTION_TYPE_MISMATCH.on(loopRangeExpression, hasNextType));
            }
            return checkConventionForIterator(context, loopRangeExpression, iteratorType, "next",
                                              NEXT_AMBIGUITY, NEXT_MISSING, NEXT_NONE_APPLICABLE,
                                              LOOP_RANGE_NEXT_RESOLVED_CALL);
        }
        else {
            if (iteratorResolutionResults.isAmbiguity()) {
//                    StringBuffer stringBuffer = new StringBuffer("Method 'iterator()' is ambiguous for this expression: ");
//                    for (FunctionDescriptor functionDescriptor : iteratorResolutionResults.getResultingCalls()) {
//                        stringBuffer.append(DescriptorRenderer.FQ_NAMES_IN_TYPES.render(functionDescriptor)).append(" ");
//                    }
//                    errorMessage = stringBuffer.toString();
                context.trace.report(ITERATOR_AMBIGUITY.on(loopRangeExpression, iteratorResolutionResults.getResultingCalls()));
            }
            else {
                context.trace.report(ITERATOR_MISSING.on(loopRangeExpression));
            }
        }
        return null;
    }

    @Nullable
    private JetType checkConventionForIterator(
            @NotNull ExpressionTypingContext context,
            @NotNull JetExpression loopRangeExpression,
            @NotNull JetType iteratorType,
            @NotNull String name,
            @NotNull DiagnosticFactory1<JetExpression, JetType> ambiguity,
            @NotNull DiagnosticFactory1<JetExpression, JetType> missing,
            @NotNull DiagnosticFactory1<JetExpression, JetType> noneApplicable,
            @NotNull WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> resolvedCallKey
    ) {
        OverloadResolutionResults<FunctionDescriptor> nextResolutionResults = expressionTypingUtils.resolveFakeCall(
                context, new TransientReceiver(iteratorType), Name.identifier(name));
        if (nextResolutionResults.isAmbiguity()) {
            context.trace.report(ambiguity.on(loopRangeExpression, iteratorType));
        }
        else if (nextResolutionResults.isNothing()) {
            context.trace.report(missing.on(loopRangeExpression, iteratorType));
        }
        else if (!nextResolutionResults.isSuccess()) {
            context.trace.report(noneApplicable.on(loopRangeExpression, iteratorType));
        }
        else {
            assert nextResolutionResults.isSuccess();
            ResolvedCall<FunctionDescriptor> resolvedCall = nextResolutionResults.getResultingCall();
            context.trace.record(resolvedCallKey, loopRangeExpression, resolvedCall);
            return resolvedCall.getResultingDescriptor().getReturnType();
        }
        return null;
    }
}
