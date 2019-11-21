/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl

class AnnotationConstructorLowering(context: CommonBackendContext) : ClassLoweringPass {

    private val unitType = context.irBuiltIns.unitType

    override fun lower(irClass: IrClass) {
        if (irClass.kind != ClassKind.ANNOTATION_CLASS) return

        val constructor = irClass.declarations.filterIsInstance<IrConstructor>().single()
        assert(constructor.isPrimary)
        // put empty body to make sure proper initializer is generated
        constructor.body = IrBlockBodyImpl(constructor.startOffset, constructor.endOffset).apply {
            statements += IrInstanceInitializerCallImpl(startOffset, endOffset, irClass.symbol, unitType)
        }
    }
}