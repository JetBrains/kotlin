/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.memoryOptimizedMap

inline fun <reified T : IrElement> T.deepCopyWithSymbols(
    initialParent: IrDeclarationParent? = null,
    createTypeRemapper: (SymbolRemapper) -> TypeRemapper = ::DeepCopyTypeRemapper,
): T {
    return (deepCopyImpl(createTypeRemapper) as T).patchDeclarationParents(initialParent)
}

inline fun <reified T : IrElement> T.deepCopyWithoutPatchingParents(): T {
    return deepCopyImpl(::DeepCopyTypeRemapper) as T
}

@PublishedApi
internal inline fun <T : IrElement> T.deepCopyImpl(createTypeRemapper: (SymbolRemapper) -> TypeRemapper): IrElement {
    val symbolRemapper = DeepCopySymbolRemapper()
    acceptVoid(symbolRemapper)
    val typeRemapper = createTypeRemapper(symbolRemapper)
    return transform(DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper), null)
}

abstract class IrDeepCopyBase : IrElementTransformerVoid() {
    protected abstract fun IrType.remapType(): IrType

    protected open fun <D : IrElement> D.processAttributes(other: IrElement) =
        copyAttributes(other)

    protected inline fun <reified T : IrElement> T.transform() =
        transform(this@IrDeepCopyBase, null) as T

    protected fun IrMutableAnnotationContainer.transformAnnotations(declaration: IrAnnotationContainer) {
        annotations = declaration.annotations.memoryOptimizedMap { it.transform() }
    }

    protected fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(other: IrMemberAccessExpression<*>) {
        typeArguments.assignFrom(other.typeArguments) { it?.remapType() }
    }

    protected fun <T : IrMemberAccessExpression<*>> T.transformValueArguments(original: T) {
        rawCopyValueArgumentsFrom(original)
        for ((i, arg) in original.arguments.withIndex()) {
            arguments[i] = arg?.transform()
        }
    }
}