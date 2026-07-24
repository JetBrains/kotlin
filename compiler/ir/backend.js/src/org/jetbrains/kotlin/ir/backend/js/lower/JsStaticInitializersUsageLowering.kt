/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext

@PhasePrerequisites(WebStaticInitializersDeclarationLowering::class)
class JsStaticInitializersUsageLowering(context: JsCommonBackendContext) : WebStaticInitializersUsageLowering(
    context,
    initializeContainerOfInnerObject = true
)
