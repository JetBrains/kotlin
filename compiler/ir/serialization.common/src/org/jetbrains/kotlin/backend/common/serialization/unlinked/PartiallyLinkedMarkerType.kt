/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.Variance

/**
 * Replacement for IR types that use partially linked classifier symbols.
 * Behaves like [kotlin.Any]?. Preserves [LinkedClassifierStatus.Partially].
 */
internal class PartiallyLinkedMarkerType(
    builtIns: IrBuiltIns,
    val partialLinkageReason: LinkedClassifierStatus.Partially
) : IrSimpleType(null) {
    override val annotations get() = emptyList<IrConstructorCall>()
    override val classifier = builtIns.anyClass
    override val nullability get() = SimpleTypeNullability.MARKED_NULLABLE
    override val arguments get() = emptyList<IrTypeArgument>()
    override val abbreviation: IrTypeAbbreviation? get() = null
    override val variance get() = Variance.INVARIANT

    override fun equals(other: Any?) = (other as? PartiallyLinkedMarkerType)?.partialLinkageReason == partialLinkageReason
    override fun hashCode() = partialLinkageReason.hashCode()
}
