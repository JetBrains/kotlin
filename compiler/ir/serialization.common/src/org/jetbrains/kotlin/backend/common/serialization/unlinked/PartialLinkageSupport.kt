/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrMessageLogger

interface PartialLinkageSupport {
    val partialLinkageEnabled: Boolean

    /** For general use in IR linker. */
    fun markUsedClassifiersExcludingUnlinkedFromFakeOverrideBuilding(fakeOverrideBuilder: FakeOverrideBuilder)

    /** For local use only in inline lazy-IR functions. */
    fun markUsedClassifiersInInlineLazyIrFunction(function: IrFunction)

    fun processUnlinkedDeclarations(messageLogger: IrMessageLogger, lazyRoots: () -> List<IrElement>)

    interface UnlinkedMarkerTypeHandler {
        val unlinkedMarkerType: IrType
        fun IrType.isUnlinkedMarkerType(): Boolean
    }

    companion object {
        val DISABLED = object : PartialLinkageSupport {
            override val partialLinkageEnabled get() = false
            override fun markUsedClassifiersExcludingUnlinkedFromFakeOverrideBuilding(fakeOverrideBuilder: FakeOverrideBuilder) = Unit
            override fun markUsedClassifiersInInlineLazyIrFunction(function: IrFunction) = Unit
            override fun processUnlinkedDeclarations(messageLogger: IrMessageLogger, lazyRoots: () -> List<IrElement>) = Unit
        }
    }
}
