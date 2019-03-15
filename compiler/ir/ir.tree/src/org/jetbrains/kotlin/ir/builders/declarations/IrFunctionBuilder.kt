/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType

class IrFunctionBuilder : IrDeclarationBuilder() {

    var isInline: Boolean = false
    var isExternal: Boolean = false

    var returnType: IrType = IrUninitializedType

    var modality: Modality = Modality.FINAL
    var isTailrec: Boolean = false
    var isSuspend: Boolean = false

    var isPrimary: Boolean = false

    fun updateFrom(from: IrFunction) {
        super.updateFrom(from)

        isInline = from.isInline
        isExternal = from.isExternal

        if (from is IrSimpleFunction) {
            modality = from.modality
            isTailrec = from.isTailrec
            isSuspend = from.isSuspend
        } else {
            modality = Modality.FINAL
            isTailrec = false
            isSuspend = false
        }

        if (from is IrConstructor) {
            isPrimary = from.isPrimary
        }
    }
}