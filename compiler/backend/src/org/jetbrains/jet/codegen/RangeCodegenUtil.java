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
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.isPrimitiveNumberClassDescriptor;

public class RangeCodegenUtil {
    private static final ImmutableMap<String, JetType> RANGE_TO_ELEMENT_TYPE = ImmutableMap.<String, JetType>builder()
            .put("ByteRange", KotlinBuiltIns.getInstance().getByteType())
            .put("ShortRange", KotlinBuiltIns.getInstance().getShortType())
            .put("IntRange", KotlinBuiltIns.getInstance().getIntType())
            .put("LongRange", KotlinBuiltIns.getInstance().getLongType())
            .put("FloatRange", KotlinBuiltIns.getInstance().getFloatType())
            .put("DoubleRange", KotlinBuiltIns.getInstance().getDoubleType())
            .put("CharRange", KotlinBuiltIns.getInstance().getCharType())
            .build();

    private RangeCodegenUtil() {}

    public static boolean isRange(JetType rangeType) {
        return !rangeType.isNullable() && getPrimitiveRangeElementType(rangeType) != null;
    }

    @Nullable
    public static BinaryCall getRangeAsBinaryCall(@NotNull JetForExpression forExpression) {
        // We are looking for rangeTo() calls
        // Other binary operations will succeed too, but will be filtered out later (by examining a resolvedCall)
        JetExpression rangeExpression = forExpression.getLoopRange();
        assert rangeExpression != null;
        JetExpression loopRange = JetPsiUtil.deparenthesizeWithNoTypeResolution(rangeExpression);
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
    public static JetType getPrimitiveRangeElementType(JetType rangeType) {
        ClassifierDescriptor declarationDescriptor = rangeType.getConstructor().getDeclarationDescriptor();
        assert declarationDescriptor != null;
        if (declarationDescriptor != KotlinBuiltIns.getInstance().getBuiltInsScope().getClassifier(declarationDescriptor.getName())) {
            // Must be a standard library class
            return null;
        }
        return RANGE_TO_ELEMENT_TYPE.get(declarationDescriptor.getName().getName());
    }

    public static boolean isOptimizableRangeTo(CallableDescriptor rangeTo) {
        if ("rangeTo".equals(rangeTo.getName().getName())) {
            if (isPrimitiveNumberClassDescriptor(rangeTo.getContainingDeclaration())) {
                return true;
            }
        }
        return false;
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
