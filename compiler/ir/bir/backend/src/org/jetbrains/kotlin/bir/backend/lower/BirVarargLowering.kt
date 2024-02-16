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
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirFunction
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.declarations.BirPackageFragment
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.types.utils.makeNotNull
import org.jetbrains.kotlin.bir.types.utils.substitute
import org.jetbrains.kotlin.bir.util.*
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

context(JvmBirBackendContext)
class BirVarargLowering : BirLoweringPhase() {
    private val functionAccessesWithMissingArgument = registerIndexKey(BirFunctionAccessExpression, false) { expression ->
        expression.valueArguments.any { it == null }
    }

    private val varargs = registerIndexKey(BirVararg, false)

    override fun lower(module: BirModuleFragment) {
        getAllElementsWithIndex(functionAccessesWithMissingArgument).forEach { expression ->
            if (expression.isInsideAnnotation()) return@forEach

            for (i in expression.valueArguments.indices) {
                if (expression.valueArguments[i] == null) {
                    val parameter = expression.symbol.owner.valueParameters[i]
                    if (parameter.varargElementType != null && !parameter.hasDefaultValue()) {
                        val arrayType = parameter.type.substitute(expression.typeSubstitutionMap).makeNotNull()
                        expression.valueArguments[i] = birBodyScope { birArrayOf(arrayType, emptyList()) }
                    }
                }
            }
        }

        // todo: does it work with nested varargs properly?
        getAllElementsWithIndex(varargs).forEach { vararg ->
            if (vararg.isInsideAnnotation()) return@forEach

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
                    val spread = element.expression!!
                    if (spread is BirFunctionAccessExpression && spread.symbol.owner.isArrayOf()) {
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
}

internal val PRIMITIVE_ARRAY_OF_NAMES: Set<String> =
    (PrimitiveType.values().map { type -> type.name } + UnsignedType.values().map { type -> type.typeName.asString() })
        .map { name -> name.toLowerCaseAsciiOnly() + "ArrayOf" }.toSet()

internal const val ARRAY_OF_NAME = "arrayOf"

internal fun BirFunction.isArrayOf(): Boolean {
    val parent = ancestors().firstIsInstanceOrNull<BirPackageFragment>() ?: return false
    return parent.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME &&
            name.asString().let { it in PRIMITIVE_ARRAY_OF_NAMES || it == ARRAY_OF_NAME } &&
            extensionReceiverParameter == null &&
            dispatchReceiverParameter == null &&
            valueParameters.size == 1 &&
            valueParameters[0].isVararg
}

internal fun BirFunction.isEmptyArray(): Boolean =
    name.asString() == "emptyArray" &&
            (parent as? BirPackageFragment)?.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME
