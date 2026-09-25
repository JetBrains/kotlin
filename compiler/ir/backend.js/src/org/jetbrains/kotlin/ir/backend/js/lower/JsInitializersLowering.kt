/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.InitializersCleanupLowering
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationPopupLowering
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField

@PhasePrerequisites(
    EnumClassConstructorLowering::class,
    PrimaryConstructorLowering::class,
    AnnotationConstructorLowering::class,
    LocalDeclarationPopupLowering::class
)
internal class JsInitializersLowering(context: JsIrBackendContext) : InitializersLowering(context)

abstract class WebInitializersCleanupLowering(context: JsCommonBackendContext) : InitializersCleanupLowering(context) {
    override fun shouldEraseFieldInitializer(field: IrField): Boolean {
        return super.shouldEraseFieldInitializer(field)
                && field.origin != WebStaticInitializersDeclarationLowering.STATIC_CLASS_INITIALIZER // We need to preserve initializers for `static_init_state` fields (KT-89144).
                && field.origin != IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE // Keep the initializer for eagerly initialized objects
    }
}

@PhasePrerequisites(JsInitializersLowering::class)
internal class JsInitializersCleanupLowering(context: JsIrBackendContext) : WebInitializersCleanupLowering(context)
