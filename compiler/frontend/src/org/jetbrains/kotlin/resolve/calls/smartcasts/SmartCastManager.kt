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
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeIntersector;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.SMARTCAST_IMPOSSIBLE;
import static org.jetbrains.kotlin.resolve.BindingContext.IMPLICIT_RECEIVER_SMARTCAST;
import static org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST;

// We do not want to make methods static to keep SmartCastManager as a component
@SuppressWarnings("MethodMayBeStatic")
public class SmartCastManager {

    @NotNull
    public List<KotlinType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull ResolutionContext context
    ) {
        return getSmartCastVariants(receiverToCast, context.trace.getBindingContext(), context.scope.getOwnerDescriptor(), context.dataFlowInfo);
    }

    @NotNull
    public List<KotlinType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        List<KotlinType> variants = Lists.newArrayList();
        variants.add(receiverToCast.getType());
        variants.addAll(getSmartCastVariantsExcludingReceiver(bindingContext, containingDeclarationOrModule, dataFlowInfo, receiverToCast));
        return variants;
    }

    @NotNull
    public List<KotlinType> getSmartCastVariantsWithLessSpecificExcluded(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        final List<KotlinType> variants = CollectionsKt.distinct(
                getSmartCastVariants(receiverToCast, bindingContext, containingDeclarationOrModule, dataFlowInfo));
        return CollectionsKt.filter(variants, new Function1<KotlinType, Boolean>() {
            @Override
            public Boolean invoke(final KotlinType type) {
                return CollectionsKt.none(variants, new Function1<KotlinType, Boolean>() {
                    @Override
                    public Boolean invoke(KotlinType another) {
                        return another != type && KotlinTypeChecker.DEFAULT.isSubtypeOf(another, type);
                    }
                });
            }
        });
    }

    /**
     * @return variants @param receiverToCast may be cast to according to context dataFlowInfo, receiverToCast itself is NOT included
     */
    @NotNull
    public Collection<KotlinType> getSmartCastVariantsExcludingReceiver(
            @NotNull ResolutionContext context,
            @NotNull ReceiverValue receiverToCast
    ) {
        return getSmartCastVariantsExcludingReceiver(context.trace.getBindingContext(),
                                                     context.scope.getOwnerDescriptor(),
                                                     context.dataFlowInfo,
                                                     receiverToCast);
    }

    /**
     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
     */
    @NotNull
    public Collection<KotlinType> getSmartCastVariantsExcludingReceiver(
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ReceiverValue receiverToCast
    ) {
        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(
                receiverToCast, bindingContext, containingDeclarationOrModule
        );

        return dataFlowInfo.getCollectedTypes(dataFlowValue);
    }

    public boolean isSubTypeBySmartCastIgnoringNullability(
            @NotNull ReceiverValue receiverArgument,
            @NotNull KotlinType receiverParameterType,
            @NotNull ResolutionContext context
    ) {
        List<KotlinType> smartCastTypes = getSmartCastVariants(receiverArgument, context);
        return getSmartCastSubType(TypeUtils.makeNullable(receiverParameterType), smartCastTypes) != null;
    }

    @Nullable
    private KotlinType getSmartCastSubType(
            @NotNull KotlinType receiverParameterType,
            @NotNull Collection<KotlinType> smartCastTypes
    ) {
        Set<KotlinType> subTypes = Sets.newHashSet();
        for (KotlinType smartCastType : smartCastTypes) {
            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(smartCastType, receiverParameterType)) {
                subTypes.add(smartCastType);
            }
        }
        if (subTypes.isEmpty()) return null;

        KotlinType intersection = TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, subTypes);
        if (intersection == null || !intersection.getConstructor().isDenotable()) {
            return receiverParameterType;
        }
        return intersection;
    }

    private static void recordCastOrError(
            @NotNull KtExpression expression,
            @NotNull KotlinType type,
            @NotNull BindingTrace trace,
            @NotNull DataFlowValue dataFlowValue,
            boolean recordExpressionType
    ) {
        if (KotlinBuiltIns.isNullableNothing(type)) return;
        if (dataFlowValue.isPredictable()) {
            trace.record(SMARTCAST, expression, type);
            if (recordExpressionType) {
                //TODO
                //Why the expression type is rewritten for receivers and is not rewritten for arguments? Is it necessary?
                trace.recordType(expression, type);
            }
        }
        else {
            trace.report(SMARTCAST_IMPOSSIBLE.on(expression, type, expression.getText(), dataFlowValue.getKind().getDescription()));
        }
    }

    @Nullable
    public static SmartCastResult checkAndRecordPossibleCast(
            @NotNull DataFlowValue dataFlowValue,
            @NotNull KotlinType expectedType,
            @Nullable KtExpression expression,
            @NotNull ResolutionContext c,
            @Nullable KtExpression calleeExpression,
            boolean recordExpressionType
    ) {
        return checkAndRecordPossibleCast(
                dataFlowValue, expectedType, null, expression, c, calleeExpression, recordExpressionType);
    }

    @Nullable
    public static SmartCastResult checkAndRecordPossibleCast(
            @NotNull DataFlowValue dataFlowValue,
            @NotNull KotlinType expectedType,
            @Nullable Function1<KotlinType, Boolean> additionalPredicate,
            @Nullable KtExpression expression,
            @NotNull ResolutionContext c,
            @Nullable KtExpression calleeExpression,
            boolean recordExpressionType
    ) {
        for (KotlinType possibleType : c.dataFlowInfo.getCollectedTypes(dataFlowValue)) {
            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(possibleType, expectedType)
                    && (additionalPredicate == null || additionalPredicate.invoke(possibleType))) {
                if (expression != null) {
                    recordCastOrError(expression, possibleType, c.trace, dataFlowValue, recordExpressionType);
                }
                else if (calleeExpression != null && dataFlowValue.isPredictable()) {
                    c.trace.record(IMPLICIT_RECEIVER_SMARTCAST, calleeExpression, possibleType);
                }
                return new SmartCastResult(possibleType, dataFlowValue.isPredictable());
            }
        }

        if (!c.dataFlowInfo.getCollectedNullability(dataFlowValue).canBeNull() && !expectedType.isMarkedNullable()) {
            // Handling cases like:
            // fun bar(x: Any) {}
            // fun <T : Any?> foo(x: T) {
            //      if (x != null) {
            //          bar(x) // Should be allowed with smart cast
            //      }
            // }
            //
            // It doesn't handled by lower code with getPossibleTypes because smart cast of T after `x != null` is still has same type T.
            // But at the same time we're sure that `x` can't be null and just check for such cases manually

            // E.g. in case x!! when x has type of T where T is type parameter with nullable upper bounds
            // x!! is immanently not null (see DataFlowValueFactory.createDataFlowValue for expression)
            boolean immanentlyNotNull = !dataFlowValue.getImmanentNullability().canBeNull();
            KotlinType nullableExpectedType = TypeUtils.makeNullable(expectedType);

            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(dataFlowValue.getType(), nullableExpectedType)
                    && (additionalPredicate == null || additionalPredicate.invoke(dataFlowValue.getType()))) {
                if (!immanentlyNotNull) {
                    if (expression != null) {
                        recordCastOrError(expression, dataFlowValue.getType(), c.trace, dataFlowValue, recordExpressionType);
                    }
                }

                return new SmartCastResult(dataFlowValue.getType(), immanentlyNotNull || dataFlowValue.isPredictable());
            }
            return checkAndRecordPossibleCast(dataFlowValue, nullableExpectedType, expression, c, calleeExpression, recordExpressionType);
        }

        return null;
    }
}
