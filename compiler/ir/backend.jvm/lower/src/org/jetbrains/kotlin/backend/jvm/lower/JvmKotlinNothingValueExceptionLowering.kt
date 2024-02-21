/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.KotlinNothingValueExceptionLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.shouldContainSuspendMarkers
import org.jetbrains.kotlin.ir.declarations.IrFunction

@PhaseDescription(
    name = "KotlinNothingValueException",
    description = "Throw proper exception for calls returning value of type 'kotlin.Nothing'"
)
internal class JvmKotlinNothingValueExceptionLowering(context: JvmBackendContext) : KotlinNothingValueExceptionLowering(
    context,
    { it is IrFunction && !it.shouldContainSuspendMarkers() }
)
