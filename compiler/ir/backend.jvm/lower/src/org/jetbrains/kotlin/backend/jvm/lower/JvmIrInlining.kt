/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.inline.AbstractDeepCopyIrTreeWithSymbolsForInliner
import org.jetbrains.kotlin.backend.common.lower.inline.InlinerTypeRemapper
import org.jetbrains.kotlin.backend.common.lower.inline.IrTreeWithSymbolsCopier
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.SymbolRemapper

class JvmInlinerTypeRemapper(
    private val context: JvmBackendContext,
    symbolRemapper: SymbolRemapper,
    typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
) : InlinerTypeRemapper(symbolRemapper, typeArguments) {

    var boxPrimitives = false

    override fun transformBackendSpecific(type: IrType): IrType {
        if (!boxPrimitives) return type
        val symbols = context.ir.symbols
        return when {
            type.isBoolean() -> symbols.javaLangBool.defaultType
            type.isByte() -> symbols.javaLangByte.defaultType
            type.isShort() -> symbols.javaLangShort.defaultType
            type.isChar() -> symbols.javaLangChar.defaultType
            type.isInt() -> symbols.javaLangInteger.defaultType
            type.isFloat() -> symbols.javaLangFloat.defaultType
            type.isLong() -> symbols.javaLangLong.defaultType
            type.isDouble() -> symbols.javaLangDouble.defaultType
            else -> type
        }
    }
}

class JvmDeepCopyIrTreeWithSymbolsForInliner(
    context: JvmBackendContext,
    typeArguments: Map<IrTypeParameterSymbol, IrType?>?, parent: IrDeclarationParent?,
) : AbstractDeepCopyIrTreeWithSymbolsForInliner(typeArguments, parent) {

    override val copier: JvmIrTreeWithSymbolsCopier

    init {
        val typeRemapper = JvmInlinerTypeRemapper(context, symbolRemapper, typeArguments)
        copier = JvmIrTreeWithSymbolsCopier(symbolRemapper, typeRemapper)
        typeRemapper.copier = copier
    }
}

class JvmIrTreeWithSymbolsCopier(
    symbolRemapper: SymbolRemapper,
    override val typeRemapper: JvmInlinerTypeRemapper,
) : IrTreeWithSymbolsCopier(symbolRemapper, typeRemapper) {

    override fun visitClassReference(expression: IrClassReference): IrClassReference {
        val boxPrimitivesBefore = typeRemapper.boxPrimitives
        typeRemapper.boxPrimitives = true
        return super.visitClassReference(expression).also { typeRemapper.boxPrimitives = boxPrimitivesBefore }
    }

}