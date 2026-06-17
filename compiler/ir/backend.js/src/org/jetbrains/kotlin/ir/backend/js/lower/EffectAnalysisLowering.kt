/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.EffectAnalysisClassIds
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

    inner class BodyVisitor : IrVisitor<Unit, EffectsKindCell.Lazy>() {
        fun IrFunction.getConstructedClass(): IrClass? = when {
            this is IrConstructor -> this.constructedClass
            isEs6ConstructorReplacement -> parent as IrClass
            else -> null
        }

        inline fun EffectsKindCell.Lazy.dependOnNew(other: IrElement, with: (EffectsKindCell.Lazy) -> Unit) =
            EffectsKindCell.Lazy(context, function, other).also {
                with(it)
                // this can happen if the lowering is ran multiple times over the same code.
                if (other.effects != null) throw IllegalStateException("element already has effects")
                other.effects = it
                it.freeze()
                dependOn(it)
            }

        fun maybeVisit(owner: IrFunction): EffectsKindCell {
            owner.effects?.let { return it }
            val effectsAnnotation = owner.getAnnotation(EffectAnalysisClassIds.annotation.asSingleFqName())
            if (effectsAnnotation != null) {
                val arg = effectsAnnotation.argumentMapping[EffectAnalysisClassIds.kindParameter]
                if (arg is IrGetEnumValue) {
                    return EffectsKindCell.Exact(EffectsKind.valueOf(arg.symbol.owner.name.asString())).also { owner.effects = it }
                }
            }
            if (owner.isExternal) {
                // add warning if symbol is in stdlib?
                return EffectsKindCell.Exact(EffectsKind.WRITE)
            }
            return EffectsKindCell.Lazy(context, owner, owner).also {
                owner.effects = it
                owner.accept(this, it)
                it.freeze()
            }
        }

        override fun visitElement(element: IrElement, data: EffectsKindCell.Lazy) {
            element.acceptChildren(this, data)
        }

        override fun visitGetValue(expression: IrGetValue, data: EffectsKindCell.Lazy) {
            if (expression.symbol.owner.parent != data.function) {
                data.dependOnNew(expression) { effects ->
                    effects.setAtLeast(EffectsKind.READ)
                }
            }
        }

        override fun visitGetField(expression: IrGetField, data: EffectsKindCell.Lazy) {
            if (expression.symbol.owner.origin == PROPERTY_BACKING_FIELD) {
                // we can ignore reads from global constants here.
                if (expression.symbol.owner.isFinal && expression.symbol.owner.parent is IrFile) {
                    return
                }
            }
            // we can ignore reads from the instance field since there is only one write (and it doesn't have any reads before it).
            if (data.function.origin == OBJECT_GET_INSTANCE_FUNCTION) {
                if (expression.symbol.owner.origin == FIELD_FOR_OBJECT_INSTANCE) {
                    return
                }
            }
            data.dependOnNew(expression) { effects ->
                effects.setAtLeast(EffectsKind.READ)
                super.visitGetField(expression, effects)
            }
        }

        override fun visitSetField(expression: IrSetField, data: EffectsKindCell.Lazy) {
            // we ignore writes to the object instance field because there are never any reads before them.
            if (expression.symbol.owner.origin == FIELD_FOR_OBJECT_INSTANCE) {
                return
            }
            data.dependOnNew(expression) { effects ->
                val constructedClass = effects.function.getConstructedClass()
                if (constructedClass != null) {
                    // we ignore writes to "this" in constructors because there are never any reads before them.
                    val receiver = expression.receiver
                    if (receiver is IrGetValue) {
                        if (receiver.symbol == constructedClass.thisReceiver!!.symbol) {
                            expression.value.accept(this, effects)
                            return@dependOnNew
                        }
                        if (receiver.symbol.owner.origin == ES6_DELEGATING_CONSTRUCTOR_CALL_REPLACEMENT) {
                            expression.value.accept(this, effects)
                            return@dependOnNew
                        }
                    }
                }
                effects.setAtLeast(EffectsKind.WRITE)
                super.visitSetField(expression, effects)
            }
        }

        override fun visitSetValue(expression: IrSetValue, data: EffectsKindCell.Lazy) {
            data.dependOnNew(expression) { effects ->
                if (expression.symbol.owner.parent != effects.function) {
                    effects.setAtLeast(EffectsKind.WRITE)
                }
                super.visitSetValue(expression, effects)
            }
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: EffectsKindCell.Lazy) {
            val called = expression.symbol.owner
            // exception for Unit_getInstance?
            if (called.origin == OBJECT_GET_INSTANCE_FUNCTION && called.returnType.isUnit()) {
                return
            }
            data.dependOnNew(expression) { effects ->
                if (called.isFinal) {
                    effects.dependOn(maybeVisit(called))
                } else {
                    effects.setAtLeast(EffectsKind.WRITE)
                }
                super.visitFunctionAccess(expression, effects)
            }
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
