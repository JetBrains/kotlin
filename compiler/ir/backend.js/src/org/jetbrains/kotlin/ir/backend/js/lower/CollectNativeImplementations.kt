/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

fun collectNativeImplementations(context: JsIrBackendContext, moduleFragment: IrModuleFragment) =
    CollectNativeImplementationsVisitor(context).let { collector ->
        moduleFragment.files.forEach { it.accept(collector, null) }
    }


class CollectNativeImplementationsVisitor(private val context: JsIrBackendContext) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {}

    override fun visitFile(declaration: IrFile) {
        declaration.declarations.forEach {
            context.polyfills.registerDeclarationNativeImplementation(declaration, it)
        }
        super.visitFile(declaration)
    }
}
