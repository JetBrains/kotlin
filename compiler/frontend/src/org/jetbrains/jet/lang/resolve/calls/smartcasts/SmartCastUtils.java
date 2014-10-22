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

package org.jetbrains.jet.lang.resolve.calls.smartcasts;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.SMARTCAST_IMPOSSIBLE;
import static org.jetbrains.jet.lang.resolve.BindingContext.EXPRESSION_TYPE;
import static org.jetbrains.jet.lang.resolve.BindingContext.SMARTCAST;

public class SmartCastUtils {

    private SmartCastUtils() {}

    @NotNull
    public static List<JetType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull ResolutionContext context
    ) {
        return getSmartCastVariants(receiverToCast, context.trace.getBindingContext(), context.dataFlowInfo);
    }

    @NotNull
    public static List<JetType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        List<JetType> variants = Lists.newArrayList();
        variants.add(receiverToCast.getType());
        variants.addAll(getSmartCastVariantsExcludingReceiver(bindingContext, dataFlowInfo, receiverToCast));
        return variants;
    }

    /**
     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
     */
    @NotNull
    public static Collection<JetType> getSmartCastVariantsExcludingReceiver(
            @NotNull BindingContext bindingContext,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ReceiverValue receiverToCast
    ) {
        if (receiverToCast instanceof ThisReceiver) {
            ThisReceiver receiver = (ThisReceiver) receiverToCast;
            assert receiver.exists();
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver);
            return dataFlowInfo.getPossibleTypes(dataFlowValue);
        }
        else if (receiverToCast instanceof ExpressionReceiver) {
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverToCast, bindingContext);
            return dataFlowInfo.getPossibleTypes(dataFlowValue);
        }
        return Collections.emptyList();
    }

    public static boolean isSubTypeBySmartCastIgnoringNullability(
            @NotNull ReceiverValue receiverArgument,
            @NotNull JetType receiverParameterType,
            @NotNull ResolutionContext context
    ) {
        List<JetType> smartCastTypes = getSmartCastVariants(receiverArgument, context);
        return getSmartCastSubType(TypeUtils.makeNullable(receiverParameterType), smartCastTypes) != null;
    }

    @Nullable
    private static JetType getSmartCastSubType(
            @NotNull JetType receiverParameterType,
            @NotNull Collection<JetType> smartCastTypes
    ) {
        Set<JetType> subTypes = Sets.newHashSet();
        for (JetType smartCastType : smartCastTypes) {
            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(smartCastType, receiverParameterType)) {
                subTypes.add(smartCastType);
            }
        }
        if (subTypes.isEmpty()) return null;

        JetType intersection = TypeUtils.intersect(JetTypeChecker.DEFAULT, subTypes);
        if (intersection == null || !intersection.getConstructor().isDenotable()) {
            return receiverParameterType;
        }
        return intersection;
    }

    public static boolean recordSmartCastIfNecessary(
            @NotNull ReceiverValue receiver,
            @NotNull JetType receiverParameterType,
            @NotNull ResolutionContext context,
            boolean safeAccess
    ) {
        if (!(receiver instanceof ExpressionReceiver)) return false;

        receiverParameterType = safeAccess ? TypeUtils.makeNullable(receiverParameterType) : receiverParameterType;
        if (ArgumentTypeResolver.isSubtypeOfForArgumentType(receiver.getType(), receiverParameterType)) {
            return false;
        }

        Collection<JetType> smartCastTypesExcludingReceiver = getSmartCastVariantsExcludingReceiver(
                context.trace.getBindingContext(), context.dataFlowInfo, receiver);
        JetType smartCastSubType = getSmartCastSubType(receiverParameterType, smartCastTypesExcludingReceiver);
        if (smartCastSubType == null) return false;

        JetExpression expression = ((ExpressionReceiver) receiver).getExpression();
        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver, context.trace.getBindingContext());

        recordCastOrError(expression, smartCastSubType, context.trace, dataFlowValue.isStableIdentifier(), true);
        return true;
    }

    public static void recordCastOrError(
            @NotNull JetExpression expression,
            @NotNull JetType type,
            @NotNull BindingTrace trace,
            boolean canBeCast,
            boolean recordExpressionType
    ) {
        if (canBeCast) {
            trace.record(SMARTCAST, expression, type);
            if (recordExpressionType) {
                //TODO
                //Why the expression type is rewritten for receivers and is not rewritten for arguments? Is it necessary?
                trace.record(EXPRESSION_TYPE, expression, type);
            }
        }
        else {
            trace.report(SMARTCAST_IMPOSSIBLE.on(expression, type, expression.getText()));
        }
    }

    public static boolean isNotNull(
            @NotNull ReceiverValue receiver,
            @NotNull BindingContext bindingContext,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        if (!receiver.getType().isNullable()) return true;

        List<JetType> smartCastVariants = getSmartCastVariants(receiver, bindingContext, dataFlowInfo);
        for (JetType smartCastVariant : smartCastVariants) {
            if (!smartCastVariant.isNullable()) return true;
        }
        return false;
    }
}
