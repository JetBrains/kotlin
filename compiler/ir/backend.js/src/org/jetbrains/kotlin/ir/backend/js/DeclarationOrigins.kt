/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

object JsLoweredDeclarationOrigin {
    val JS_INTRINSICS_STUB by IrDeclarationOriginImpl
    val JS_CLOSURE_BOX_CLASS_DECLARATION by IrDeclarationOriginImpl
    val BRIDGE_WITH_STABLE_NAME by IrDeclarationOriginImpl
    val BRIDGE_WITHOUT_STABLE_NAME by IrDeclarationOriginImpl
    val BRIDGE_PROPERTY_ACCESSOR by IrDeclarationOriginImpl
    val OBJECT_GET_INSTANCE_FUNCTION by IrDeclarationOriginImpl
    val JS_SHADOWED_EXPORT by IrDeclarationOriginImpl
    val JS_SUPER_CONTEXT_PARAMETER by IrDeclarationOriginImpl
    val JS_SHADOWED_DEFAULT_PARAMETER by IrDeclarationOriginImpl
    val ENUM_GET_INSTANCE_FUNCTION by IrDeclarationOriginImpl

    fun isBridgeDeclarationOrigin(origin: IrDeclarationOrigin) = when (origin) {
        BRIDGE_WITH_STABLE_NAME -> true
        BRIDGE_WITHOUT_STABLE_NAME -> true
        BRIDGE_PROPERTY_ACCESSOR -> true
        else -> false
    }
}
