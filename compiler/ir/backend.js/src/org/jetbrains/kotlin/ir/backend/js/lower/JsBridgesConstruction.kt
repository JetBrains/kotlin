/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.Signature
import org.jetbrains.kotlin.ir.backend.js.utils.jsFunctionSignature
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

class JsBridgesConstruction(context: JsCommonBackendContext) : BridgesConstruction(context) {
    override fun getFunctionSignature(function: IrSimpleFunction): Signature =
        jsFunctionSignature(function)
}
