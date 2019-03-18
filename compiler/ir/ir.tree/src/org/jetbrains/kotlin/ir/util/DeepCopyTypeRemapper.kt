/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeProjectionImpl

class DeepCopyTypeRemapper(
    private val symbolRemapper: SymbolRemapper
) : TypeRemapper {

    lateinit var deepCopy: DeepCopyIrTreeWithSymbols

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        // TODO
    }

    override fun leaveScope() {
        // TODO
    }

    // TODO This is a hack
    override fun remapType(type: IrType): IrType {
        if (type !is IrSimpleType) return type

        val arguments = type.arguments.map {
            if (it is IrTypeProjection) {
                IrTypeProjectionImpl(this.remapType(it.type), it.variance)
            } else {
                it
            }
        }

        val annotations = type.annotations.map { it.transform(deepCopy, null) as IrCall }

        return IrSimpleTypeImpl(
            null,
            symbolRemapper.getReferencedClassifier(type.classifier),
            type.hasQuestionMark,
            arguments,
            annotations)
    }

}