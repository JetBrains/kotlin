/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.js.intrinsics

import org.jetbrains.kotlin.backend.js.context.FunctionIntrinsic
import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.util.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

object SpecializedNumberConversionIntrinsic : FunctionIntrinsic {
    private val numberClasses = setOf(
            KotlinBuiltIns.FQ_NAMES._byte,
            KotlinBuiltIns.FQ_NAMES._short,
            KotlinBuiltIns.FQ_NAMES._char,
            KotlinBuiltIns.FQ_NAMES._int,
            KotlinBuiltIns.FQ_NAMES._long,
            KotlinBuiltIns.FQ_NAMES._float,
            KotlinBuiltIns.FQ_NAMES._double
    )
    private val conversionFunctions = setOf("toByte", "toShort", "toInt", "toLong", "toFloat", "toDouble", "toChar")

    override fun isApplicable(descriptor: FunctionDescriptor): Boolean {
        val owner = (descriptor.containingDeclaration as? ClassDescriptor) ?: return false
        return owner.fqNameUnsafe in numberClasses &&
               descriptor.name.asString() in conversionFunctions &&
               descriptor.valueParameters.isEmpty()
    }

    override fun apply(
            context: IrTranslationContext, call: IrCall,
            dispatchReceiver: JsExpression?, extensionReceiver: JsExpression?, arguments: List<JsExpression>
    ): JsExpression? {
        val function = call.descriptor
        val fromClass = function.containingDeclaration.fqNameUnsafe.shortName().asString()
        val toClass = function.returnType!!.constructor.declarationDescriptor!!.fqNameUnsafe.shortName().asString()
        val value = dispatchReceiver!!

        fun noConversion(): Nothing = error("Unknown conversion: $fromClass to $toClass")

        return when (fromClass) {
            "Byte",
            "Short",
            "Int" -> when (toClass) {
                "Int",
                "Float",
                "Double" -> value
                "Long" -> buildJs { intToLong(value) }
                "Short" -> buildJs { toShort(value) }
                "Byte" -> buildJs { toByte(value) }
                "Char" -> buildJs { toChar(value) }
                else -> noConversion()
            }

            "Char" -> when (toClass) {
                "Int",
                "Char",
                "Float",
                "Double" -> value
                "Long" -> buildJs { intToLong(value) }
                "Short" -> buildJs { toShort(value) }
                "Byte" -> buildJs { toByte(value) }
                else -> noConversion()
            }

            "Long" -> when (toClass) {
                "Long" -> value
                "Int" -> buildJs { longToInt(value) }
                "Float",
                "Double" -> buildJs { longToNumber(value) }
                "Short" -> buildJs { toShort(longToInt(value)) }
                "Byte" -> buildJs { toByte(longToInt(value)) }
                "Char" -> buildJs { toChar(longToInt(value)) }
                else -> noConversion()
            }

            "Float",
            "Double" -> when (toClass) {
                "Float",
                "Double" -> value
                "Int" -> buildJs { numberToInt(value) }
                "Long" -> buildJs { numberToLong(value) }
                "Short" -> buildJs { toShort(value) }
                "Byte" -> buildJs { toByte(value) }
                "Char" -> buildJs { toChar(numberToInt(value)) }
                else -> noConversion()
            }

            else -> noConversion()
        }
    }
}