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
import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KtType;
import org.jetbrains.kotlin.types.TypeIntersector;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.SMARTCAST_IMPOSSIBLE;
import static org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST;

public class SmartCastManager {

    @NotNull
    public List<KtType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull ResolutionContext context
    ) {
        return getSmartCastVariants(receiverToCast, context.trace.getBindingContext(), context.scope.getOwnerDescriptor(), context.dataFlowInfo);
    }

    @NotNull
    public List<KtType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        List<KtType> variants = Lists.newArrayList();
        variants.add(receiverToCast.getType());
        variants.addAll(getSmartCastVariantsExcludingReceiver(bindingContext, containingDeclarationOrModule, dataFlowInfo, receiverToCast));
        return variants;
    }

    @NotNull
    public List<KtType> getSmartCastVariantsWithLessSpecificExcluded(
            @NotNull ReceiverValue receiverToCast,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        final List<KtType> variants = getSmartCastVariants(receiverToCast, bindingContext,
                                                           containingDeclarationOrModule, dataFlowInfo);
        return CollectionsKt.filter(variants, new Function1<KtType, Boolean>() {
            @Override
            public Boolean invoke(final KtType type) {
                return !CollectionsKt.any(variants, new Function1<KtType, Boolean>() {
                    @Override
                    public Boolean invoke(KtType another) {
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
    public Collection<KtType> getSmartCastVariantsExcludingReceiver(
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
    public Collection<KtType> getSmartCastVariantsExcludingReceiver(
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ReceiverValue receiverToCast
    ) {
        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(
                receiverToCast, bindingContext, containingDeclarationOrModule
        );

        return dataFlowInfo.getPossibleTypes(dataFlowValue);
    }

    public boolean isSubTypeBySmartCastIgnoringNullability(
            @NotNull ReceiverValue receiverArgument,
            @NotNull KtType receiverParameterType,
            @NotNull ResolutionContext context
    ) {
        List<KtType> smartCastTypes = getSmartCastVariants(receiverArgument, context);
        return getSmartCastSubType(TypeUtils.makeNullable(receiverParameterType), smartCastTypes) != null;
    }

    @Nullable
    private KtType getSmartCastSubType(
            @NotNull KtType receiverParameterType,
            @NotNull Collection<KtType> smartCastTypes
    ) {
        Set<KtType> subTypes = Sets.newHashSet();
        for (KtType smartCastType : smartCastTypes) {
            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(smartCastType, receiverParameterType)) {
                subTypes.add(smartCastType);
            }
        }
        if (subTypes.isEmpty()) return null;

        KtType intersection = TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, subTypes);
        if (intersection == null || !intersection.getConstructor().isDenotable()) {
            return receiverParameterType;
        }
        return intersection;
    }

    private static void recordCastOrError(
            @NotNull KtExpression expression,
            @NotNull KtType type,
            @NotNull BindingTrace trace,
            boolean canBeCast,
            boolean recordExpressionType
    ) {
        if (canBeCast) {
            trace.record(SMARTCAST, expression, type);
            if (recordExpressionType) {
                //TODO
                //Why the expression type is rewritten for receivers and is not rewritten for arguments? Is it necessary?
                trace.recordType(expression, type);
            }
        }
        else {
            trace.report(SMARTCAST_IMPOSSIBLE.on(expression, type, expression.getText()));
        }
    }

    @Nullable
    public SmartCastResult checkAndRecordPossibleCast(
            @NotNull DataFlowValue dataFlowValue,
            @NotNull KtType expectedType,
            @Nullable KtExpression expression,
            @NotNull ResolutionContext c,
            boolean recordExpressionType
    ) {
        for (KtType possibleType : c.dataFlowInfo.getPossibleTypes(dataFlowValue)) {
            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(possibleType, expectedType)) {
                if (expression != null) {
                    recordCastOrError(expression, possibleType, c.trace, dataFlowValue.isPredictable(), recordExpressionType);
                }
                return new SmartCastResult(possibleType, dataFlowValue.isPredictable());
            }
        }

        if (!c.dataFlowInfo.getNullability(dataFlowValue).canBeNull() && !expectedType.isMarkedNullable()) {
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
            KtType nullableExpectedType = TypeUtils.makeNullable(expectedType);

            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(dataFlowValue.getType(), nullableExpectedType)) {
                if (!immanentlyNotNull) {
                    if (expression != null) {
                        recordCastOrError(expression, dataFlowValue.getType(), c.trace, dataFlowValue.isPredictable(),
                                          recordExpressionType);
                    }
                }

                return new SmartCastResult(dataFlowValue.getType(), immanentlyNotNull || dataFlowValue.isPredictable());
            }
            return checkAndRecordPossibleCast(dataFlowValue, nullableExpectedType, expression, c, recordExpressionType);
        }

        return null;
    }
}
