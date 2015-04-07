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

package org.jetbrains.kotlin.resolve.calls.smartcasts;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.SMARTCAST_IMPOSSIBLE;
import static org.jetbrains.kotlin.resolve.BindingContext.EXPRESSION_TYPE;
import static org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST;

public class SmartCastUtils {

    private SmartCastUtils() {}

    @NotNull
    public static List<JetType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull ResolutionContext context
    ) {
        return getSmartCastVariants(receiverToCast, context.trace.getBindingContext(), context.scope.getContainingDeclaration(), context.dataFlowInfo);
    }

    @NotNull
    public static List<JetType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        List<JetType> variants = Lists.newArrayList();
        variants.add(receiverToCast.getType());
        variants.addAll(getSmartCastVariantsExcludingReceiver(bindingContext, containingDeclarationOrModule, dataFlowInfo, receiverToCast));
        return variants;
    }

    @NotNull
    public static List<JetType> getSmartCastVariantsWithLessSpecificExcluded(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        final List<JetType> variants = getSmartCastVariants(receiverToCast, bindingContext,
                                                            containingDeclarationOrModule, dataFlowInfo);
        return KotlinPackage.filter(variants, new Function1<JetType, Boolean>() {
            @Override
            public Boolean invoke(final JetType type) {
                return !KotlinPackage.any(variants, new Function1<JetType, Boolean>() {
                    @Override
                    public Boolean invoke(JetType another) {
                        return another != type && JetTypeChecker.DEFAULT.isSubtypeOf(another, type);
                    }
                });
            }
        });
    }

    /**
     * @return variants @param receiverToCast may be cast to according to context dataFlowInfo, receiverToCast itself is NOT included
     */
    @NotNull
    public static Collection<JetType> getSmartCastVariantsExcludingReceiver(
            @NotNull ResolutionContext context,
            @NotNull ReceiverValue receiverToCast
    ) {
        return getSmartCastVariantsExcludingReceiver(context.trace.getBindingContext(),
                                                     context.scope.getContainingDeclaration(),
                                                     context.dataFlowInfo,
                                                     receiverToCast);
    }

    /**
     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
     */
    @NotNull
    public static Collection<JetType> getSmartCastVariantsExcludingReceiver(
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
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
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(
                    receiverToCast, bindingContext, containingDeclarationOrModule);
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

        Collection<JetType> smartCastTypesExcludingReceiver = getSmartCastVariantsExcludingReceiver(context, receiver);
        JetType smartCastSubType = getSmartCastSubType(receiverParameterType, smartCastTypesExcludingReceiver);
        if (smartCastSubType == null) return false;

        JetExpression expression = ((ExpressionReceiver) receiver).getExpression();
        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver, context);

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

    public static boolean canBeSmartCast(
            @NotNull ReceiverParameterDescriptor receiverParameter,
            @NotNull ReceiverValue receiver,
            @NotNull ResolutionContext context) {
        if (!receiver.getType().isMarkedNullable()) return true;

        List<JetType> smartCastVariants = getSmartCastVariants(receiver, context);
        for (JetType smartCastVariant : smartCastVariants) {
            if (JetTypeChecker.DEFAULT.isSubtypeOf(smartCastVariant, receiverParameter.getType())) return true;
        }
        return false;
    }
}
