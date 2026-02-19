/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass

class IrClassBuilder : IrDeclarationBuilder() {
    var kind: ClassKind = ClassKind.CLASS
    var modality: Modality = Modality.FINAL

    var isCompanion: Boolean = false
    var isInner: Boolean = false
    var isData: Boolean = false
    var isExternal: Boolean = false
    var isValue: Boolean = false
    var isExpect: Boolean = false
    var isFun: Boolean = false
    var hasEnumEntries: Boolean = false

    fun updateFrom(from: IrClass) {
        super.updateFrom(from)

        kind = from.kind
        modality = from.modality
        isCompanion = from.isCompanion
        isInner = from.isInner
        isData = from.isData
        isExternal = from.isExternal
        isValue = from.isValue
        isExpect = from.isExpect
        isFun = from.isFun
        hasEnumEntries = from.hasEnumEntries
    }
}
