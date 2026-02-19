/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.InitializersCleanupLowering
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationPopupLowering
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext

@PhasePrerequisites(
    EnumClassConstructorLowering::class,
    PrimaryConstructorLowering::class,
    AnnotationConstructorLowering::class,
    LocalDeclarationPopupLowering::class
)
internal class JsInitializersLowering(context: JsIrBackendContext) : InitializersLowering(context)

@PhasePrerequisites(JsInitializersLowering::class)
internal class JsInitializersCleanupLowering(context: CommonBackendContext) : InitializersCleanupLowering(context)