/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.EffectsKind
import org.jetbrains.kotlin.ir.backend.js.EffectsKindCell
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION
import org.jetbrains.kotlin.ir.backend.js.effects
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.FIELD_FOR_OBJECT_INSTANCE
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.PROPERTY_BACKING_FIELD
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.StandardClassIds
import java.lang.ref.WeakReference

class EffectAnalysisLowering(val context: JsCommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrFunction) return
        BodyVisitor().maybeVisit(container)
    }

    override fun lower(irFile: IrFile) {
        if (context is JsIrBackendContext && context.incrementalCacheEnabled) return
        context.effectAnalysisFinished = false
        super.lower(irFile)
        context.effectAnalysisFinished = true
    }

    inner class BodyVisitor : IrVisitor<Unit, IrFunction>() {
        fun IrFunction.getConstructedClass(): IrClass? = when {
            this is IrConstructor -> this.constructedClass
            isEs6ConstructorReplacement -> parent as IrClass
            else -> null
        }

        val IrFunction.nonNullEffects: EffectsKindCell
            get() {
                // this can happen if an ir node is visited outside a call to maybeVisit
                if (effects == null) throw IllegalStateException("IrElement has null effects attribute inside BodyVisitor")
                return effects!!
            }

        fun maybeVisit(owner: IrFunction): EffectsKindCell {
            if (owner.effects != null) return owner.effects!!
            val effectsAnnotation = owner.getAnnotation(StandardClassIds.Annotations.Effects.asSingleFqName())
            if (effectsAnnotation != null) {
                val arg = effectsAnnotation.argumentMapping[StandardClassIds.Annotations.ParameterNames.effectsKind]
                if (arg is IrGetEnumValue) {
                    owner.effects = EffectsKindCell(context, owner, EffectsKind.valueOf(arg.symbol.owner.name.asString()))
                    return owner.effects!!
                }
            }
            owner.effects = EffectsKindCell(context, owner, null)
            owner.accept(this, owner)
            return owner.effects!!
        }

        override fun visitElement(element: IrElement, data: IrFunction) {
            element.acceptChildren(this, data)
        }

        override fun visitGetValue(expression: IrGetValue, data: IrFunction) {
            super.visitGetValue(expression, data)
            if (expression.symbol.owner.parent != data) {
                data.nonNullEffects.setAtLeast(EffectsKind.READ)
            }
        }

        override fun visitGetField(expression: IrGetField, data: IrFunction) {
            super.visitGetField(expression, data)
            if (expression.symbol.owner.origin == PROPERTY_BACKING_FIELD) {
                // we can ignore reads from global constants.
                if (expression.symbol.owner.isFinal && expression.symbol.owner.parent is IrFile) {
                    return
                }
            }
            // we can ignore reads from the instance field since there is only one write (and it doesn't have any reads before it).
            if (data.origin == OBJECT_GET_INSTANCE_FUNCTION) {
                if (expression.symbol.owner.origin == FIELD_FOR_OBJECT_INSTANCE) {
                    return
                }
            }
            data.nonNullEffects.setAtLeast(EffectsKind.READ)
        }

        override fun visitSetField(expression: IrSetField, data: IrFunction) {
            val constructedClass = data.getConstructedClass()
            if (constructedClass != null) {
                // we ignore writes to "this" in constructors because there are never any reads before them.
                val receiver = expression.receiver
                if (receiver is IrGetValue) {
                    if (receiver.symbol == constructedClass.thisReceiver!!.symbol) {
                        expression.value.accept(this, data)
                        return
                    }
                    if (receiver.symbol.owner.origin == ES6_DELEGATING_CONSTRUCTOR_CALL_REPLACEMENT) {
                        expression.value.accept(this, data)
                        return
                    }
                }
            }
            // we ignore writes to the object instance field because there are never any reads before them.
            if (expression.symbol.owner.origin == FIELD_FOR_OBJECT_INSTANCE) {
                return
            }
            data.nonNullEffects.setAtLeast(EffectsKind.WRITE)
        }

        override fun visitSetValue(expression: IrSetValue, data: IrFunction) {
            if (expression.symbol.owner.parent != data) {
                data.nonNullEffects.setAtLeast(EffectsKind.WRITE)
                return
            }
            super.visitSetValue(expression, data)
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrFunction) {
            val called = expression.symbol.owner
            if (!called.isFinal || called.isExternal) {
                data.nonNullEffects.setAtLeast(EffectsKind.WRITE)
                return
            }
            // exception for Unit_getInstance?
            if (called.origin == OBJECT_GET_INSTANCE_FUNCTION && called.returnType.isUnit()) {
                return
            }
            super.visitFunctionAccess(expression, data)
            data.nonNullEffects.dependOn(maybeVisit(called))
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
