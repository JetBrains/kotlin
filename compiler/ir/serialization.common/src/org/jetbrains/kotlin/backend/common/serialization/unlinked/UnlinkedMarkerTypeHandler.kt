/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl

internal interface UnlinkedMarkerTypeHandler {
    val unlinkedMarkerType: IrType
    fun IrType.isUnlinkedMarkerType(): Boolean
}

internal class UnlinkedMarkerTypeHandlerImpl(builtIns: IrBuiltIns) : UnlinkedMarkerTypeHandler {
    override val unlinkedMarkerType = IrSimpleTypeImpl(
        classifier = builtIns.anyClass,
        hasQuestionMark = true,
        arguments = emptyList(),
        annotations = emptyList()
    )

    override fun IrType.isUnlinkedMarkerType(): Boolean = this === unlinkedMarkerType
}
