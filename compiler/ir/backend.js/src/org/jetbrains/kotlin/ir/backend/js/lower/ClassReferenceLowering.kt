/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.toJsArrayLiteral
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isThrowable
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*

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
            IrType::isThrowable to "throwableClass",
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

    private fun callGetKClass(
        returnType: IrType = intrinsics.jsGetKClass.owner.returnType,
        typeArgument: IrType
    ): IrCall {
        val primitiveKClass =
            getFinalPrimitiveKClass(returnType, typeArgument) ?: getOpenPrimitiveKClass(returnType, typeArgument)

        if (primitiveKClass != null)
            return primitiveKClass

        return JsIrBuilder.buildCall(intrinsics.jsGetKClass, returnType, listOf(typeArgument))
            .apply {
                putValueArgument(0, callJsClass(typeArgument))
            }
    }

    private fun callJsClass(type: IrType) =
        JsIrBuilder.buildCall(intrinsics.jsClass, typeArguments = listOf(type))

    private fun buildCall(name: IrSimpleFunctionSymbol, vararg args: IrExpression): IrExpression =
        JsIrBuilder.buildCall(name).apply {
            args.forEachIndexed { index, irExpression ->
                putValueArgument(index, irExpression)
            }
        }

    fun createKType(type: IrType): IrExpression {
        if (type is IrSimpleType)
            return createSimpleKType(type)
        if (type is IrDynamicType)
            return createDynamicType()
        error("Unexpected type $type")
    }

    private fun createDynamicType(): IrExpression {
        return buildCall(context.intrinsics.createDynamicKType!!)
    }

    private fun createSimpleKType(type: IrSimpleType): IrExpression {
        val classifier: IrClassifierSymbol = type.classifier

        if (classifier is IrTypeParameterSymbol && classifier.owner.isReified) {
            error("Fail")
        }

        val kClassifier = createKClassifier(classifier)
        // TODO: Use static array types
        val arguments = type.arguments.map { createKTypeProjection(it) }.toJsArrayLiteral(
            context,
            context.dynamicType,
            context.dynamicType
        )
        val isMarkedNullable = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, type.isMarkedNullable())
        return buildCall(
            context.intrinsics.createKType!!,
            kClassifier,
            arguments,
            isMarkedNullable
        )
    }

    private fun createKTypeProjection(tp: IrTypeArgument): IrExpression {
        if (tp !is IrTypeProjection) {
            return buildCall(context.intrinsics.getStarKTypeProjection!!)
        }

        val factoryName = when (tp.variance) {
            Variance.INVARIANT -> context.intrinsics.createInvariantKTypeProjection!!
            Variance.IN_VARIANCE -> context.intrinsics.createContravariantKTypeProjection!!
            Variance.OUT_VARIANCE -> context.intrinsics.createCovariantKTypeProjection!!
        }

        val kType = createKType(tp.type)
        return buildCall(factoryName, kType)

    }

    private fun createKClassifier(classifier: IrClassifierSymbol): IrExpression =
        when (classifier) {
            is IrTypeParameterSymbol -> createKTypeParameter(classifier.owner)
            else -> callGetKClass(typeArgument = classifier.defaultType)
        }

    private fun createKTypeParameter(typeParameter: IrTypeParameter): IrExpression {
        val name = JsIrBuilder.buildString(context.irBuiltIns.stringType, typeParameter.name.asString())
        val upperBounds = typeParameter.superTypes.map { createKType(it) }.toJsArrayLiteral(
            context,
            context.dynamicType,
            context.dynamicType
        )

        val variance = when (typeParameter.variance) {
            Variance.INVARIANT -> JsIrBuilder.buildString(context.irBuiltIns.stringType, "invariant")
            Variance.IN_VARIANCE -> JsIrBuilder.buildString(context.irBuiltIns.stringType, "in")
            Variance.OUT_VARIANCE -> JsIrBuilder.buildString(context.irBuiltIns.stringType, "out")
        }
        if (typeParameter.isReified) {
            error("Reified parameter")
        }

        return buildCall(
            context.intrinsics.createKTypeParameter!!,
            name,
            upperBounds,
            variance
        )
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
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

            override fun visitCall(expression: IrCall): IrExpression =
                if (Symbols.isTypeOfIntrinsic(expression.symbol)) {
                    createKType(expression.getTypeArgument(0)!!)
                } else {
                    super.visitCall(expression)
                }
        })
    }
}

