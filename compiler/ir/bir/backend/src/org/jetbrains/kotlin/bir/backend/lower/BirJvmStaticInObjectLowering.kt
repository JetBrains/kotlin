/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

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

import org.jetbrains.kotlin.bir.util.defaultType
import org.jetbrains.kotlin.bir.util.hasAnnotation
import org.jetbrains.kotlin.bir.util.isTrivial
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME

context(JvmBirBackendContext)
class BirJvmStaticInObjectLowering : BirLoweringPhase() {
    private val JvmStaticAnnotation by lz { birBuiltIns.findClass(JVM_STATIC_ANNOTATION_FQ_NAME) }

    private val functionsWithStaticAnnotationKey = registerIndexKey<BirSimpleFunction>(false) {
        it.isJvmStaticDeclaration()
    }
    private val callsToJvmStaticObjects = registerIndexKey<BirMemberAccessExpression<*>>(false) { call ->
        (call.symbol as? BirDeclaration)?.isJvmStaticDeclaration() == true
    }
    private val valueReads = registerBackReferencesKey<BirGetValue> { recordReference(it.symbol.owner) }

    private val fieldForObjectInstanceToken = acquireProperty(JvmCachedDeclarations.FieldForObjectInstance)
    private val interfaceCompanionFieldDeclaration = acquireProperty(JvmCachedDeclarations.InterfaceCompanionFieldDeclaration)
    private val fieldForObjectInstanceParentToken = acquireProperty(JvmCachedDeclarations.FieldForObjectInstanceParent)

    override fun invoke(module: BirModuleFragment) {
        compiledBir.getElementsWithIndex(functionsWithStaticAnnotationKey).forEach { function ->
            val parent = function.correspondingPropertySymbol?.owner?.parent
                ?: function.parent as? BirClass
            if (parent is BirClass && parent.kind == ClassKind.OBJECT && !parent.isCompanion) {
                function.dispatchReceiverParameter?.let { oldDispatchReceiverParameter ->
                    replaceThisByStaticReference(parent, oldDispatchReceiverParameter)
                    function.dispatchReceiverParameter = null
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

    private fun BirDeclaration.isJvmStaticDeclaration(): Boolean = JvmStaticAnnotation?.let { annotation ->
        hasAnnotation(annotation) ||
                (this as? BirSimpleFunction)?.correspondingPropertySymbol?.owner?.hasAnnotation(annotation) == true ||
                (this as? BirProperty)?.getter?.hasAnnotation(annotation) == true
    } == true

    private fun BirMemberAccessExpression<*>.replaceWithStatic(replaceCallee: BirSimpleFunction?) {
        val receiver = dispatchReceiver ?: return
        dispatchReceiver = null
        if (replaceCallee != null) {
            (this as BirCall).symbol = replaceCallee
        }
        if (receiver.isTrivial()) {
            // Receiver has no side effects (aside from maybe class initialization) so discard it.
            return
        }

        val block = BirBlockImpl(sourceSpan, type, null)
        replaceWith(block)
        block.statements += receiver.coerceToUnit() // evaluate for side effects
        block.statements += this@replaceWithStatic
    }

    private fun BirExpression.coerceToUnit() =
        BirTypeOperatorCallImpl(sourceSpan, birBuiltIns.unitType, IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, this, birBuiltIns.unitType)

    private fun replaceThisByStaticReference(
        birClass: BirClass,
        oldThisReceiverParameter: BirValueParameter,
    ) {
        val field = JvmCachedDeclarations.getPrivateFieldForObjectInstance(
            birClass,
            interfaceCompanionFieldDeclaration,
            fieldForObjectInstanceToken,
            fieldForObjectInstanceParentToken,
        )
        oldThisReceiverParameter.getBackReferences(valueReads).forEach { getValue ->
            val new = BirGetFieldImpl(getValue.sourceSpan, birClass.defaultType, field, null, null, null)
            getValue.replaceWith(new)
        }
    }
}


