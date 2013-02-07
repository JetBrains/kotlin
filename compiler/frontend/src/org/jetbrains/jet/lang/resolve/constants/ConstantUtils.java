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

package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;
import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.BYTE;
import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.INT;

public class ConstantUtils {

    public static void propagateConstantValues(JetQualifiedExpression expression, BindingTrace trace, JetSimpleNameExpression selectorExpression) {
        JetExpression receiverExpression = expression.getReceiverExpression();
        CompileTimeConstant<?> receiverValue = trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, receiverExpression);
        CompileTimeConstant<?> wholeExpressionValue = trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        DeclarationDescriptor declarationDescriptor = trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, selectorExpression);
        if (wholeExpressionValue == null && receiverValue != null && !(receiverValue instanceof ErrorValue) && receiverValue.getValue() instanceof Number
            && KotlinBuiltIns.getInstance().getNumber() == declarationDescriptor) {
            Number value = (Number) receiverValue.getValue();
            Name referencedName = selectorExpression.getReferencedNameAsName();
            if (OperatorConventions.NUMBER_CONVERSIONS.contains(referencedName)) {
                if (DOUBLE.equals(referencedName)) {
                    trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new DoubleValue(value.doubleValue()));
                }
                else if (FLOAT.equals(referencedName)) {
                    trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new FloatValue(value.floatValue()));
                }
                else if (LONG.equals(referencedName)) {
                    trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new LongValue(value.longValue()));
                }
                else if (SHORT.equals(referencedName)) {
                    trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new ShortValue(value.shortValue()));
                }
                else if (BYTE.equals(referencedName)) {
                    trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new ByteValue(value.byteValue()));
                }
                else if (INT.equals(referencedName)) {
                    trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new IntValue(value.intValue()));
                }
            }
        }
    }
}
