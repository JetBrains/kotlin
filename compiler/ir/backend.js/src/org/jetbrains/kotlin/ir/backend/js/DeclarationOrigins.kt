/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

object JsLoweredDeclarationOrigin : IrDeclarationOrigin {
    object JS_INTRINSICS_STUB : IrDeclarationOriginImpl("JS_INTRINSICS_STUB")
    object JS_CLOSURE_BOX_CLASS : IrStatementOriginImpl("JS_CLOSURE_BOX_CLASS")
    object JS_CLOSURE_BOX_CLASS_DECLARATION : IrDeclarationOriginImpl("JS_CLOSURE_BOX_CLASS_DECLARATION")
    object BRIDGE_WITH_STABLE_NAME : IrDeclarationOriginImpl("BRIDGE_WITH_STABLE_NAME")
    object BRIDGE_WITHOUT_STABLE_NAME : IrDeclarationOriginImpl("BRIDGE_WITHOUT_STABLE_NAME")
    object OBJECT_GET_INSTANCE_FUNCTION : IrDeclarationOriginImpl("OBJECT_GET_INSTANCE_FUNCTION")
    object JS_SHADOWED_EXPORT : IrDeclarationOriginImpl("JS_SHADOWED_EXPORT")
}
