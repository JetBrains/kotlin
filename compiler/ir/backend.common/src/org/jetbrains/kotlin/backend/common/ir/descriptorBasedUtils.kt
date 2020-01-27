/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.declarations.*

val IrDeclaration.isExpect
    get() = this is IrClass && isExpect ||
            this is IrFunction && isExpect ||
            this is IrProperty && isExpect

// The original isExpect represents what user has written.
// This predicate means "there can possibly exist an 'actual' for the given declaration".
// Shouldn't it be incorporated to descriptor -> ir declaration psi2ir translation phase?
val IrDeclaration.isProperExpect: Boolean
    get() = this is IrClass && isExpect ||
            this is IrFunction && isExpect ||
            this is IrProperty && isExpect ||
            (this is IrClass || this is IrFunction || this is IrProperty || this is IrConstructor || this is IrEnumEntry)
            && (this.parent as? IrDeclaration)?.isProperExpect ?: false

