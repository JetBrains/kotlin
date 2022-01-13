/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.types.IrType

interface UnlinkedDeclarationsSupport {
    val allowUnboundSymbols: Boolean
    fun <T : Any> whenUnboundSymbolsAllowed(action: (UnlinkedMarkerTypeHandler) -> T): T?

    interface UnlinkedMarkerTypeHandler {
        val unlinkedMarkerType: IrType
        fun IrType.isUnlinkedMarkerType(): Boolean
    }

    companion object {
        val DISABLED = object : UnlinkedDeclarationsSupport {
            override val allowUnboundSymbols get() = false
            override fun <T : Any> whenUnboundSymbolsAllowed(action: (UnlinkedMarkerTypeHandler) -> T): T? = null
        }
    }
}
