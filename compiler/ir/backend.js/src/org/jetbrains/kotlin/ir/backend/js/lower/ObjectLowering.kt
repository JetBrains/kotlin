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
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ObjectDeclarationLowering(
    val context: JsCommonBackendContext
) : DeclarationTransformer {

    private var IrClass.instanceField by context.mapping.objectToInstanceField
    private var IrClass.syntheticPrimaryConstructor by context.mapping.classToSyntheticPrimaryConstructor

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrClass || declaration.kind != ClassKind.OBJECT || declaration.isEffectivelyExternal())
            return null

        val getInstanceFun = context.getOrCreateGetInstanceFunction(declaration)

        val instanceField = buildField {
            name = Name.identifier(declaration.name.asString() + "_instance")
            type = declaration.defaultType.makeNullable()
            isStatic = true
        }.apply {
            parent = declaration.parent
            initializer = null  // Initialized with 'undefined'
        }

        declaration.instanceField = instanceField

        val primaryConstructor = declaration.primaryConstructor ?: declaration.syntheticPrimaryConstructor!!

        getInstanceFun.body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements += context.createIrBuilder(getInstanceFun.symbol).irBlockBody(getInstanceFun) {
                +irIfThen(
                    irEqualsNull(irGetField(null, instanceField)),
                    // Instance field initialized inside constructor
                    irCallConstructor(primaryConstructor.symbol, emptyList())
                )
                +irReturn(irGetField(null, instanceField))
            }.statements
        }

        return listOf(declaration, instanceField, getInstanceFun)
    }
}

class ObjectUsageLowering(
    val context: JsCommonBackendContext
) : BodyLoweringPass {

    private var IrClass.instanceField by context.mapping.objectToInstanceField

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrConstructor && container.isPrimary) {
            val irClass = container.parentAsClass
            irClass.instanceField?.let { instanceField ->
                // Initialize instance field in the beginning of the constructor because it can be used inside the constructor later
                val initInstanceField = context.createIrBuilder(container.symbol).buildStatement(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                    irSetField(null, instanceField, irGet(irClass.thisReceiver!!))
                }
                (irBody as IrBlockBody).statements.add(0, initInstanceField)
            }
        }

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val obj: IrClass = expression.symbol.owner
                if (obj.isEffectivelyExternal()) return expression
                return JsIrBuilder.buildCall(context.getOrCreateGetInstanceFunction(obj).symbol)
            }
        })
    }
}

fun JsCommonBackendContext.getOrCreateGetInstanceFunction(obj: IrClass) =
    mapping.objectToGetInstanceFunction.getOrPut(obj) {
        JsIrBuilder.buildFunction(
            obj.name.asString() + "_getInstance",
            returnType = obj.defaultType,
            parent = obj.parent
        )
    }