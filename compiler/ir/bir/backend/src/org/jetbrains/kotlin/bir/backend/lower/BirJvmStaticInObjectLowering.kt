/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.BirElement
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
import org.jetbrains.kotlin.bir.getBackReferences
import org.jetbrains.kotlin.bir.util.defaultType
import org.jetbrains.kotlin.bir.util.hasAnnotation
import org.jetbrains.kotlin.bir.util.isTrivial
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME

context(JvmBirBackendContext)
class BirJvmStaticInObjectLowering : BirLoweringPhase() {
    private val JvmStaticAnnotation by lz { birBuiltIns.findClass(JVM_STATIC_ANNOTATION_FQ_NAME) }

    private val staticFunctions = registerIndexKey(BirSimpleFunction, true) {
        it.hasJvmStaticAnnotation() || it.correspondingPropertySymbol?.owner?.hasJvmStaticAnnotation() == true
    }
    private val staticProperties = registerIndexKey(BirProperty, true) {
        it.hasJvmStaticAnnotation() || it.getter?.hasJvmStaticAnnotation() == true
    }
    private val memberAccesses = registerBackReferencesKey(BirMemberAccessExpression) { it.symbol.owner }
    private val valueReads = registerBackReferencesKey(BirGetValue) { it.symbol.owner }

    private val fieldForObjectInstanceToken = acquireProperty(JvmCachedDeclarations.FieldForObjectInstance)
    private val interfaceCompanionFieldDeclaration = acquireProperty(JvmCachedDeclarations.InterfaceCompanionFieldDeclaration)
    private val fieldForObjectInstanceParentToken = acquireProperty(JvmCachedDeclarations.FieldForObjectInstanceParent)

    private fun BirDeclaration.hasJvmStaticAnnotation(): Boolean =
        JvmStaticAnnotation?.let { hasAnnotation(it) } == true

    override fun lower(module: BirModuleFragment) {
        getAllElementsWithIndex(staticFunctions).forEach { declaration ->
            val parent = declaration.correspondingPropertySymbol?.owner?.parent
                ?: declaration.parent

            if (parent is BirClass && parent.isSuitableParentOfStaticDeclaration()) {
                replaceReferencesWithStaticAccess(declaration)

                if (declaration.getContainingDatabase() == compiledBir) {
                    declaration.removeStaticDispatchReceiver(parent)
                }
            }
        }

        getAllElementsWithIndex(staticProperties).forEach { declaration ->
            val parent = declaration.parent

            if (parent is BirClass && parent.isSuitableParentOfStaticDeclaration()) {
                replaceReferencesWithStaticAccess(declaration)
            }
        }
    }

    private fun BirClass.isSuitableParentOfStaticDeclaration() =
        kind == ClassKind.OBJECT && !isCompanion

    private fun replaceReferencesWithStaticAccess(declaration: BirDeclaration) {
        declaration.getBackReferences(memberAccesses).forEach { call ->
            call.replaceWithStatic(replaceCallee = null)
        }
    }

    private fun BirSimpleFunction.removeStaticDispatchReceiver(parentObject: BirClass) {
        dispatchReceiverParameter?.let { oldDispatchReceiverParameter ->
            replaceThisByStaticReference(parentObject, oldDispatchReceiverParameter)
            dispatchReceiverParameter = null
        }
    }

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


