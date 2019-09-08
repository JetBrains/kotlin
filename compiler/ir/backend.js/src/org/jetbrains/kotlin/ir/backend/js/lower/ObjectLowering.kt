/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ObjectDeclarationLowering(
    val context: CommonBackendContext,
    val objectToGetInstanceFunction: MutableMap<IrClassSymbol, IrSimpleFunction>
) : DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.transformDeclarationsFlat { declaration ->
            if (declaration !is IrClass || declaration.kind != ClassKind.OBJECT || declaration.isEffectivelyExternal())
                return@transformDeclarationsFlat null

            val getInstanceFun = getOrCreateGetInstanceFunction(objectToGetInstanceFunction, declaration)

            val instanceField = buildField {
                name = Name.identifier(declaration.name.asString() + "_instance")
                type = declaration.defaultType.makeNullable()
                isStatic = true
            }.apply {
                parent = declaration.parent
                initializer = null  // Initialized with 'undefined'
            }
            val primaryConstructor = declaration.primaryConstructor!!
            val body = primaryConstructor.body as IrBlockBody

            // Initialize instance field in the beginning of the constructor because it can be used inside the constructor later
            val initInstanceField = context.createIrBuilder(primaryConstructor.symbol).buildStatement(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                irSetField(null, instanceField, irGet(declaration.thisReceiver!!))
            }
            body.statements.add(0, initInstanceField)

            getInstanceFun.body = context.createIrBuilder(getInstanceFun.symbol).irBlockBody(getInstanceFun) {
                +irIfThen(
                    irEqualsNull(irGetField(null, instanceField)),
                    // Instance field initialized inside constructor
                    irCallConstructor(primaryConstructor.symbol, emptyList())
                )
                +irReturn(irGetField(null, instanceField))
            }

            listOf(declaration, instanceField, getInstanceFun)
        }
    }
}

class ObjectUsageLowering(
    val context: CommonBackendContext,
    val objectToGetInstanceFunction: MutableMap<IrClassSymbol, IrSimpleFunction>
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val obj: IrClass = expression.symbol.owner
                if (obj.isEffectivelyExternal()) return expression
                return JsIrBuilder.buildCall(getOrCreateGetInstanceFunction(objectToGetInstanceFunction, obj).symbol)
            }
        })
    }
}

fun getOrCreateGetInstanceFunction(objectToGetInstanceFunction: MutableMap<IrClassSymbol, IrSimpleFunction>, obj: IrClass) =
    objectToGetInstanceFunction.getOrPut(obj.symbol) {
        JsIrBuilder.buildFunction(
            obj.name.asString() + "_getInstance",
            returnType = obj.defaultType,
            parent = obj.parent
        )
    }