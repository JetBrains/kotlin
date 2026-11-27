/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.builders.declarations.v2.IrSimpleFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.v2.irFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull

class MethodsFromAnyGeneratorForLowerings(val context: LoweringContext, val irClass: IrClass, val origin: IrDeclarationOrigin) {
    private fun IrClass.addSyntheticFunction(name: String, returnType: IrType, builder: IrSimpleFunctionBuilder.() -> Unit = {}) =
        irFunction {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            this.name = Name.identifier(name)
            this.returnType = returnType
            builder()
        }

    fun createToStringMethodDeclaration(): IrSimpleFunction =
        irClass.addSyntheticFunction("toString", context.irBuiltIns.stringType) {
            overriddenSymbols = irClass.collectOverridenSymbols { it.isToString() }
        }

    fun createHashCodeMethodDeclaration(): IrSimpleFunction =
        irClass.addSyntheticFunction("hashCode", context.irBuiltIns.intType) {
            overriddenSymbols = irClass.collectOverridenSymbols { it.isHashCode() }
        }

    fun createEqualsMethodDeclaration(): IrSimpleFunction =
        irClass.addSyntheticFunction("equals", context.irBuiltIns.booleanType) {
            overriddenSymbols = irClass.collectOverridenSymbols { it.isEquals() }
            valueParameter("other", context.irBuiltIns.anyNType)
        }

    companion object {
        fun IrClass.collectOverridenSymbols(predicate: (IrFunction) -> Boolean): List<IrSimpleFunctionSymbol> =
            superTypes.memoryOptimizedMapNotNull { it.getClass()?.functions?.singleOrNull(predicate)?.symbol }
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

    override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
        // no-op — irFunction from lowering should already have necessary parameters
    }

    override fun getProperty(irValueParameter: IrValueParameter?): IrProperty {
        error("This API shouldn't be used in lowerings")
    }

    override fun getHashCodeFunctionInfo(type: IrType): HashCodeFunctionInfo {
        val symbol = if (type.isArray() || type.isPrimitiveArray()) {
            context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
        } else {
            type.classOrNull?.functions?.singleOrNull { it.owner.isHashCode() }
                ?: context.irBuiltIns.anyClass.functions.single { it.owner.name.asString() == "hashCode" }
        }
        return object : HashCodeFunctionInfo {
            override val symbol: IrSimpleFunctionSymbol = symbol

            override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression<*>) {}
        }
    }
}
