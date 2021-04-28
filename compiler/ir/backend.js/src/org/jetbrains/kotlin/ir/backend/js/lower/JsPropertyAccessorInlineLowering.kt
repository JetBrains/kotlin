/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.declarations.IrProperty

class JsPropertyAccessorInlineLowering(
    val context: JsIrBackendContext
) : PropertyAccessorInlineLowering(context) {
    override val IrProperty.isSafeToInline: Boolean
        get() {
            if (context.granularity != JsGenerationGranularity.WHOLE_PROGRAM)
                return isConst
            return isSafeToInlineInClosedWorld()
        }
}