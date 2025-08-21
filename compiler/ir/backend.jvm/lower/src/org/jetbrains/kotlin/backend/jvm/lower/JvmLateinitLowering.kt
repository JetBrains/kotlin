/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.lower.UninitializedPropertyAccessExceptionThrower
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpression

@PhaseDescription(name = "JvmLateinitLowering")
class JvmLateinitLowering(context: CommonBackendContext) :
    LateinitLowering(context, JvmUninitializedPropertyAccessExceptionThrower(context.symbols)) {
    override fun transformLateinitBackingField(backingField: IrField, property: IrProperty) {
        super.transformLateinitBackingField(backingField, property)
        backingField.visibility = property.setter?.visibility ?: property.visibility
    }
}

open class JvmUninitializedPropertyAccessExceptionThrower(symbols: Symbols) : UninitializedPropertyAccessExceptionThrower(symbols) {
    override fun build(builder: IrBuilderWithScope, name: String): IrExpression =
        builder.irBlock {
            +super.build(builder, name)
        }
}
