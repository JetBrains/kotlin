/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.render

class JvmBackend(val context: JvmBackendContext) {
    private val lower = JvmLower(context)
    private val codegen = JvmCodegen(context)

    fun lowerFile(irFile: IrFile) {
        for (extension in IrGenerationExtension.getInstances(context.state.project)) {
            extension.generate(irFile, context, context.state.bindingContext)
        }

        lower.lower(irFile)
    }

    fun generateLoweredFile(irFile: IrFile) {
        for (loweredClass in irFile.declarations) {
            if (loweredClass !is IrClass) {
                throw AssertionError("File-level declaration should be IrClass after JvmLower, got: " + loweredClass.render())
            }

            codegen.generateClass(loweredClass)
        }
    }
}
