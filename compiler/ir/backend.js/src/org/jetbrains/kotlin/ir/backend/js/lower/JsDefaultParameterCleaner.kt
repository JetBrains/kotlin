/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultParameterCleaner
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.config.useEs6DefaultArgs

class JsDefaultParameterCleaner(context: JsIrBackendContext) : DefaultParameterCleaner(context) {
    override fun lower(irModule: IrModuleFragment) {
        if (context.configuration.useEs6DefaultArgs) return
        super.lower(irModule)
    }
}
