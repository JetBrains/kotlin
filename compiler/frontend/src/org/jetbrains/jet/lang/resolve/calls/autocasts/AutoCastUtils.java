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

package org.jetbrains.jet.lang.resolve.calls.autocasts;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.AUTOCAST_IMPOSSIBLE;
import static org.jetbrains.jet.lang.resolve.BindingContext.AUTOCAST;
import static org.jetbrains.jet.lang.resolve.BindingContext.EXPRESSION_TYPE;

public class AutoCastUtils {

    private AutoCastUtils() {}

    public static List<ReceiverValue> getAutoCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull ResolutionContext context
    ) {
        return getAutoCastVariants(receiverToCast, context.trace.getBindingContext(), context.dataFlowInfo);
    }

    public static List<ReceiverValue> getAutoCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        List<ReceiverValue> variants = Lists.newArrayList();
        variants.add(receiverToCast);
        variants.addAll(getAutoCastVariantsExcludingReceiver(bindingContext, dataFlowInfo, receiverToCast));
        return variants;
    }

    /**
     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
     */
    @NotNull
    public static List<ReceiverValue> getAutoCastVariantsExcludingReceiver(
            @NotNull BindingContext bindingContext,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ReceiverValue receiverToCast
    ) {
        if (receiverToCast instanceof ThisReceiver) {
            ThisReceiver receiver = (ThisReceiver) receiverToCast;
            assert receiver.exists();
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver);
            return collectAutoCastReceiverValues(dataFlowInfo, receiver, dataFlowValue);
        }
        else if (receiverToCast instanceof ExpressionReceiver) {
            ExpressionReceiver receiver = (ExpressionReceiver) receiverToCast;
            DataFlowValue dataFlowValue =
                    DataFlowValueFactory.createDataFlowValue(receiver.getExpression(), receiver.getType(), bindingContext);
            return collectAutoCastReceiverValues(dataFlowInfo, receiver, dataFlowValue);
        }
        else if (receiverToCast instanceof AutoCastReceiver) {
            return getAutoCastVariantsExcludingReceiver(bindingContext, dataFlowInfo, ((AutoCastReceiver) receiverToCast).getOriginal());
        }
        return Collections.emptyList();
    }

    @NotNull
    private static List<ReceiverValue> collectAutoCastReceiverValues(
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ReceiverValue receiver,
            @NotNull DataFlowValue dataFlowValue
    ) {
        Set<JetType> possibleTypes = dataFlowInfo.getPossibleTypes(dataFlowValue);
        List<ReceiverValue> result = new ArrayList<ReceiverValue>(possibleTypes.size());
        for (JetType possibleType : possibleTypes) {
            result.add(new AutoCastReceiver(receiver, possibleType, dataFlowValue.isStableIdentifier()));
        }
        return result;
    }

    public static void recordAutoCastIfNecessary(ReceiverValue receiver, @NotNull BindingTrace trace) {
        if (receiver instanceof AutoCastReceiver) {
            AutoCastReceiver autoCastReceiver = (AutoCastReceiver) receiver;
            ReceiverValue original = autoCastReceiver.getOriginal();
            if (original instanceof ExpressionReceiver) {
                JetExpression expression = ((ExpressionReceiver) original).getExpression();
                recordCastOrError(expression, autoCastReceiver.getType(), trace, autoCastReceiver.canCast(), true);
            }
            else {
                assert autoCastReceiver.canCast() : "A non-expression receiver must always be autocastabe: " + original;
            }
        }
    }

    public static void recordAutoCastToNotNullableType(@NotNull ReceiverValue receiver, @NotNull BindingTrace trace) {
        if (!(receiver instanceof ExpressionReceiver)) return;

        JetType receiverType = receiver.getType();
        if (!receiverType.isNullable()) return;
        JetType notNullableType = TypeUtils.makeNotNullable(receiverType);

        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver, trace.getBindingContext());
        JetExpression expression = ((ExpressionReceiver) receiver).getExpression();
        recordCastOrError(expression, notNullableType, trace, dataFlowValue.isStableIdentifier(), true);
    }

    public static void recordCastOrError(
            @NotNull JetExpression expression,
            @NotNull JetType type,
            @NotNull BindingTrace trace,
            boolean canBeCasted,
            boolean recordExpressionType
    ) {
        if (canBeCasted) {
            trace.record(AUTOCAST, expression, type);
            if (recordExpressionType) {
                //TODO
                //Why the expression type is rewritten for receivers and is not rewritten for arguments? Is it necessary?
                trace.record(EXPRESSION_TYPE, expression, type);
            }
        }
        else {
            trace.report(AUTOCAST_IMPOSSIBLE.on(expression, type, expression.getText()));
        }
    }

    public static boolean isNotNull(
            @NotNull ReceiverValue receiver,
            @NotNull BindingContext bindingContext,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        if (!receiver.getType().isNullable()) return true;

        List<ReceiverValue> autoCastVariants = getAutoCastVariants(receiver, bindingContext, dataFlowInfo);
        for (ReceiverValue autoCastVariant : autoCastVariants) {
            if (!autoCastVariant.getType().isNullable()) return true;
        }
        return false;
    }
}
