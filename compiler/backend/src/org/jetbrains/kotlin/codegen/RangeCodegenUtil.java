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
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.KotlinType;

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
    public static BinaryCall getRangeAsBinaryCall(@NotNull KtForExpression forExpression) {
        // We are looking for rangeTo() calls
        // Other binary operations will succeed too, but will be filtered out later (by examining a resolvedCall)
        KtExpression rangeExpression = forExpression.getLoopRange();
        assert rangeExpression != null;
        KtExpression loopRange = KtPsiUtil.deparenthesize(rangeExpression);
        if (loopRange instanceof KtQualifiedExpression) {
            // a.rangeTo(b)
            KtQualifiedExpression qualifiedExpression = (KtQualifiedExpression) loopRange;
            KtExpression selector = qualifiedExpression.getSelectorExpression();
            if (selector instanceof KtCallExpression) {
                KtCallExpression callExpression = (KtCallExpression) selector;
                List<? extends ValueArgument> arguments = callExpression.getValueArguments();
                if (arguments.size() == 1) {
                    return new BinaryCall(qualifiedExpression.getReceiverExpression(), callExpression.getCalleeExpression(),
                                          arguments.get(0).getArgumentExpression());
                }
            }
        }
        else if (loopRange instanceof KtBinaryExpression) {
            // a rangeTo b
            // a .. b
            KtBinaryExpression binaryExpression = (KtBinaryExpression) loopRange;
            return new BinaryCall(binaryExpression.getLeft(), binaryExpression.getOperationReference(), binaryExpression.getRight());

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

    public static boolean isOptimizableRangeTo(CallableDescriptor rangeTo) {
        if ("rangeTo".equals(rangeTo.getName().asString())) {
            if (isPrimitiveNumberClassDescriptor(rangeTo.getContainingDeclaration())) {
                return true;
            }
        }
        return false;
    }

    public static class BinaryCall {
        public final KtExpression left;
        public final KtExpression op;
        public final KtExpression right;

        private BinaryCall(KtExpression left, KtExpression op, KtExpression right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }
    }
}
