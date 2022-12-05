/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.MutableReference
import org.jetbrains.kotlin.ir.backend.js.utils.mutableReferenceOf
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid


/**
 * When I don't want to optimize class hierarchy chain if there is a class inside which:
 * 1. Has primary constructor which delegates to a secondary
 * 2. Has secondary constructor which delegates to a primary
 * 3. Has a constructor with a box parameter, and it has an external superclass
 * 4. Is a subtype for Throwable, because we replace the super call inside constructors with `setPropertiesToThrowableInstance` call
 * Otherwise, we can generate a simple ES-class constructor in each class of the hierarchy
 */
class ES6ConstructorOptimizationLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val esClassWhichNeedBoxParameters = context.mapping.esClassWhichNeedBoxParameters
    private val esClassToPossibilityForOptimization = context.mapping.esClassToPossibilityForOptimization

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (
            context.es6mode &&
            !context.incrementalCacheEnabled &&
            declaration is IrClass &&
            !declaration.isExternal &&
            !context.inlineClassesUtils.isClassInlineLike(declaration) &&
            !esClassToPossibilityForOptimization.contains(declaration)
        ) {
            declaration.checkIfCanBeOptimized()
        }

        return null
    }

    private fun IrClass.checkIfCanBeOptimized() {
        var currentClass: IrClass? = this
        var nearestOptimizationDecision: MutableReference<Boolean>? = null

        while (currentClass != null && !currentClass.isExternal) {
            val currentClassOptimizationDecision = esClassToPossibilityForOptimization[currentClass]

            if (currentClassOptimizationDecision != null) {
                nearestOptimizationDecision = currentClassOptimizationDecision
                break
            }

            currentClass = currentClass.superClass
        }

        if (nearestOptimizationDecision == null) {
            nearestOptimizationDecision = mutableReferenceOf(true)
        }

        currentClass = this
        while (currentClass != null && !currentClass.isExternal && !esClassToPossibilityForOptimization.contains(currentClass)) {
            esClassToPossibilityForOptimization[currentClass] = nearestOptimizationDecision

            if (nearestOptimizationDecision.value &&  !currentClass.canBeOptimized()) {
                nearestOptimizationDecision.value = false
            }

            currentClass = currentClass.superClass
        }
    }

    private fun IrClass.canBeOptimized(): Boolean {
        return superClass?.symbol != context.throwableClass && !isSubclassOfExternalClassWithRequiredBoxParameter() && !hasPrimaryDelegatedToSecondaryOrSecondaryToPrimary()
    }

    private fun IrClass.hasPrimaryDelegatedToSecondaryOrSecondaryToPrimary(): Boolean {
        constructors.forEach {
            var meetUnoptimizableDelegation = false
            it.body?.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
                    if (expression.symbol.owner.isPrimary != it.isPrimary) {
                        meetUnoptimizableDelegation = true
                    }
                    super.visitDelegatingConstructorCall(expression)
                }
            })

            if (meetUnoptimizableDelegation) return true
        }
        return false
    }

    private fun IrClass.isSubclassOfExternalClassWithRequiredBoxParameter(): Boolean {
        return superClass?.isExternal == true && esClassWhichNeedBoxParameters.contains(this)
    }
}

class ES6CollectConstructorsWhichNeedBoxParameters(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val esClassWhichNeedBoxParameters = context.mapping.esClassWhichNeedBoxParameters

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || context.incrementalCacheEnabled || declaration !is IrClass) return null

        val hasSuperClass = declaration.superClass != null

        if (hasSuperClass && declaration.isInner) {
            declaration.addToClassListWhichNeedBoxParameter()
        }
        if (hasSuperClass && declaration.isLocal && declaration.containsCapturedValues()) {
            declaration.addToClassListWhichNeedBoxParameter()
        }

        return null
    }

    private fun IrClass.containsCapturedValues(): Boolean {
        if (superClass == null) return false
        val thisReceiverSymbol = thisReceiver?.symbol ?: return false

        constructors.forEach {
            var meetCapturing = false
            var meetDelegation = false
            it.body?.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitSetField(expression: IrSetField) {
                    val receiver = expression.receiver as? IrGetValue
                    if (!meetDelegation && receiver?.symbol == thisReceiverSymbol) {
                        meetCapturing = true
                    }
                    super.visitSetField(expression)
                }

                override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
                    meetDelegation = true
                    super.visitDelegatingConstructorCall(expression)
                }
            })

            if (meetCapturing) return true
        }

        return false
    }

    private fun IrClass.addToClassListWhichNeedBoxParameter() {
        if (isExternal) return
        esClassWhichNeedBoxParameters.add(this)
        superClass?.addToClassListWhichNeedBoxParameter()
    }
}