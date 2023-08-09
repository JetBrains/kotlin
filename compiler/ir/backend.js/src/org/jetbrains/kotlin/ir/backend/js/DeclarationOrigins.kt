/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

object JsLoweredDeclarationOrigin {
    object JS_INTRINSICS_STUB : IrDeclarationOriginImpl("JS_INTRINSICS_STUB")
    object JS_CLOSURE_BOX_CLASS_DECLARATION : IrDeclarationOriginImpl("JS_CLOSURE_BOX_CLASS_DECLARATION")
    object BRIDGE_WITH_STABLE_NAME : IrDeclarationOriginImpl("BRIDGE_WITH_STABLE_NAME")
    object BRIDGE_WITHOUT_STABLE_NAME : IrDeclarationOriginImpl("BRIDGE_WITHOUT_STABLE_NAME")
    object BRIDGE_PROPERTY_ACCESSOR : IrDeclarationOriginImpl("BRIDGE_PROPERTY_ACCESSOR")
    object OBJECT_GET_INSTANCE_FUNCTION : IrDeclarationOriginImpl("OBJECT_GET_INSTANCE_FUNCTION")
    object JS_SHADOWED_EXPORT : IrDeclarationOriginImpl("JS_SHADOWED_EXPORT")
    object JS_SUPER_CONTEXT_PARAMETER : IrDeclarationOriginImpl("JS_SUPER_CONTEXT_PARAMETER")
    object JS_SHADOWED_DEFAULT_PARAMETER : IrDeclarationOriginImpl("JS_SHADOWED_DEFAULT_PARAMETER")
    object ENUM_GET_INSTANCE_FUNCTION : IrDeclarationOriginImpl("ENUM_GET_INSTANCE_FUNCTION")

    fun isBridgeDeclarationOrigin(origin: IrDeclarationOrigin) = when (origin) {
        is BRIDGE_WITH_STABLE_NAME -> true
        is BRIDGE_WITHOUT_STABLE_NAME -> true
        is BRIDGE_PROPERTY_ACCESSOR -> true
        else -> false
    }
}
