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

package org.jetbrains.jet.codegen;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.isPrimitiveNumberClassDescriptor;

public class RangeCodegenUtil {
    private static final ImmutableMap<FqName, PrimitiveType> RANGE_TO_ELEMENT_TYPE;
    private static final ImmutableMap<FqName, PrimitiveType> PROGRESSION_TO_ELEMENT_TYPE;

    static {
        ImmutableMap.Builder<FqName, PrimitiveType> rangeBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<FqName, PrimitiveType> progressionBuilder = ImmutableMap.builder();
        for (PrimitiveType primitiveType : PrimitiveType.values()) {
            rangeBuilder.put(getRangeClassFqName(primitiveType), primitiveType);
            progressionBuilder.put(getProgressionClassFqName(primitiveType), primitiveType);
        }
        RANGE_TO_ELEMENT_TYPE = rangeBuilder.build();
        PROGRESSION_TO_ELEMENT_TYPE = progressionBuilder.build();
    }

    private RangeCodegenUtil() {}

    public static boolean isRange(JetType rangeType) {
        return !rangeType.isNullable() && getPrimitiveRangeElementType(rangeType) != null;
    }

    public static boolean isProgression(JetType rangeType) {
        return !rangeType.isNullable() && getPrimitiveProgressionElementType(rangeType) != null;
    }

    @Nullable
    public static BinaryCall getRangeAsBinaryCall(@NotNull JetForExpression forExpression) {
        // We are looking for rangeTo() calls
        // Other binary operations will succeed too, but will be filtered out later (by examining a resolvedCall)
        JetExpression rangeExpression = forExpression.getLoopRange();
        assert rangeExpression != null;
        JetExpression loopRange = JetPsiUtil.deparenthesize(rangeExpression);
        if (loopRange instanceof JetQualifiedExpression) {
            // a.rangeTo(b)
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) loopRange;
            JetExpression selector = qualifiedExpression.getSelectorExpression();
            if (selector instanceof JetCallExpression) {
                JetCallExpression callExpression = (JetCallExpression) selector;
                List<? extends ValueArgument> arguments = callExpression.getValueArguments();
                if (arguments.size() == 1) {
                    return new BinaryCall(qualifiedExpression.getReceiverExpression(), callExpression.getCalleeExpression(),
                                          arguments.get(0).getArgumentExpression());
                }
            }
        }
        else if (loopRange instanceof JetBinaryExpression) {
            // a rangeTo b
            // a .. b
            JetBinaryExpression binaryExpression = (JetBinaryExpression) loopRange;
            return new BinaryCall(binaryExpression.getLeft(), binaryExpression.getOperationReference(), binaryExpression.getRight());

        }
        return null;
    }

    @Nullable
    private static PrimitiveType getPrimitiveRangeElementType(JetType rangeType) {
        return getPrimitiveRangeOrProgressionElementType(rangeType, RANGE_TO_ELEMENT_TYPE);
    }

    @Nullable
    private static PrimitiveType getPrimitiveProgressionElementType(JetType rangeType) {
        return getPrimitiveRangeOrProgressionElementType(rangeType, PROGRESSION_TO_ELEMENT_TYPE);
    }

    @Nullable
    private static PrimitiveType getPrimitiveRangeOrProgressionElementType(
            @NotNull JetType rangeOrProgression,
            @NotNull ImmutableMap<FqName, PrimitiveType> map
    ) {
        ClassifierDescriptor declarationDescriptor = rangeOrProgression.getConstructor().getDeclarationDescriptor();
        assert declarationDescriptor != null;
        if (declarationDescriptor != KotlinBuiltIns.getInstance().getBuiltInsPackageScope().getClassifier(declarationDescriptor.getName())) {
            // Must be a standard library class
            return null;
        }
        return map.get(DescriptorUtils.getFqNameSafe(declarationDescriptor));
    }

    public static boolean isOptimizableRangeTo(CallableDescriptor rangeTo) {
        if ("rangeTo".equals(rangeTo.getName().asString())) {
            if (isPrimitiveNumberClassDescriptor(rangeTo.getContainingDeclaration())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static FqName getRangeClassFqName(@NotNull PrimitiveType type) {
        return KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier(type.getTypeName() + "Range"));
    }

    @NotNull
    public static FqName getProgressionClassFqName(@NotNull PrimitiveType type) {
        return KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier(type.getTypeName() + "Progression"));
    }

    public static class BinaryCall {
        public final JetExpression left;
        public final JetExpression op;
        public final JetExpression right;

        private BinaryCall(JetExpression left, JetExpression op, JetExpression right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }
    }
}
