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

package org.jetbrains.jet.lang.resolve.constants

import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.expressions.OperatorConventions

public fun propagateConstantValues(expression : JetQualifiedExpression, trace : BindingTrace, selectorExpression : JetCallExpression) {
    val wholeExpressionValue = trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression)
    if (wholeExpressionValue != null) {
        return
    }

    val receiverExpression = expression.getReceiverExpression()
    val receiverValue = trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, receiverExpression)
    if (receiverValue == null || receiverValue is ErrorValue) {
        return
    }

    val calleeExpression = selectorExpression.getCalleeExpression()
    if (calleeExpression !is JetSimpleNameExpression) {
        return
    }

    val declarationDescriptor = trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, calleeExpression)
    if (declarationDescriptor !is FunctionDescriptor) {
        return
    }

    val returnType = declarationDescriptor.getReturnType()
    if (returnType == null || !KotlinBuiltIns.getInstance().isPrimitiveType(returnType)) {
        return
    }

    val referencedName = calleeExpression.getReferencedNameAsName()
    val value = receiverValue.getValue()
    val compileTimeValue = when (value) {
        is Number -> {
            when(referencedName) {
                OperatorConventions.DOUBLE -> DoubleValue(value.toDouble())
                OperatorConventions.FLOAT -> FloatValue(value.toFloat())
                OperatorConventions.LONG ->  LongValue(value.toLong())
                OperatorConventions.SHORT -> ShortValue(value.toShort())
                OperatorConventions.BYTE ->  ByteValue(value.toByte())
                OperatorConventions.INT ->   IntValue(value.toInt())
                OperatorConventions.CHAR ->  CharValue(value.toInt().toChar())
            }
        }
        is Char -> {
            when(referencedName) {
                OperatorConventions.DOUBLE -> DoubleValue(value.toChar().toDouble())
                OperatorConventions.FLOAT -> FloatValue(value.toChar().toFloat())
                OperatorConventions.LONG ->  LongValue(value.toChar().toLong())
                OperatorConventions.SHORT -> ShortValue(value.toChar().toShort())
                OperatorConventions.BYTE ->  ByteValue(value.toChar().toByte())
                OperatorConventions.INT ->   IntValue(value.toChar().toInt())
                OperatorConventions.CHAR ->   CharValue(value.toChar())
            }
        }
        else -> null
    }

    if (compileTimeValue != null) {
        trace.record(BindingContext.COMPILE_TIME_VALUE, expression,compileTimeValue)
    }

}

