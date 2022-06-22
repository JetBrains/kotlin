/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.unlinked.BasicUnlinkedDeclarationsSupport
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UnlinkedDeclarationsSupport.UnlinkedMarkerTypeHandler
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.types.Variance

class JsUnlinkedDeclarationsSupport(
    override val builtIns: IrBuiltIns,
    override val allowUnboundSymbols: Boolean
) : BasicUnlinkedDeclarationsSupport() {
    override val handler = object : UnlinkedMarkerTypeHandler {
        override val unlinkedMarkerType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)
        override fun IrType.isUnlinkedMarkerType() = this is IrErrorType
    }
}
