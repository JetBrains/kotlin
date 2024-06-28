/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.isTopLevel

class JsPropertyAccessorInlineLowering(
    val context: JsIrBackendContext
) : PropertyAccessorInlineLowering(context) {
    override fun IrProperty.isSafeToInline(accessContainer: IrDeclaration): Boolean {
        if (!isSafeToInlineInClosedWorld())
            return false

        // Member properties could be safely inlined, because initialization processed via parent declaration
        if (!isTopLevel && !context.incrementalCacheEnabled)
            return true

        // Just undefined value
        if (symbol == context.intrinsics.void) {
            return true
        }

        // TODO: teach the deserializer to load constant property initializers
        val accessFile = accessContainer.fileOrNull ?: return false
        val file = fileOrNull ?: return false

        return accessFile == file
    }
}
