/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrFunctionBuilder : IrDeclarationBuilder() {

    var isInline: Boolean = false
    var isExternal: Boolean = false

    var returnType: IrType = IrUninitializedType

    var modality: Modality = Modality.FINAL
    var isTailrec: Boolean = false
    var isSuspend: Boolean = false
    var isExpect: Boolean = false
    var isOperator: Boolean = false
    var isInfix: Boolean = false

    var isPrimary: Boolean = false

    var isFakeOverride: Boolean = false

    var originalDeclaration: IrFunction? = null
    var containerSource: DeserializedContainerSource? = null

    fun updateFrom(from: IrFunction) {
        super.updateFrom(from)

        containerSource = from.containerSource

        isInline = from.isInline
        isExternal = from.isExternal
        isExpect = from.isExpect

        if (from is IrSimpleFunction) {
            modality = from.modality
            isTailrec = from.isTailrec
            isSuspend = from.isSuspend
            isOperator = from.isOperator
            isInfix = from.isInfix
            isFakeOverride = from.isFakeOverride
        } else {
            modality = Modality.FINAL
            isTailrec = false
            isSuspend = false
            isOperator = false
            isInfix = false
        }

        if (from is IrConstructor) {
            isPrimary = from.isPrimary
        }
    }
}