/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrPropertyBuilder : IrDeclarationBuilder() {
    var modality: Modality = Modality.FINAL

    var isVar: Boolean = false
    var isConst: Boolean = false
    var isLateinit: Boolean = false
    var isDelegated: Boolean = false
    var isExternal: Boolean = false
    var isExpect: Boolean = false
    var isFakeOverride: Boolean = false

    var originalDeclaration: IrProperty? = null
    var containerSource: DeserializedContainerSource? = null

    fun updateFrom(from: IrProperty) {
        super.updateFrom(from)

        isVar = from.isVar
        isConst = from.isConst
        isLateinit = from.isLateinit
        isDelegated = from.isDelegated
        isExternal = from.isExternal
        isExpect = from.isExpect
        isFakeOverride = from.isFakeOverride

        originalDeclaration = from
        containerSource = from.containerSource
    }
}
