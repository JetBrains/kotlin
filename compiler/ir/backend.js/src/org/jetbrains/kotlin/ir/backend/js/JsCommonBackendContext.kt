/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrDeclarationBuilder

interface JsCommonBackendContext : CommonBackendContext {
    override val mapping: JsMapping

    val jsIrDeclarationBuilder: JsIrDeclarationBuilder

    val es6mode: Boolean
        get() = false
}
