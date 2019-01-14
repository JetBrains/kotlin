/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.collectRealOverrides

val IrFunction.realOverrideTarget: IrFunction
    get() = when (this) {
        is IrSimpleFunction -> this.realOverrideTarget
        is IrConstructor -> this
        else -> error(this)
    }

val IrSimpleFunction.realOverrideTarget: IrSimpleFunction
    get(): IrSimpleFunction {
        val realOverrides = collectRealOverrides()
        return realOverrides.find { it.modality != Modality.ABSTRACT } ?: realOverrides.first()
    }