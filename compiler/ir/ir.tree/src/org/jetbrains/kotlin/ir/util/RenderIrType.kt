/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType

fun IrType.render() =
    originalKotlinType?.let {
        RenderIrElementVisitor.DECLARATION_RENDERER.renderType(it)
    } ?: "IrType without originalKotlinType: $this"