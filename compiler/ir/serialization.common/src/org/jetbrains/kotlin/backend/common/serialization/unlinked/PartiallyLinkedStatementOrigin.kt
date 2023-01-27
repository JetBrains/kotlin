/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression

internal enum class PartiallyLinkedStatementOrigin : IrStatementOrigin {
    /** An [IrExpression] that represents an PL runtime error. */
    PARTIAL_LINKAGE_RUNTIME_ERROR,

    /** An [IrDelegatingConstructorCall] that restores the correct constructor delegation. */
    FIXED_CONSTRUCTOR_DELEGATION
}
