/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.SmartList

class IrTypeParameterBuilder : IrDeclarationBuilder() {
    var index: Int = UNDEFINED_PARAMETER_INDEX
    var variance: Variance = Variance.INVARIANT
    var isReified: Boolean = false
    val superTypes: MutableList<IrType> = SmartList()

    fun updateFrom(from: IrTypeParameter) {
        super.updateFrom(from)
        index = from.index
        variance = from.variance
        isReified = from.isReified
        // Do not copy superTypes. You typically want a remapping for a group of type parameters at a time, see IrTypeParameterRemapper.
    }
}
