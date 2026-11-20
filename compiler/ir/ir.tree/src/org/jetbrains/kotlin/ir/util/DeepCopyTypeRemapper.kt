/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.irRichErrorType
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class DeepCopyTypeRemapper(
    private val symbolRemapper: ReferencedSymbolRemapper
) : AbstractTypeRemapper() {

    lateinit var deepCopy: DeepCopyIrTreeWithSymbols

    override fun remapTypeOrNull(type: IrType): IrType? {
        if (type !is IrSimpleType) return null
        val newClassifier = symbolRemapper.getReferencedClassifier(type.classifier)
        val typeParameters = remapTypeArguments(type.arguments)
        val errorUnionElements = type.errorUnionElements.takeIf { it.isNotEmpty() }?.let(::remapErrorUnionElements)
        return when {
            type.annotations.isEmpty() &&
                    typeParameters == null &&
                    newClassifier == type.classifier &&
                    errorUnionElements == null
                -> type

            type.errorUnionElements.isNotEmpty() -> irRichErrorType(
                anyClassifier = newClassifier,
                nullability = type.nullability,
                errorUnionElements = errorUnionElements ?: type.errorUnionElements,
                annotations = type.annotations.memoryOptimizedMap { it.transform(deepCopy, null) as IrAnnotation },
            )

            else -> IrSimpleTypeImpl(
                newClassifier,
                type.nullability,
                typeParameters ?: type.arguments,
                type.annotations.memoryOptimizedMap { it.transform(deepCopy, null) as IrAnnotation }
            )
        }
    }

    private fun remapErrorUnionElements(elements: List<IrSimpleType>): List<IrSimpleType>? {
        var anyChanged = false
        val result = elements.memoryOptimizedMap {
            val remappedArgument = remapTypeOrNull(it)
            if (remappedArgument != null) anyChanged = true
            remappedArgument as IrSimpleType? ?: it
        }
        return if (anyChanged) result else null
    }
}
