/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class JsDeclarationTable(builtIns: IrBuiltIns, descriptorTable: DescriptorTable) : DeclarationTable(builtIns, descriptorTable, JsMangler) {

    override var currentIndex = PUBLIC_LOCAL_UNIQ_ID_EDGE

    init {
        loadKnownBuiltins()
    }
}
