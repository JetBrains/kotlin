/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isThrowable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ClassReferenceLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val intrinsics = context.intrinsics

    private val primitiveClassesObject = context.primitiveClassesObject

    private val primitiveClassProperties = context.primitiveClassProperties

    private fun primitiveClassProperty(name: String) =
        primitiveClassProperties.singleOrNull { it.name == Name.identifier(name) }?.getter
            ?: primitiveClassesObject.owner.declarations.filterIsInstance<IrSimpleFunction>().single { it.name == Name.special("<get-$name>") }

    private val finalPrimitiveClasses by lazy {
        mapOf(
            IrType::isBoolean to "booleanClass",
            IrType::isByte to "byteClass",
            IrType::isShort to "shortClass",
            IrType::isInt to "intClass",
            IrType::isFloat to "floatClass",
            IrType::isDouble to "doubleClass",
            IrType::isArray to "arrayClass",
            IrType::isString to "stringClass",
            IrType::isThrowable to "throwableClass",
            IrType::isBooleanArray to "booleanArrayClass",
            IrType::isCharArray to "charArrayClass",
            IrType::isByteArray to "byteArrayClass",
            IrType::isShortArray to "shortArrayClass",
            IrType::isIntArray to "intArrayClass",
            IrType::isLongArray to "longArrayClass",
            IrType::isFloatArray to "floatArrayClass",
            IrType::isDoubleArray to "doubleArrayClass"
        ).mapValues {
            primitiveClassProperty(it.value)
        }
    }

    private val openPrimitiveClasses by lazy {
        mapOf(
            IrType::isAny to "anyClass",
            IrType::isNumber to "numberClass",
            IrType::isNothing to "nothingClass"
        ).mapValues {
            primitiveClassProperty(it.value)
        }
    }

    private fun callGetKClassFromExpression(returnType: IrType, typeArgument: IrType, argument: IrExpression): IrExpression {
        val primitiveKClass = getFinalPrimitiveKClass(returnType, typeArgument)
        if (primitiveKClass != null)
            return JsIrBuilder.buildBlock(returnType, listOf(argument, primitiveKClass))

        return JsIrBuilder.buildCall(intrinsics.jsGetKClassFromExpression, returnType, listOf(typeArgument)).apply {
            putValueArgument(0, argument)
        }
    }

    private fun getPrimitiveClass(target: IrSimpleFunction, returnType: IrType) =
        JsIrBuilder.buildCall(target.symbol, returnType).apply {
            dispatchReceiver = JsIrBuilder.buildGetObjectValue(primitiveClassesObject.defaultType, primitiveClassesObject)
        }

    private fun getFinalPrimitiveKClass(returnType: IrType, typeArgument: IrType): IrCall? {
        for ((typePredicate, v) in finalPrimitiveClasses) {
            if (typePredicate(typeArgument))
                return getPrimitiveClass(v, returnType)
        }

        return null
    }


    private fun getOpenPrimitiveKClass(returnType: IrType, typeArgument: IrType): IrCall? {
        for ((typePredicate, v) in openPrimitiveClasses) {
            if (typePredicate(typeArgument))
                return getPrimitiveClass(v, returnType)
        }

        if (typeArgument.isFunction()) {
            val functionInterface = typeArgument.getClass()!!
            val arity = functionInterface.typeParameters.size - 1
            return getPrimitiveClass(context.primitiveClassFunctionClass, returnType).apply {
                putValueArgument(0, JsIrBuilder.buildInt(context.irBuiltIns.intType, arity))
            }
        }

        return null
    }

    private fun callGetKClass(returnType: IrType, typeArgument: IrType): IrCall {
        val primitiveKClass =
            getFinalPrimitiveKClass(returnType, typeArgument) ?: getOpenPrimitiveKClass(returnType, typeArgument)

        if (primitiveKClass != null)
            return primitiveKClass

        return JsIrBuilder.buildCall(intrinsics.jsGetKClass, returnType, listOf(typeArgument)).apply {
            putValueArgument(0, callJsClass(typeArgument))
        }
    }

    private fun callJsClass(type: IrType) =
        JsIrBuilder.buildCall(intrinsics.jsClass, typeArguments = listOf(type))

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetClass(expression: IrGetClass) =
                callGetKClassFromExpression(
                    returnType = expression.type,
                    typeArgument = expression.argument.type,
                    argument = expression.argument.transform(this, null)
                )

            override fun visitClassReference(expression: IrClassReference) =
                callGetKClass(
                    returnType = expression.type,
                    typeArgument = expression.classType.makeNotNull()
                )
        })
    }
}

