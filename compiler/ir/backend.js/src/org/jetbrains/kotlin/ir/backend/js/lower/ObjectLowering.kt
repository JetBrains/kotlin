/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.objectInstanceField
import org.jetbrains.kotlin.ir.backend.js.syntheticPrimaryConstructor
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Creates lazy object instance generator functions.
 *
 * @param initializeParentCompanions When true, companion objects will initialize their enclosing
 * class's superclass companion first, so the initialization order matches the JVM (parent companion
 * before child companion). JS leaves this false to preserve existing behavior.
 * But see KT-40768 and KT-86422.
 */
class ObjectDeclarationLowering(
    val context: JsCommonBackendContext,
    private val initializeParentCompanions: Boolean = false
) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrClass || declaration.kind != ClassKind.OBJECT || declaration.isEffectivelyExternal())
            return null

        val getInstanceFun = getOrCreateGetInstanceFunction(declaration)

        val instanceField = context.irFactory.buildField {
            name = Name.identifier(declaration.name.asString() + "_instance")
            type = declaration.defaultType.makeNullable()
            isStatic = true
            origin = IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
        }.apply {
            parent = declaration.parent
            initializer = null  // Initialized with 'undefined'
        }

        declaration.objectInstanceField = instanceField

        val primaryConstructor = declaration.primaryConstructor ?: declaration.syntheticPrimaryConstructor!!

        // When initializeParentCompanions is enabled, a companion object's getInstance() will first
        // ensure the enclosing class's superclass companion is initialized. This matches the JVM
        // class-initialization protocol where a superclass is always initialized before its subclass.
        // We walk up the superclass chain to find the nearest ancestor that has a companion, because
        // intermediate classes without companions must not block the chain.
        val parentCompanionGetInstanceFun = if (initializeParentCompanions && declaration.isCompanion) {
            var superClass = declaration.parent.safeAs<IrClass>()?.superClass
            var result: IrSimpleFunction? = null
            while (superClass != null && result == null) {
                val companion = superClass.companionObject()
                if (companion != null) {
                    result = declaration.factory.stageController.restrictTo(companion) {
                        getOrCreateGetInstanceFunction(companion)
                    }
                }
                superClass = superClass.superClass
            }
            result
        } else null

        getInstanceFun.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements += context.createIrBuilder(getInstanceFun.symbol).irBlockBody(getInstanceFun) {
                val thenPart: IrExpression = if (parentCompanionGetInstanceFun != null) {
                    irBlock {
                        +irCall(parentCompanionGetInstanceFun.symbol)
                        +irCallConstructor(primaryConstructor.symbol, emptyList())
                    }
                } else {
                    irCallConstructor(primaryConstructor.symbol, emptyList())
                }
                +irIfThen(
                    irNullabilityCheck(instanceField),
                    // Instance field initialized inside constructor
                    thenPart
                )
                +irReturn(irGetField(null, instanceField))
            }.statements
        }

        return listOf(declaration, instanceField, getInstanceFun)
    }

    private fun IrBuilderWithScope.irNullabilityCheck(instanceField: IrField): IrExpression {
        val context = this@ObjectDeclarationLowering.context
        return if (context is JsIrBackendContext && context.es6mode) {
            irEqeqeqWithoutBox(irGetField(null, instanceField), context.getVoid())
        } else {
            irEqualsNull(irGetField(null, instanceField))
        }
    }
}

/**
 * Transforms [IrGetObjectValue] into an instance generator call.
 */
@PhasePrerequisites(PrimaryConstructorLowering::class)
class ObjectUsageLowering(val context: JsCommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val functionContainer = container.takeIf { it is IrConstructor && it.isPrimary }
        val irClass = functionContainer?.parentAsClass

        irClass?.objectInstanceField?.let {
            if (context.es6mode && irClass.superClass == null) return@let
            // Initialize instance field in the beginning of the constructor because it can be used inside the constructor later
            val initInstanceField = generateInitInstanceField(it, irClass.getValueForInstanceFieldForTheFirstTime())
            (irBody as IrBlockBody).statements.add(0, initInstanceField)
        }

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val obj: IrClass = expression.symbol.owner
                if (obj.isEffectivelyExternal()) return expression
                return JsIrBuilder.buildCall(getOrCreateGetInstanceFunction(obj).symbol)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                val instanceField = irClass?.objectInstanceField
                return if (!context.es6mode || instanceField == null) {
                    super.visitDelegatingConstructorCall(expression)
                } else {
                    JsIrBuilder.buildComposite(
                        context.irBuiltIns.unitType,
                        listOf(
                            super.visitDelegatingConstructorCall(expression),
                            generateInitInstanceField(instanceField, JsIrBuilder.buildGetValue(irClass.thisReceiver!!.symbol))
                        )
                    )
                }
            }
        })
    }

    private fun IrClass.getValueForInstanceFieldForTheFirstTime(): IrExpression {
        return if (context.es6mode && context is JsIrBackendContext) {
            JsIrBuilder.buildNull(thisReceiver!!.type)
        } else {
            JsIrBuilder.buildGetValue(thisReceiver!!.symbol)
        }
    }

    private fun generateInitInstanceField(instanceField: IrField, value: IrExpression): IrStatement {
        return JsIrBuilder.buildSetField(instanceField.symbol, null, value, context.irBuiltIns.unitType)
    }
}

private fun getOrCreateGetInstanceFunction(obj: IrClass): IrSimpleFunction =
    obj::objectGetInstanceFunction.getOrSetIfNull {
        obj.factory.buildFun {
            name = Name.identifier(obj.name.asString() + "_getInstance")
            returnType = obj.defaultType
            origin = JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION
            visibility = obj.visibility
        }.apply {
            parent = obj.parent
        }
    }
