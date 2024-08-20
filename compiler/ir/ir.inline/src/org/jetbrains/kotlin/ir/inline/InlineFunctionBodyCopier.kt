/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.utils.memoryOptimizedMap

internal class InlineFunctionBodyCopier(
    symbolRemapper: SymbolRemapper,
    private val typeRemapper: InlinerTypeRemapper,
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {

    private fun IrType.leaveNonReifiedAsIs() = typeRemapper.remapType(this, NonReifiedTypeParameterRemappingMode.LEAVE_AS_IS)

    private fun IrType.substituteAll() = typeRemapper.remapType(this, NonReifiedTypeParameterRemappingMode.SUBSTITUTE)

    override fun visitClass(declaration: IrClass): IrClass {
        // Substitute type argument to make Class::genericSuperclass work as expected (see kt52417.kt)
        // Substitution to the super types does not lead to reification and therefore is safe
        return super.visitClass(declaration).apply {
            superTypes = declaration.superTypes.memoryOptimizedMap {
                it.substituteAll()
            }
        }
    }

    override fun visitCall(expression: IrCall): IrCall {
        val expressionCopy = super.visitCall(expression)
        if (Symbols.isTypeOfIntrinsic(expression.symbol)) {
            // We should neither erase nor substitute non-reified type parameters in the `typeOf` call so that reflection is able
            // to create a proper KTypeParameter for it. See KT-60175, KT-30279.
            expressionCopy.putTypeArgument(0, expression.getTypeArgument(0)?.leaveNonReifiedAsIs())
        }
        return expressionCopy
    }
}
