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
import org.jetbrains.jet.lang.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.AUTOCAST_IMPOSSIBLE;
import static org.jetbrains.jet.lang.resolve.BindingContext.AUTOCAST;
import static org.jetbrains.jet.lang.resolve.BindingContext.EXPRESSION_TYPE;

public class AutoCastUtils {

    private AutoCastUtils() {}

    public static List<JetType> getAutoCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull ResolutionContext context
    ) {
        return getAutoCastVariants(receiverToCast, context.trace.getBindingContext(), context.dataFlowInfo);
    }

    public static List<JetType> getAutoCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        List<JetType> variants = Lists.newArrayList();
        variants.add(receiverToCast.getType());
        variants.addAll(getAutoCastVariantsExcludingReceiver(bindingContext, dataFlowInfo, receiverToCast));
        return variants;
    }

    /**
     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
     */
    @NotNull
    public static List<JetType> getAutoCastVariantsExcludingReceiver(
            @NotNull BindingContext bindingContext,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ReceiverValue receiverToCast
    ) {
        if (receiverToCast instanceof ThisReceiver) {
            ThisReceiver receiver = (ThisReceiver) receiverToCast;
            assert receiver.exists();
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver);
            return collectAutoCastReceiverValues(dataFlowInfo, dataFlowValue);
        }
        else if (receiverToCast instanceof ExpressionReceiver) {
            ExpressionReceiver receiver = (ExpressionReceiver) receiverToCast;
            DataFlowValue dataFlowValue =
                    DataFlowValueFactory.createDataFlowValue(receiver.getExpression(), receiver.getType(), bindingContext);
            return collectAutoCastReceiverValues(dataFlowInfo, dataFlowValue);
        }
        return Collections.emptyList();
    }

    @NotNull
    private static List<JetType> collectAutoCastReceiverValues(
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull DataFlowValue dataFlowValue
    ) {
        return Lists.newArrayList(dataFlowInfo.getPossibleTypes(dataFlowValue));
    }

    public static boolean isSubTypeByAutoCast(
            @NotNull ReceiverValue receiverArgument,
            @NotNull JetType receiverParameterType,
            @NotNull ResolutionContext context
    ) {
        List<JetType> autoCastTypes = getAutoCastVariants(receiverArgument, context);
        return isSubTypeByAutoCast(receiverParameterType, autoCastTypes);
    }

    private static boolean isSubTypeByAutoCast(
            @NotNull JetType receiverParameterType,
            @NotNull List<JetType> autoCastTypes
    ) {
        for (JetType autoCastType : autoCastTypes) {
            JetType effectiveAutoCastType = TypeUtils.makeNotNullable(autoCastType);
            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(effectiveAutoCastType, receiverParameterType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean recordAutoCastIfNecessary(
            ReceiverValue receiver,
            @NotNull JetType receiverType,
            @NotNull ResolutionContext context
    ) {
        if (!(receiver instanceof ExpressionReceiver)) return false;

        if (ArgumentTypeResolver.isSubtypeOfForArgumentType(receiver.getType(), receiverType)) {
            return false;
        }

        List<JetType> autoCastTypesExcludingReceiver = getAutoCastVariantsExcludingReceiver(
                context.trace.getBindingContext(), context.dataFlowInfo, receiver);
        boolean autoCast = isSubTypeByAutoCast(receiverType, autoCastTypesExcludingReceiver);
        if (autoCast) {
            JetExpression expression = ((ExpressionReceiver) receiver).getExpression();
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver, context.trace.getBindingContext());

            recordCastOrError(expression, receiverType, context.trace, dataFlowValue.isStableIdentifier(), true);
        }
        return autoCast;
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

        List<JetType> autoCastVariants = getAutoCastVariants(receiver, bindingContext, dataFlowInfo);
        for (JetType autoCastVariant : autoCastVariants) {
            if (!autoCastVariant.isNullable()) return true;
        }
        return false;
    }
}
