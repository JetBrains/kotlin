/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.SingleAbstractMethodLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.render

class JsSingleAbstractMethodLowering(context: JsIrBackendContext) : SingleAbstractMethodLowering(context) {
    override fun getSuperTypeForWrapper(typeOperand: IrType): IrType {
        // FE doesn't allow type parameters for now.
        // And since there is a to-do in common SingleAbstractMethodLowering (at function visitTypeOperator),
        // we don't have to be more saint than a pope here.
        return typeOperand.classOrNull?.defaultType ?: error("Unsupported SAM conversion: ${typeOperand.render()}")
    }
}