/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
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

class MethodsFromAnyGeneratorForLowerings(val context: BackendContext, val irClass: IrClass, val origin: IrDeclarationOrigin) {
    private fun IrClass.addSyntheticFunction(name: String, returnType: IrType) =
        addFunction(name, returnType, startOffset = SYNTHETIC_OFFSET, endOffset = SYNTHETIC_OFFSET)

    fun createToStringMethodDeclaration(): IrSimpleFunction =
        irClass.addSyntheticFunction("toString", context.irBuiltIns.stringType).apply {
            overriddenSymbols = irClass.collectOverridenSymbols { it.isToString() }
        }

    fun createHashCodeMethodDeclaration(): IrSimpleFunction =
        irClass.addSyntheticFunction("hashCode", context.irBuiltIns.intType).apply {
            overriddenSymbols = irClass.collectOverridenSymbols { it.isHashCode() }
        }

    fun createEqualsMethodDeclaration(): IrSimpleFunction =
        irClass.addSyntheticFunction("equals", context.irBuiltIns.booleanType).apply {
            overriddenSymbols = irClass.collectOverridenSymbols { it.isEquals() }
            addValueParameter("other", context.irBuiltIns.anyNType)
        }

    companion object {
        fun IrClass.collectOverridenSymbols(predicate: (IrFunction) -> Boolean): List<IrSimpleFunctionSymbol> =
            superTypes.memoryOptimizedMapNotNull { it.getClass()?.functions?.singleOrNull(predicate)?.symbol }
    }
}

open class LoweringDataClassMemberGenerator(
    val backendContext: BackendContext,
    irClass: IrClass,
    origin: IrDeclarationOrigin,
    forbidDirectFieldAccess: Boolean = false
) :
    DataClassMembersGenerator(
        IrGeneratorContextBase(backendContext.irBuiltIns),
        backendContext.ir.symbols.externalSymbolTable,
        irClass,
        irClass.kotlinFqName,
        origin,
        forbidDirectFieldAccess
    ) {

    override fun declareSimpleFunction(startOffset: Int, endOffset: Int, functionDescriptor: FunctionDescriptor): IrFunction {
        error("Descriptor API shouldn't be used in lowerings")
    }

    override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
        // no-op — irFunction from lowering should already have necessary parameters
    }

    override fun getProperty(parameter: ValueParameterDescriptor?, irValueParameter: IrValueParameter?): IrProperty? {
        error("Descriptor API shouldn't be used in lowerings")
    }

    override fun transform(typeParameterDescriptor: TypeParameterDescriptor): IrType {
        error("Descriptor API shouldn't be used in lowerings")
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





