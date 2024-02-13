/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.utils.SmartList

class IrConstantObjectImpl constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var constructor: IrConstructorSymbol,
    initValueArguments: List<IrConstantValue>,
    initTypeArguments: List<IrType>,
    override var type: IrType = constructor.owner.constructedClassType,
) : IrConstantObject() {
    override val valueArguments = SmartList(initValueArguments)
    override val typeArguments = SmartList(initTypeArguments)

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    override fun contentEquals(other: IrConstantValue): Boolean =
        other is IrConstantObject &&
                other.type == type &&
                other.constructor == constructor &&
                valueArguments.size == other.valueArguments.size &&
                typeArguments.size == other.typeArguments.size &&
                valueArguments.indices.all { index -> valueArguments[index].contentEquals(other.valueArguments[index]) } &&
                typeArguments.indices.all { index -> typeArguments[index] == other.typeArguments[index] }


    override fun contentHashCode(): Int {
        var res = type.hashCode() * 31 + constructor.hashCode()
        for (value in valueArguments) {
            res = res * 31 + value.contentHashCode()
        }
        for (value in typeArguments) {
            res = res * 31 + value.hashCode()
        }
        return res
    }
}
