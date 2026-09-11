/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

/**
 * Creates lazy object instance generator functions.
 */
abstract class ObjectDeclarationLowering<Context : JsCommonBackendContext>(val context: Context) : DeclarationTransformer {
    companion object {
        internal val INSTANCE_FIELD_NAME: Name = Name.identifier("instance")
        internal val GET_INSTANCE_METHOD_NAME: Name = Name.identifier("getInstance")
    }

    protected abstract val initializationGenerator: LazyGlobalInitializationGenerator

    protected abstract fun IrBlockBodyBuilder.generateLazyInitialization(
        instanceField: IrField,
        declaration: IrClass,
        primaryConstructorCall: IrExpression,
    )

    protected open fun instanceFieldType(obj: IrClass) = obj.defaultType.makeNullable()

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrClass || declaration.kind != ClassKind.OBJECT || declaration.isEffectivelyExternal())
            return null

        val getInstanceFun = getOrCreateGetInstanceFunction(declaration)
        val instanceField = getOrCreateInstanceField(declaration)
        declaration.declarations.addAll(0, listOf(instanceField, getInstanceFun))

        val primaryConstructor = declaration.primaryConstructor ?: declaration.syntheticPrimaryConstructor!!

        getInstanceFun.body = context.createIrBuilder(getInstanceFun.symbol).irBlockBody {
            generateLazyInitialization(
                instanceField,
                declaration,
                irCallConstructor(primaryConstructor.symbol, emptyList()).coerceToUnit(context.irBuiltIns),
            )
            +irReturn(irGetField(null, instanceField).implicitCastTo(declaration.defaultType))
        }

        return null
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

private fun getOrCreateInstanceField(obj: IrClass): IrField =
    obj::objectInstanceField.getOrSetIfNull {
        obj.factory.buildField {
            name = ObjectDeclarationLowering.INSTANCE_FIELD_NAME
            type = obj.defaultType.makeNullable()
            isStatic = true
            origin = IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
        }.apply {
            parent = obj
            initializer = null  // Initialized with 'undefined'
        }
    }

private fun getOrCreateGetInstanceFunction(obj: IrClass): IrSimpleFunction =
    obj::objectGetInstanceFunction.getOrSetIfNull {
        // There is need to initialize _instance field together with _getInstance, so the outer restrictTo call would properly assign
        // signature and tags for JS namer. It prevents name clashes during the JS namer phase.
        getOrCreateInstanceField(obj)
        obj.factory.buildFun {
            name = ObjectDeclarationLowering.GET_INSTANCE_METHOD_NAME
            returnType = obj.defaultType
            origin = JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION
            visibility = obj.visibility
        }.apply {
            parent = obj
        }
    }

/**
 * For an object `O`, generates the following:
 *
 * ```kotlin
 * class O {
 *   /*static*/ var instance: Any?
 *   /*static*/ fun getInstance(): O {
 *     val instance = this.instance
 *     if (instance === 2) {
 *       staticInitializationFailureWithClassName(O::class)
 *     }
 *     if (instance == null) {
 *       try {
 *         O() // The `instance` field is initialized inside the object constructor
 *       } catch (reason: dynamic) {
 *         this.instance = 2
 *         kotlin.internal.staticInitializationFailure(reason, VOID)
 *       }
 *     }
 *     return this.instance /* implicitly cast to O */
 *   }
 * }
 * ```
 *
 * Note that the error state is stored in the instance field itself.
 * This is done as a code size optimization and should not affect performance.
 */
class JsObjectDeclarationLowering(context: JsIrBackendContext) : ObjectDeclarationLowering<JsIrBackendContext>(context) {
    override val initializationGenerator = JsLazyGlobalInitializationGenerator(context)

    override fun instanceFieldType(obj: IrClass): IrType = context.irBuiltIns.anyNType

    override fun IrBlockBodyBuilder.generateLazyInitialization(
        instanceField: IrField,
        declaration: IrClass,
        primaryConstructorCall: IrExpression,
    ) {
        with(initializationGenerator) {
            val instanceVar = createTmpVariable(irGetField(null, instanceField), nameHint = "instance")
            +generateErrorStateCheck(instanceVar, declaration)
            +irIfThen(
                irNullabilityCheck(instanceVar),
                // Instance field initialized inside constructor
                generateInitializationExceptionHandling(instanceField, primaryConstructorCall)
            )
        }
    }

    private fun IrBuilderWithScope.irNullabilityCheck(instanceVar: IrVariable): IrExpression {
        val context = this@JsObjectDeclarationLowering.context
        return if (context.es6mode) {
            irEqeqeqWithoutBox(irGet(instanceVar), context.getVoid())
        } else {
            irEqualsNull(irGet(instanceVar))
        }
    }
}
