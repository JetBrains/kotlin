/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.util.isAnnotationClass

internal val annotationPhase = makeIrFilePhase(
    ::AnnotationLowering,
    name = "Annotation",
    description = "Remove constructors from annotation classes"
)

private class AnnotationLowering() : ClassLoweringPass {
    constructor(@Suppress("UNUSED_PARAMETER") context: BackendContext) : this()

    override fun lower(irClass: IrClass) {
        if (!irClass.isAnnotationClass) return
        irClass.declarations.removeIf {
            it is IrConstructor
        }
    }
}