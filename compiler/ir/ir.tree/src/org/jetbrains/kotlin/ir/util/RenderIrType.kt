/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.renderer.DescriptorRenderer

fun IrType.render() =
    originalKotlinType?.let {
        DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.renderType(it)
    } ?: "UNBOUND IrType"