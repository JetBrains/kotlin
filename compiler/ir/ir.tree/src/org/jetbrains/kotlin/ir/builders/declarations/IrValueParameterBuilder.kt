/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrType

const val UNDEFINED_PARAMETER_INDEX = -1

class IrValueParameterBuilder : IrDeclarationBuilder() {
    lateinit var type: IrType

    var index: Int = UNDEFINED_PARAMETER_INDEX
    var varargElementType: IrType? = null
    var isCrossInline = false
    var isNoinline = false

    fun updateFrom(from: IrValueParameter) {
        super.updateFrom(from)

        type = from.type
        index = from.index
        varargElementType = from.varargElementType
        isCrossInline = from.isCrossinline
        isNoinline = from.isNoinline
    }
}
