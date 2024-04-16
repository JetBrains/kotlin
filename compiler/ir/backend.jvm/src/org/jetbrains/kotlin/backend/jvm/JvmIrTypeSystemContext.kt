/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.ir.IrJvmFlexibleType
import org.jetbrains.kotlin.backend.jvm.ir.asJvmFlexibleType
import org.jetbrains.kotlin.backend.jvm.ir.isWithFlexibleMutability
import org.jetbrains.kotlin.backend.jvm.ir.isWithFlexibleNullability
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.types.model.FlexibleTypeMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.ir.types.isMarkedNullable as irIsMarkedNullable

class JvmIrTypeSystemContext(override val irBuiltIns: IrBuiltIns) : IrTypeSystemContext {
    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? =
        (this as IrType).asJvmFlexibleType(irBuiltIns, JvmIrSpecialAnnotationSymbolProvider)

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        return when (this) {
            is IrJvmFlexibleType -> this.upperBound
            else -> error("Unexpected flexible type ${this::class.java.simpleName}: $this")
        }
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        return when (this) {
            is IrJvmFlexibleType -> this.lowerBound
            else -> error("Unexpected flexible type ${this::class.java.simpleName}: $this")
        }
    }

    override fun KotlinTypeMarker.isMarkedNullable(): Boolean =
        this is IrSimpleType && !isWithFlexibleNullability() && irIsMarkedNullable()

    override fun KotlinTypeMarker.isDynamic(): Boolean =
        false

    override fun KotlinTypeMarker.isFlexibleWithDifferentTypeConstructors(): Boolean =
        (this as IrType).isWithFlexibleMutability()
}
