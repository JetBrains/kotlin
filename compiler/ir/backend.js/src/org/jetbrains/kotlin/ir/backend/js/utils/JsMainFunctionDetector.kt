/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.Variance

object JsMainFunctionDetector {


    private fun IrSimpleFunction.isSuitableForMainParametersSize(allowEmptyParameters: Boolean): Boolean =
        when (valueParameters.size) {
            1 -> true
            0 -> allowEmptyParameters
            else -> false
        }

    private fun IrSimpleFunction.isMain(allowEmptyParameters: Boolean): Boolean {
        if (typeParameters.isNotEmpty()) return false
        if (!isSuitableForMainParametersSize(allowEmptyParameters)) return false
        if (!returnType.isUnit()) return false
        if (name.asString() != "main") return false
        if (extensionReceiverParameter != null) return false

        if (valueParameters.size == 1) {
            val parameter = valueParameters.single()

            if (!parameter.type.isArray()) return false

            val type = parameter.type as IrSimpleType

            if (type.arguments.size != 1) return false

            val argument = type.arguments.single() as? IrTypeProjection ?: return false

            if (argument.variance == Variance.IN_VARIANCE) return false

            return argument.type.isString()
        } else {
            require(allowEmptyParameters)
            require(valueParameters.isEmpty())

            val file = parent as IrFile

            return !file.declarations.filterIsInstance<IrSimpleFunction>().any { it.isMain(allowEmptyParameters = false) }
        }
    }

    fun getMainFunctionOrNull(module: IrModuleFragment): IrSimpleFunction? {

        var resultPair: Pair<String, IrSimpleFunction>? = null

        module.files.forEach { f ->
            val fqn = f.fqName.asString()

            f.declarations.filterIsInstance<IrSimpleFunction>().singleOrNull { it.isMain(allowEmptyParameters = true) }?.let {
                val result = resultPair
                if (result == null) {
                    resultPair = Pair(fqn, it)
                } else {
                    if (fqn < result.first) {
                        resultPair = Pair(fqn, it)
                    }
                }
            }
        }

        return resultPair?.second
    }
}