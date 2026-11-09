/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull

class MethodsFromAnyGeneratorForLowerings(val context: LoweringContext, val irClass: IrClass, val origin: IrDeclarationOrigin) {
    private fun IrClass.addSyntheticFunction(name: String, returnType: IrType) =
        addFunction(name, returnType, startOffset = SYNTHETIC_OFFSET, endOffset = SYNTHETIC_OFFSET)

    fun createToStringMethodDeclaration(): IrSimpleFunction = TODO()

    fun createHashCodeMethodDeclaration(): IrSimpleFunction = TODO()

    fun createEqualsMethodDeclaration(): IrSimpleFunction = TODO()

    companion object {
        fun IrClass.collectOverridenSymbols(predicate: (IrFunction) -> Boolean): List<IrSimpleFunctionSymbol> = TODO()
    }
}

open class LoweringDataClassMemberGenerator(
    val loweringContext: LoweringContext,
    symbolTable: ReferenceSymbolTable,
    irClass: IrClass,
    origin: IrDeclarationOrigin,
    forbidDirectFieldAccess: Boolean = false
) :
    IrBasedDataClassMembersGenerator(
        IrGeneratorContextBase(loweringContext.irBuiltIns),
        symbolTable,
        irClass,
        irClass.kotlinFqName,
        origin,
        forbidDirectFieldAccess,
        generateBodies = true,
    ) {

    override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) { TODO() }

    override fun getProperty(irValueParameter: IrValueParameter?): IrProperty { TODO() }

    override fun getHashCodeFunctionInfo(type: IrType): HashCodeFunctionInfo { TODO() }
}
