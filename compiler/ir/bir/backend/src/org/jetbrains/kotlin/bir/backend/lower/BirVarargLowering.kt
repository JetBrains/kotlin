/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.birBodyScope
import org.jetbrains.kotlin.bir.backend.jvm.BirArrayBuilder
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.jvm.birArray
import org.jetbrains.kotlin.bir.backend.jvm.birArrayOf
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.types.utils.makeNotNull
import org.jetbrains.kotlin.bir.types.utils.substitute
import org.jetbrains.kotlin.bir.util.*
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

context(JvmBirBackendContext)
class BirVarargLowering : BirLoweringPhase() {
    override fun lower(module: BirModuleFragment) {
        for (expression in getAllElementsOfClass(BirFunctionAccessExpression, false)) {
            expression.valueArguments.forEachIndexed { i, arg ->
                if (arg == null) {
                    val parameter = expression.symbol.owner.valueParameters[i]
                    if (parameter.varargElementType != null && !parameter.hasDefaultValue()) {
                        if (!expression.isInsideAnnotation()) {
                            val arrayType = parameter.type.substitute(expression.typeSubstitutionMap).makeNotNull()
                            expression.valueArguments[i] = birBodyScope { birArrayOf(arrayType, emptyList()) }
                        }
                    }
                }
            }
        }

        // todo: does it work with nested varargs properly?
        for (vararg in getAllElementsOfClass(BirVararg, false)) {
            if (vararg.isInsideAnnotation()) continue

            val array = birBodyScope {
                sourceSpan = vararg.sourceSpan
                birArray(vararg.type) {
                    addVariableArgumentsToArray(vararg)
                }
            }
            vararg.replaceWith(array)
        }
    }

    private fun BirArrayBuilder.addVariableArgumentsToArray(vararg: BirVararg) {
        val elements = vararg.elements.toList()
        for (element in elements) {
            when (element) {
                is BirExpression -> +element
                is BirSpreadElement -> {
                    val spread = element.expression
                    if (spread is BirFunctionAccessExpression && spread.symbol.owner in primitiveArrayOfFunctions) {
                        // Skip empty arrays and don't copy immediately created arrays
                        val argument = spread.valueArguments[0] ?: continue
                        if (argument is BirVararg) {
                            addVariableArgumentsToArray(argument)
                            continue
                        }
                    }
                    addSpread(spread)
                }
                else -> error("Unexpected IrVarargElement subclass: $element")
            }
        }
    }

    private fun BirElement.isInsideAnnotation(): Boolean =
        ancestors().takeWhile { it is BirExpression }.any { it is BirConstructorCall && it.isAnnotation }

    private val primitiveArrayOfFunctions by lz {
        (PrimitiveType.entries.map { type -> type.name } + UnsignedType.entries.map { type -> type.typeName.asString() })
            .map { name -> name.toLowerCaseAsciiOnly() + "ArrayOf" }
            .plus("arrayOf")
            .flatMap { name -> birBuiltIns.findFunctions(Name.identifier(name), StandardNames.BUILT_INS_PACKAGE_FQ_NAME.asString()) }
            .map { it.owner }
            .filter {
                it.extensionReceiverParameter == null &&
                        it.dispatchReceiverParameter == null &&
                        it.valueParameters.size == 1 &&
                        it.valueParameters[0].isVararg
            }.toSet()
    }
}
