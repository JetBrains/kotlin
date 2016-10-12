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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.RANGES_PACKAGE_FQ_NAME;
import static org.jetbrains.kotlin.codegen.AsmUtil.isPrimitiveNumberClassDescriptor;

public class RangeCodegenUtil {
    private static final ImmutableMap<FqName, PrimitiveType> RANGE_TO_ELEMENT_TYPE;
    private static final ImmutableMap<FqName, PrimitiveType> PROGRESSION_TO_ELEMENT_TYPE;

    @NotNull
    public static List<PrimitiveType> supportedRangeTypes() {
        return Arrays.asList(PrimitiveType.CHAR, PrimitiveType.INT, PrimitiveType.LONG);
    }

    static {
        ImmutableMap.Builder<FqName, PrimitiveType> rangeBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<FqName, PrimitiveType> progressionBuilder = ImmutableMap.builder();
        for (PrimitiveType primitiveType : supportedRangeTypes()) {
            FqName rangeClassFqName = RANGES_PACKAGE_FQ_NAME.child(Name.identifier(primitiveType.getTypeName() + "Range"));
            FqName progressionClassFqName = RANGES_PACKAGE_FQ_NAME.child(Name.identifier(primitiveType.getTypeName() + "Progression"));
            rangeBuilder.put(rangeClassFqName, primitiveType);
            progressionBuilder.put(progressionClassFqName, primitiveType);
        }
        RANGE_TO_ELEMENT_TYPE = rangeBuilder.build();
        PROGRESSION_TO_ELEMENT_TYPE = progressionBuilder.build();
    }

    private RangeCodegenUtil() {}

    public static boolean isRange(KotlinType rangeType) {
        return !rangeType.isMarkedNullable() && getPrimitiveRangeElementType(rangeType) != null;
    }

    public static boolean isProgression(KotlinType rangeType) {
        return !rangeType.isMarkedNullable() && getPrimitiveProgressionElementType(rangeType) != null;
    }

    @Nullable
    public static ResolvedCall<? extends CallableDescriptor> getLoopRangeResolvedCall(@NotNull KtForExpression forExpression, @NotNull BindingContext bindingContext) {
        KtExpression loopRange = KtPsiUtil.deparenthesize(forExpression.getLoopRange());

        if (loopRange instanceof KtQualifiedExpression) {
            KtQualifiedExpression qualifiedExpression = (KtQualifiedExpression) loopRange;
            KtExpression selector = qualifiedExpression.getSelectorExpression();
            if (selector instanceof KtCallExpression || selector instanceof KtSimpleNameExpression) {
                return CallUtilKt.getResolvedCall(selector, bindingContext);
            }
        }
        else if (loopRange instanceof KtSimpleNameExpression || loopRange instanceof KtCallExpression) {
            return CallUtilKt.getResolvedCall(loopRange, bindingContext);
        }
        else if (loopRange instanceof KtBinaryExpression) {
            return CallUtilKt.getResolvedCall(((KtBinaryExpression) loopRange).getOperationReference(), bindingContext);
        }

        return null;
    }

    @Nullable
    private static PrimitiveType getPrimitiveRangeElementType(KotlinType rangeType) {
        return getPrimitiveRangeOrProgressionElementType(rangeType, RANGE_TO_ELEMENT_TYPE);
    }

    @Nullable
    private static PrimitiveType getPrimitiveProgressionElementType(KotlinType rangeType) {
        return getPrimitiveRangeOrProgressionElementType(rangeType, PROGRESSION_TO_ELEMENT_TYPE);
    }

