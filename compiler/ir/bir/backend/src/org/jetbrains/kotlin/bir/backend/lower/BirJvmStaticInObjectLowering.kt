/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.acceptChildren
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.jvm.JvmCachedDeclarations
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirGetValue
import org.jetbrains.kotlin.bir.expressions.BirMemberAccessExpression
import org.jetbrains.kotlin.bir.expressions.impl.BirBlockImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirGetFieldImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirTypeOperatorCallImpl
import org.jetbrains.kotlin.bir.replaceWith
import org.jetbrains.kotlin.bir.util.defaultType
import org.jetbrains.kotlin.bir.util.hasAnnotation
import org.jetbrains.kotlin.bir.util.isTrivial
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.name.Name

context(JvmBirBackendContext)
class BirJvmStaticInObjectLowering : BirLoweringPhase() {
    private val JvmStaticAnnotation = birBuiltIns.findClass(Name.identifier("JvmStatic"), "kotlin", "jvm")!!

    private val functionsWithStaticAnnotationKey = registerIndexKey<BirSimpleFunction>(false) {
        it.isJvmStaticDeclaration()
    }
    private val callsToJvmStaticObjects = registerIndexKey<BirMemberAccessExpression<*>>(false) { call ->
        (call.symbol as? BirDeclaration)?.isJvmStaticDeclaration() == true
    }

    private val fieldForObjectInstanceToken = acquireProperty(JvmCachedDeclarations.FieldForObjectInstance)
    private val interfaceCompanionFieldDeclaration = acquireProperty(JvmCachedDeclarations.InterfaceCompanionFieldDeclaration)

    override fun invoke(module: BirModuleFragment) {
        compiledBir.getElementsWithIndex(functionsWithStaticAnnotationKey).forEach { function ->
            val parent = (function.correspondingPropertySymbol?.owner as? BirProperty)?.parent
                ?: function.parent as? BirClass
            if (parent is BirClass && parent.kind == ClassKind.OBJECT && !parent.isCompanion) {
                function.dispatchReceiverParameter?.let { oldDispatchReceiverParameter ->
                    function.dispatchReceiverParameter = null
                    function.replaceThisByStaticReference(parent, oldDispatchReceiverParameter)
                }
            }
        }

        compiledBir.getElementsWithIndex(callsToJvmStaticObjects).forEach { call ->
            val callee = call.symbol.owner
            if (callee.parent.let { it is BirClass && it.kind == ClassKind.OBJECT && !it.isCompanion }) {
                call.replaceWithStatic(replaceCallee = null)
            }
        }
    }

    private fun BirDeclaration.isJvmStaticDeclaration(): Boolean =
        hasAnnotation(JvmStaticAnnotation) ||
                (this as? BirSimpleFunction)?.correspondingPropertySymbol?.owner?.hasAnnotation(JvmStaticAnnotation) == true ||
                (this as? BirProperty)?.getter?.hasAnnotation(JvmStaticAnnotation) == true

    private fun BirMemberAccessExpression<*>.replaceWithStatic(replaceCallee: BirSimpleFunction?): BirExpression {
        val receiver = dispatchReceiver ?: return this
        dispatchReceiver = null
        if (replaceCallee != null) {
            (this as BirCall).symbol = replaceCallee
        }
        if (receiver.isTrivial()) {
            // Receiver has no side effects (aside from maybe class initialization) so discard it.
            return this
        }

        val block = BirBlockImpl(sourceSpan, type, null)
        replaceWith(block)
        block.apply {
            statements += receiver.coerceToUnit() // evaluate for side effects
            statements += this@replaceWithStatic
        }

        return block
    }

    private fun BirExpression.coerceToUnit() =
        BirTypeOperatorCallImpl(sourceSpan, birBuiltIns.unitType, IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, this, birBuiltIns.unitType)

    private fun BirElement.replaceThisByStaticReference(
        irClass: BirClass,
        oldThisReceiverParameter: BirValueParameter,
    ) {
        acceptChildren { element ->
            if (element is BirGetValue && element.symbol == oldThisReceiverParameter) {
                element.replaceWith(
                    BirGetFieldImpl(
                        element.sourceSpan,
                        irClass.defaultType,
                        JvmCachedDeclarations.getPrivateFieldForObjectInstance(
                            irClass,
                            interfaceCompanionFieldDeclaration,
                            fieldForObjectInstanceToken
                        ),
                        null,
                        null,
                        null,
                    )
                )
            } else {
                element.walkIntoChildren()
            }
        }
    }
}


