/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
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
        if (!isTopLevel) return true

        // TODO: teach the deserializer to load constant property initializers
        if (context.icCompatibleIr2Js) {
            val accessFile = accessContainer.fileOrNull ?: return false
            val file = fileOrNull ?: return false

            return accessFile == file
        }

        if (isConst)
            return true

        return when (context.granularity) {
            JsGenerationGranularity.WHOLE_PROGRAM ->
                true
            JsGenerationGranularity.PER_MODULE -> {
                val accessModule = accessContainer.fileOrNull?.module ?: return false
                val module = fileOrNull?.module ?: return false
                accessModule == module
            }
            JsGenerationGranularity.PER_FILE ->
                // Not inlining because
                //   1. we need a way to distinguish per-file generation units
                //   2. per-file mode intended for debug builds only at the moment
                false
        }
    }
}