/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.EffectsKind
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.effects
import org.jetbrains.kotlin.ir.backend.js.setMaxEffects
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.StandardClassIds

class EffectAnalysisLowering(context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrFunction) return
        val effectsAnnotation = container.getAnnotation(StandardClassIds.Annotations.Effects.asSingleFqName())
        if (effectsAnnotation != null) {
            val arg = effectsAnnotation.argumentMapping[StandardClassIds.Annotations.ParameterNames.effectsKind]
            if (arg is IrGetEnumValue) {
                container.setMaxEffects(EffectsKind.valueOf(arg.symbol.owner.name.asString()))
                return
            }
        }
        BodyVisitor().visit(container)
    }

    class BodyVisitor : IrVisitor<Unit, IrFunction>() {
        val alreadyVisited = hashSetOf<IrFunction>()

        fun visit(owner: IrFunction) {
            if (alreadyVisited.contains(owner)) return
            alreadyVisited.add(owner)
            owner.accept(this, owner)
        }

        override fun visitElement(element: IrElement, data: IrFunction) {
            element.acceptChildren(this, data)
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrFunction) {
            super.visitFunctionAccess(expression, data)
            if (!expression.symbol.owner.isFinal) {
                data.setMaxEffects(EffectsKind.WRITE)
                return
            }
            if (expression.symbol.owner.effects == null) {
                visit(expression.symbol.owner)
            }
            data.setMaxEffects(expression.symbol.owner.effects?.stored)
        }

        val IrFunction.isFinal: Boolean
            get() {
                if (this.isStatic) return true
                if (this !is IrSimpleFunction) return true
                if (this.modality == Modality.FINAL) return true
                (this.parent as? IrClass)?.let {
                    return it.modality == Modality.FINAL
                }
                return true
            }
    }
}
