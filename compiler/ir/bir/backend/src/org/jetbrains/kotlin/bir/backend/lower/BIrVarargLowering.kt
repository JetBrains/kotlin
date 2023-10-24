/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.expressions.BirFunctionAccessExpression
import org.jetbrains.kotlin.bir.types.utils.makeNotNull
import org.jetbrains.kotlin.bir.types.utils.substitute
import org.jetbrains.kotlin.bir.util.hasDefaultValue
import org.jetbrains.kotlin.bir.util.typeSubstitutionMap

context(JvmBirBackendContext)
class BIrVarargLowering : BirLoweringPhase() {
    private val functionAccessesWithMissingArgument = registerIndexKey<BirFunctionAccessExpression>(false) { expression ->
        expression.valueArguments.any { it == null }
    }

    override fun invoke(module: BirModuleFragment) {
        compiledBir.getElementsWithIndex(functionAccessesWithMissingArgument).forEach { expression ->
            for (i in expression.valueArguments.indices) {
                if (expression.valueArguments[i] == null) {
                    val parameter = expression.symbol.owner.valueParameters[i]
                    if (parameter.varargElementType != null && !parameter.hasDefaultValue()) {
                        val arrayType = parameter.type.substitute(expression.typeSubstitutionMap).makeNotNull()
                        //expression.valueArguments[i] =
                    }
                }
            }
        }
    }
}