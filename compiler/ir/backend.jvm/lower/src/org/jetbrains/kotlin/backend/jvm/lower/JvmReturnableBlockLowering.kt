/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
import org.jetbrains.kotlin.backend.common.lower.ReturnableBlockLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext

@PhaseDescription(
    name = "ReturnableBlock",
    description = "Replace returnable blocks with do-while(false) loops",
    prerequisite = [ArrayConstructorLowering::class, AssertionLowering::class, DirectInvokeLowering::class]
)
internal class JvmReturnableBlockLowering(context: JvmBackendContext) : ReturnableBlockLowering(context)
