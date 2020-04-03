/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val annotationPhase = makeIrFilePhase<JvmBackendContext>(
    { AnnotationLowering() },
    name = "Annotation",
    description = "Remove constructors of annotation classes"
)

private class AnnotationLowering : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid(this)

    override fun visitClass(declaration: IrClass): IrStatement =
        declaration.transformPostfix {
            if (isAnnotationClass) {
                declarations.removeIf { it is IrConstructor }
            }
        }
}
