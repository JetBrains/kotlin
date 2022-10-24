/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isInnerClassSuperType
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.superClass

class ES6AnnotateInnerClassParentLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrClass) return null

        if (context.icCompatibleIr2Js.isCompatible || declaration.isInner) {
            declaration.superClass?.markAsInnerClassSuperType()
        }

        return null
    }

    private fun IrClass.markAsInnerClassSuperType() {
        if (isInnerClassSuperType()) return

        val innerClassSuperTypeCtor = context.intrinsics.innerClassSuperTypeAnnotationSymbol.constructors.single()

        annotations += JsIrBuilder.buildConstructorCall(innerClassSuperTypeCtor)
        superClass?.markAsInnerClassSuperType()
    }
}
