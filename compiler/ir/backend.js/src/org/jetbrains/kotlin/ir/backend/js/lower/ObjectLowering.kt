/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ObjectDeclarationLowering(val context: JsCommonBackendContext) : DeclarationTransformer {

    private var IrClass.instanceField by context.mapping.objectToInstanceField
    private var IrClass.syntheticPrimaryConstructor by context.mapping.classToSyntheticPrimaryConstructor

    /**
     * If the object being lowered is nested inside an enum class, we want to also initialize the enum entries when initializing the object.
     */
    private var IrClass.initEntryInstancesFun: IrSimpleFunction? by context.mapping.enumClassToInitEntryInstancesFun
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrClass || declaration.kind != ClassKind.OBJECT || declaration.isEffectivelyExternal())
            return null

        val getInstanceFun = context.getOrCreateGetInstanceFunction(declaration)

        val instanceField = context.irFactory.buildField {
            name = Name.identifier(declaration.name.asString() + "_instance")
            type = declaration.defaultType.makeNullable()
            isStatic = true
            origin = IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
        }.apply {
            parent = declaration.parent
            initializer = null  // Initialized with 'undefined'
        }

        declaration.instanceField = instanceField

        val primaryConstructor = declaration.primaryConstructor ?: declaration.syntheticPrimaryConstructor!!

        val initEntryInstancesFun = declaration.parent.safeAs<IrClass>()?.initEntryInstancesFun

        getInstanceFun.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements += context.createIrBuilder(getInstanceFun.symbol).irBlockBody(getInstanceFun) {
                if (initEntryInstancesFun != null)
                    +irCall(initEntryInstancesFun)
                +irIfThen(
                    irNullabilityCheck(instanceField),
                    // Instance field initialized inside constructor
                    irCallConstructor(primaryConstructor.symbol, emptyList())
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

class ObjectUsageLowering(val context: JsCommonBackendContext) : BodyLoweringPass {

    private var IrClass.instanceField by context.mapping.objectToInstanceField

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val functionContainer = container.takeIf { it is IrConstructor && it.isPrimary }
        val irClass = functionContainer?.parentAsClass

        irClass?.instanceField?.let {
            if (context.es6mode && irClass.superClass == null) return@let
            // Initialize instance field in the beginning of the constructor because it can be used inside the constructor later
            val initInstanceField = generateInitInstanceField(it, irClass.getValueForInstanceFieldForTheFirstTime())
            (irBody as IrBlockBody).statements.add(0, initInstanceField)
        }

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val obj: IrClass = expression.symbol.owner
                if (obj.isEffectivelyExternal()) return expression
                return JsIrBuilder.buildCall(context.getOrCreateGetInstanceFunction(obj).symbol)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                val instanceField = irClass?.instanceField
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

private fun JsCommonBackendContext.getOrCreateGetInstanceFunction(obj: IrClass) =
    mapping.objectToGetInstanceFunction.getOrPut(obj) {
        irFactory.buildFun {
            name = Name.identifier(obj.name.asString() + "_getInstance")
            returnType = obj.defaultType
            origin = JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION
            visibility = obj.visibility
        }.apply {
            parent = obj.parent
        }
    }
