/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

object LoweredStatementOrigins {

}

object LoweredDeclarationOrigins {
    val INLINE_LAMBDA by IrDeclarationOriginImpl
}