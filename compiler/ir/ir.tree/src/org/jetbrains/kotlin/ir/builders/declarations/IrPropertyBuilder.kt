/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.Modality

class IrPropertyBuilder : IrDeclarationBuilder() {
    var modality: Modality = Modality.FINAL

    var isVar: Boolean = false
    var isConst: Boolean = false
    var isLateinit: Boolean = false
    var isDelegated: Boolean = false
    var isExternal: Boolean = false
    var isExpect: Boolean = false
    var isFakeOverride: Boolean = false
}
