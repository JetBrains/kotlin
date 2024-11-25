/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrType

const val UNDEFINED_PARAMETER_INDEX = -1

class IrValueParameterBuilder : IrDeclarationBuilder() {
    var kind: IrParameterKind? = null
    lateinit var type: IrType

    var varargElementType: IrType? = null
    var isCrossInline = false
    var isNoinline = false
    var isHidden = false
    var isAssignable = false

    fun updateFrom(from: IrValueParameter) {
        super.updateFrom(from)

        if (from._kind != null) {
            kind = from._kind
        }
        type = from.type
        varargElementType = from.varargElementType
        isCrossInline = from.isCrossinline
        isNoinline = from.isNoinline
        isHidden = from.isHidden
        isAssignable = from.isAssignable
    }
}
