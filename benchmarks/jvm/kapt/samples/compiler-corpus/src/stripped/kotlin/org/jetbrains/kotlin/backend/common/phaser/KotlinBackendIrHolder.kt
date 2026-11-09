/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.ir.IrElement

/**
 * Adapter interface that can be implemented by phase context, input or output
 * to enable IR validation, dumping and possibly other pre- and postprocessing.
 */
interface KotlinBackendIrHolder {
    val kotlinIr: IrElement
}