    @Nullable
    private static PrimitiveType getPrimitiveRangeOrProgressionElementType(
            @NotNull KotlinType rangeOrProgression,
            @NotNull ImmutableMap<FqName, PrimitiveType> map
    ) {
        ClassifierDescriptor declarationDescriptor = rangeOrProgression.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor == null) return null;
        FqNameUnsafe fqName = DescriptorUtils.getFqName(declarationDescriptor);
        if (!fqName.isSafe()) return null;
        return map.get(fqName.toSafe());
    }

    @Nullable
    public static PrimitiveType getPrimitiveRangeOrProgressionElementType(@NotNull FqName rangeOrProgressionName) {
        PrimitiveType result = RANGE_TO_ELEMENT_TYPE.get(rangeOrProgressionName);
        return result != null ? result : PROGRESSION_TO_ELEMENT_TYPE.get(rangeOrProgressionName);
    }

    public static boolean isRangeOrProgression(@NotNull FqName className) {
        return getPrimitiveRangeOrProgressionElementType(className) != null;
    }

    /*
     * Checks whether rangeTo expression is optimizable for loop
     */
    public static boolean isOptimizableRangeTo(CallableDescriptor rangeTo) {
        if ("rangeTo".equals(rangeTo.getName().asString())) {
            if (isPrimitiveNumberClassDescriptor(rangeTo.getContainingDeclaration())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOptimizableDownTo(@NotNull CallableDescriptor descriptor) {
        if (!isTopLevelInPackage(descriptor, "downTo", "kotlin.ranges")) return false;

        ReceiverParameterDescriptor extensionReceiver = descriptor.getExtensionReceiverParameter();
        if (extensionReceiver == null) return false;
        ClassifierDescriptor extensionReceiverClassifier = extensionReceiver.getType().getConstructor().getDeclarationDescriptor();
        if (!isPrimitiveNumberClassDescriptor(extensionReceiverClassifier)) return false;

        return true;
    }

    public static boolean isArrayOrPrimitiveArrayIndices(@NotNull CallableDescriptor descriptor) {
        if (!isTopLevelInPackage(descriptor, "indices", "kotlin.collections")) return false;

        ReceiverParameterDescriptor extensionReceiver = descriptor.getExtensionReceiverParameter();
        if (extensionReceiver == null) return false;
        KotlinType extensionReceiverType = extensionReceiver.getType();
        if (!KotlinBuiltIns.isArray(extensionReceiverType) && !KotlinBuiltIns.isPrimitiveArray(extensionReceiverType)) return false;

        return true;
    }

    public static boolean isCollectionIndices(@NotNull CallableDescriptor descriptor) {
        if (!isTopLevelInPackage(descriptor, "indices", "kotlin.collections")) return false;

        ReceiverParameterDescriptor extensionReceiver = descriptor.getExtensionReceiverParameter();
        if (extensionReceiver == null) return false;
        KotlinType extensionReceiverType = extensionReceiver.getType();
        if (!KotlinBuiltIns.isCollectionOrNullableCollection(extensionReceiverType)) return false;

        return true;
    }

    public static boolean isCharSequenceIndices(@NotNull CallableDescriptor descriptor) {
        if (!isTopLevelInPackage(descriptor, "indices", "kotlin.text")) return false;

        ReceiverParameterDescriptor extensionReceiver = descriptor.getExtensionReceiverParameter();
        if (extensionReceiver == null) return false;
        KotlinType extensionReceiverType = extensionReceiver.getType();
        if (!KotlinBuiltIns.isCharSequenceOrNullableCharSequence(extensionReceiverType)) return false;

        return true;
    }

    /*
     * Checks whether rangeTo expression is optimizable target of contains operator
     */
    public static boolean isOptimizableRangeTo(@NotNull KtSimpleNameExpression operationReference, @NotNull BindingContext bindingContext) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilKt
                .getResolvedCallWithAssert(operationReference, bindingContext);
        ReceiverValue receiver = resolvedCall.getDispatchReceiver();

        /*
         * Range is optimizable if
         * 'in' receiver is expression 'rangeTo' from stdlib package and its argument
         * has same primitive type as generic range parameter.
         * For non-matching primitive types (e.g. int in double range)
         * dispatch receiver will be null, because extension method will be called.
         */
        if (receiver instanceof ExpressionReceiver) {
            ExpressionReceiver e = (ExpressionReceiver) receiver;
            ResolvedCall<? extends CallableDescriptor> resolvedReceiver =
                    CallUtilKt.getResolvedCall(e.getExpression(), bindingContext);

            if (resolvedReceiver == null) {
                return false;
            }

            CallableDescriptor descriptor = resolvedReceiver.getResultingDescriptor();
            // kotlin.ranges.Ranges#rangeTo: ClosedRange<T> and T is primitive
            // noinspection ConstantConditions
            return isBuiltInRangeTo(descriptor) && KotlinBuiltIns.isPrimitiveType(descriptor.getExtensionReceiverParameter().getType());
        }

        return false;
    }

    /*
     * Checks whether for expression 'x in a..b' a..b is primitive integral range
     * with same type as x.
     */
    public static boolean isOptimizablePrimitiveRangeSpecialization(
            @NotNull Type argumentType,
            @NotNull KtExpression rangeExpression,
            @NotNull BindingContext bindingContext
    ) {
        if (rangeExpression instanceof KtBinaryExpression) {
            KtBinaryExpression binaryExpression = (KtBinaryExpression) rangeExpression;
            if (binaryExpression.getOperationReference().getReferencedNameElementType() == KtTokens.RANGE) {
                KotlinType kotlinType = bindingContext.getType(rangeExpression);
                assert kotlinType != null;
                DeclarationDescriptor descriptor = kotlinType.getConstructor().getDeclarationDescriptor();

                // noinspection ConstantConditions
                if (DescriptorUtilsKt.getBuiltIns(descriptor).getIntegralRanges().contains(descriptor)) {
                    if ("LongRange".equals(descriptor.getName().asString())) {
                        return argumentType == Type.LONG_TYPE;
                    }

                    return AsmUtil.isIntPrimitive(argumentType);
                }
            }
        }

        return false;
    }

    private static boolean isBuiltInRangeTo(@NotNull CallableDescriptor descriptor) {
        if (!isTopLevelInPackage(descriptor, "rangeTo", "kotlin.ranges")) {
            return false;
        }

        ReceiverParameterDescriptor extensionReceiver = descriptor.getExtensionReceiverParameter();
        return extensionReceiver != null;
    }

    private static boolean isTopLevelInPackage(@NotNull CallableDescriptor descriptor, @NotNull String name, @NotNull String packageName) {
        if (!name.equals(descriptor.getName().asString())) return false;

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof PackageFragmentDescriptor)) return false;
        String packageFqName = ((PackageFragmentDescriptor) containingDeclaration).getFqName().asString();
        if (!packageName.equals(packageFqName)) return false;

        return true;
    }
}
