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
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
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
import org.jetbrains.kotlin.types.TypeIntersector;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.SMARTCAST_IMPOSSIBLE;
import static org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST;

public class SmartCastManager {

    public SmartCastManager() {
    }

    @NotNull
    public List<JetType> getSmartCastVariants(
            @NotNull ReceiverValue receiverToCast,
            @NotNull ResolutionContext context
    ) {
        return getSmartCastVariants(receiverToCast, context.trace.getBindingContext(), context.scope.getContainingDeclaration(), context.dataFlowInfo);
    }

    @NotNull
    public List<JetType> getSmartCastVariants(
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
    public List<JetType> getSmartCastVariantsWithLessSpecificExcluded(
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
    public Collection<JetType> getSmartCastVariantsExcludingReceiver(
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
    public Collection<JetType> getSmartCastVariantsExcludingReceiver(
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
            @NotNull JetType receiverParameterType,
            @NotNull ResolutionContext context
    ) {
        List<JetType> smartCastTypes = getSmartCastVariants(receiverArgument, context);
        return getSmartCastSubType(TypeUtils.makeNullable(receiverParameterType), smartCastTypes) != null;
    }

    @Nullable
    private JetType getSmartCastSubType(
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

        JetType intersection = TypeIntersector.intersectTypes(KotlinBuiltIns.getInstance(), JetTypeChecker.DEFAULT, subTypes);
        if (intersection == null || !intersection.getConstructor().isDenotable()) {
            return receiverParameterType;
        }
        return intersection;
    }

    private static void recordCastOrError(
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
            @NotNull JetType expectedType,
            @Nullable JetExpression expression,
            @NotNull ResolutionContext c,
            boolean recordExpressionType
    ) {
        for (JetType possibleType : c.dataFlowInfo.getPossibleTypes(dataFlowValue)) {
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
            JetType nullableExpectedType = TypeUtils.makeNullable(expectedType);

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
