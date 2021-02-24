/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.FlexibleTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

internal interface IrJvmFlexibleType : FlexibleTypeMarker {
    val lowerBound: SimpleTypeMarker
    val upperBound: SimpleTypeMarker
}

internal class IrJvmFlexibleNullabilityType(val irType: IrSimpleType) : IrJvmFlexibleType {
    override val lowerBound get() = irType.withHasQuestionMark(false)
    override val upperBound get() = irType.withHasQuestionMark(true)
}

internal val FLEXIBLE_NULLABILITY_FQN = FqName("kotlin.internal.ir").child(Name.identifier("FlexibleNullability"))

internal fun IrType.isWithFlexibleNullability() =
    hasAnnotation(FLEXIBLE_NULLABILITY_FQN)

internal fun IrType.asJvmFlexibleType() =
    when {
        this is IrSimpleType && isWithFlexibleNullability() ->
            IrJvmFlexibleNullabilityType(
                this.removeAnnotations { irCtorCall ->
                    irCtorCall.type.classFqName == FLEXIBLE_NULLABILITY_FQN
                } as IrSimpleType
            )
        else ->
            null
    }