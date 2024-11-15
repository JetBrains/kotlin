/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty

@PhaseDescription(name = "JvmLateinitLowering")
class JvmLateinitLowering(context: CommonBackendContext) : LateinitLowering(context) {
    override fun transformLateinitBackingField(backingField: IrField, property: IrProperty) {
        super.transformLateinitBackingField(backingField, property)
        backingField.visibility = property.setter?.visibility ?: property.visibility
    }
}
