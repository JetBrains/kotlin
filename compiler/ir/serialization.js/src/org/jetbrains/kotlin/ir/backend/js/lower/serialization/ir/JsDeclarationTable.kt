/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.ir.IrBuiltIns

class JsGlobalDeclarationTable(builtIns: IrBuiltIns) : GlobalDeclarationTable(JsManglerIr) {
    init {
        loadKnownBuiltins(builtIns)
    }
}