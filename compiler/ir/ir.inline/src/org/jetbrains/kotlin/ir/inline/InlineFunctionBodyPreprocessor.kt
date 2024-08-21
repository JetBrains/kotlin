/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.inline.localClassSymbolRemapper
import org.jetbrains.kotlin.backend.common.lower.inline.useExtractedCopy
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class InlineFunctionBodyPreprocessor(
    val context: CommonBackendContext,
    val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
    val parent: IrDeclarationParent?,
    private val useLiftedLocalClasses: Boolean,
) {
    private val symbolRemapper = SymbolRemapperImpl()

    private val copier = run {
        val typeRemapper = InlinerTypeRemapper(symbolRemapper, typeArguments)
        InlineFunctionBodyCopier(symbolRemapper, typeRemapper).also { typeRemapper.copier = it }
    }

    fun preprocess(irElement: IrFunction): IrFunction {
        // Create new symbols.
        irElement.acceptVoid(symbolRemapper)

        if (useLiftedLocalClasses) {
            irElement.localClassSymbolRemapper?.let(symbolRemapper::addMappingsFrom)
        }

        // Make symbol remapper aware of the callsite's type arguments.
        symbolRemapper.typeArguments = typeArguments

        // Copy IR.
        val result = irElement.transform(copier, data = null)

        if (useLiftedLocalClasses) {
            result.transform(ExtractedLocalClassEliminator(), null)
        }

        result.patchDeclarationParents(parent)
        return result as IrFunction
    }

    private class SymbolRemapperImpl : DeepCopySymbolRemapper() {

        var typeArguments: Map<IrTypeParameterSymbol, IrType?>? = null
            set(value) {
                if (field != null) return
                field = value?.asSequence()?.associate {
                    (getReferencedClassifier(it.key) as IrTypeParameterSymbol) to it.value
                }
            }

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments?.get(result)?.classifierOrNull ?: result
        }
    }

    private inner class ExtractedLocalClassEliminator : IrElementTransformerVoid() {
        override fun visitClass(declaration: IrClass): IrStatement =
            if (declaration.useExtractedCopy) {
                IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
            } else {
                super.visitClass(declaration)
            }
    }
}
