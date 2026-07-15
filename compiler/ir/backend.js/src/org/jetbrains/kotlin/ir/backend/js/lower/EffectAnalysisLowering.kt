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
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.FIELD_FOR_OBJECT_INSTANCE
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.util.constructedClass
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

        fun IrFunction.isConstructor(): IrClass? = when {
            this is IrConstructor -> constructedClass
            origin == ES6_CONSTRUCTOR_REPLACEMENT -> parent as IrClass
            origin == ES6_PRIMARY_CONSTRUCTOR_REPLACEMENT -> parent as IrClass
            else -> null
        }

        fun visit(owner: IrFunction) {
            if (alreadyVisited.contains(owner)) return
            alreadyVisited.add(owner)
            owner.setMaxEffects(EffectsKind.PURE)
            owner.accept(this, owner)
        }

        override fun visitElement(element: IrElement, data: IrFunction) {
            element.acceptChildren(this, data)
        }

        override fun visitGetValue(expression: IrGetValue, data: IrFunction) {
            super.visitGetValue(expression, data)
            if (expression.symbol.owner.parent != data) {
                data.setMaxEffects(EffectsKind.READ)
            }
        }

        override fun visitGetField(expression: IrGetField, data: IrFunction) {
            super.visitGetField(expression, data)
            data.setMaxEffects(EffectsKind.READ)
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: IrFunction) {
            super.visitFunctionReference(expression, data)
            if (expression.symbol.owner != data) {
                data.setMaxEffects(EffectsKind.READ)
            }
        }

        override fun visitVariable(declaration: IrVariable, data: IrFunction) {
            if (declaration.origin == ES6_DELEGATING_CONSTRUCTOR_CALL_REPLACEMENT) {
                // don't check children
                // these origin checks are to prevent functions that use object's from always being WRITE
                return
            }
            super.visitVariable(declaration, data)
        }

        override fun visitSetField(expression: IrSetField, data: IrFunction) {
            val constructedClass = data.isConstructor()
            if (constructedClass != null) {
                val receiver = expression.receiver
                if (receiver is IrGetValue) {
                    if (receiver.symbol == constructedClass.thisReceiver!!.symbol) {
                        expression.acceptChildren(this, data)
                        return
                    }
                    if (receiver.symbol.owner.origin == ES6_DELEGATING_CONSTRUCTOR_CALL_REPLACEMENT) {
                        expression.acceptChildren(this, data)
                        return
                    }
                }
            }
            if (expression.symbol.owner.origin == FIELD_FOR_OBJECT_INSTANCE) {
                return
            }
            data.setMaxEffects(EffectsKind.WRITE)
        }

        override fun visitSetValue(expression: IrSetValue, data: IrFunction) {
            if (expression.symbol.owner.parent != data) {
                data.setMaxEffects(EffectsKind.WRITE)
                return
            }
            data.setMaxEffects(EffectsKind.PURE)
            super.visitSetValue(expression, data)
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrFunction) {
            if (!expression.symbol.owner.isFinal || expression.symbol.owner.isExternal) {
                data.setMaxEffects(EffectsKind.WRITE)
                return
            }
            super.visitFunctionAccess(expression, data)
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
