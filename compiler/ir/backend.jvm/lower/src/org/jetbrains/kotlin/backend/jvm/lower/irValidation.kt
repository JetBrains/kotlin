/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.phaser.validationCallback
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun validateIr(context: JvmBackendContext, module: IrModuleFragment) {
    if (!context.config.shouldValidateIr) return
    validationCallback(context, module, checkProperties = true)
}

private fun checkAllFileLevelDeclarationsAreClasses(module: IrModuleFragment) {
    assert(module.files.all { irFile ->
        irFile.declarations.all { it is IrClass }
    })
}

fun validateJvmIr(context: JvmBackendContext, module: IrModuleFragment) {
    if (!context.config.shouldValidateIr) return

    checkAllFileLevelDeclarationsAreClasses(module)
    val validator = object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitProperty(declaration: IrProperty) {
            error("No properties should remain at this stage")
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
            error("No anonymous initializers should remain at this stage")
        }
    }
    module.acceptVoid(validator)
}
