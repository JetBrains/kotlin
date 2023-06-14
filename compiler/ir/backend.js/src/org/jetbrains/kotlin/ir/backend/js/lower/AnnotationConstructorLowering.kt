/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass

class AnnotationConstructorLowering(val context: JsCommonBackendContext) : DeclarationTransformer {

    private val unitType = context.irBuiltIns.unitType
    private val anyConstructor = context.irBuiltIns.anyClass.constructors.first()

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrConstructor || !declaration.isPrimary) return null

        val irClass = declaration.parentAsClass

        if (irClass.kind != ClassKind.ANNOTATION_CLASS) return null

        // put empty body to make sure proper initializer is generated
        // TODO what about its previous body?
        declaration.body = declaration.factory.createBlockBody(declaration.startOffset, declaration.endOffset) {
            if (context.es6mode) {
                statements += IrDelegatingConstructorCallImpl(startOffset, endOffset, context.irBuiltIns.anyType, anyConstructor, 0, 0)
            }
            statements += IrInstanceInitializerCallImpl(startOffset, endOffset, irClass.symbol, unitType)
        }

        return null
    }
}
