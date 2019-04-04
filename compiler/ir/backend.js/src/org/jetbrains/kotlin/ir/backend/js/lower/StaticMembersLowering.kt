/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

// Move static member declarations from classes to top level
class StaticMembersLowering(val context: JsIrBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        val staticDeclarationsInClasses = mutableListOf<IrDeclaration>()
        irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration.parent is IrClass && !declaration.isEffectivelyExternal())
                    staticDeclarationsInClasses.add(declaration)
                super.visitClass(declaration)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                if (declaration.parent is IrClass && declaration.isStaticMethodOfClass && !declaration.isEffectivelyExternal())
                    staticDeclarationsInClasses.add(declaration)
                super.visitSimpleFunction(declaration)
            }
        })

        for (declaration in staticDeclarationsInClasses) {
            val klass = declaration.parentAsClass
            val fragment = klass.getPackageFragment()!!
            klass.declarations.remove(declaration)
            fragment.addChild(declaration)
            declaration.parent = fragment
        }
    }
}