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

import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.evaluate.evaluateBinaryExpression
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator
import org.jetbrains.jet.lang.types.JetType

public fun resolveCallToCompileTimeValue(
        callName: Name,
        receiverValue: CompileTimeConstant<*>,
        arguments: Collection<CompileTimeConstant<*>>,
        expectedType: JetType
): CompileTimeConstant<*>? {
    if (arguments.isEmpty()) {
        val value = receiverValue.getValue()
        return  when (value) {
            is Number -> {
                when(callName) {
                    OperatorConventions.DOUBLE -> DoubleValue(value.toDouble())
                    OperatorConventions.FLOAT -> FloatValue(value.toFloat())
                    OperatorConventions.LONG -> LongValue(value.toLong())
                    OperatorConventions.SHORT -> ShortValue(value.toShort())
                    OperatorConventions.BYTE -> ByteValue(value.toByte())
                    OperatorConventions.INT -> IntValue(value.toInt())
                    OperatorConventions.CHAR -> CharValue(value.toInt().toChar())
                    else -> null
                }
            }
            is Char -> {
                when(callName) {
                    OperatorConventions.DOUBLE -> DoubleValue(value.toChar().toDouble())
                    OperatorConventions.FLOAT -> FloatValue(value.toChar().toFloat())
                    OperatorConventions.LONG -> LongValue(value.toChar().toLong())
                    OperatorConventions.SHORT -> ShortValue(value.toChar().toShort())
                    OperatorConventions.BYTE -> ByteValue(value.toChar().toByte())
                    OperatorConventions.INT -> IntValue(value.toChar().toInt())
                    OperatorConventions.CHAR -> CharValue(value.toChar())
                    else -> null
                }
            }
            else -> null
        }
    }
    else if (arguments.size() == 1) {
        val result = evaluateBinaryExpression(receiverValue, arguments.iterator().next(), callName)
        if (result == null) {
            return null
        }
        return ConstantExpressionEvaluator.createCompileTimeConstant(result, expectedType)
    }

    return null
}



