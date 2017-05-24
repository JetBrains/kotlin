/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.isRepresentedAs
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

internal class InteropLoweringPart1(val context: Context) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        return when (expression.descriptor.original) {
            context.interopBuiltIns.typeOf -> {
                val typeArgument = expression.getSingleTypeArgument()
                val classDescriptor = TypeUtils.getClassDescriptor(typeArgument)

                if (classDescriptor == null) {
                    expression
                } else {
                    val companionObjectDescriptor = classDescriptor.companionObjectDescriptor ?:
                            error("native variable class $classDescriptor must have the companion object")

                    IrGetObjectValueImpl(
                            expression.startOffset, expression.endOffset,
                            companionObjectDescriptor.defaultType, companionObjectDescriptor
                    )
                }
            }
            else -> expression
        }
    }
}

/**
 * Lowers some interop intrinsic calls.
 */
internal class InteropLoweringPart2(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = InteropTransformer(context, irFile)
        irFile.transformChildrenVoid(transformer)
    }
}

private class InteropTransformer(val context: Context, val irFile: IrFile) : IrBuildingTransformer(context) {

    val interop = context.interopBuiltIns
    val symbols = context.ir.symbols

    override fun visitCall(expression: IrCall): IrExpression {

        expression.transformChildrenVoid(this)
        builder.at(expression)
        val descriptor = expression.descriptor.original

        if (descriptor is ClassConstructorDescriptor) {
            val type = descriptor.constructedClass.defaultType
            if (type.isRepresentedAs(ValueType.C_POINTER) || type.isRepresentedAs(ValueType.NATIVE_POINTED)) {
                throw Error("Native interop types constructors must not be called directly")
            }
        }

        if (descriptor == interop.nativePointedRawPtrGetter ||
                OverridingUtil.overrides(descriptor, interop.nativePointedRawPtrGetter)) {

            // Replace by the intrinsic call to be handled by code generator:
            return builder.irCall(symbols.interopNativePointedGetRawPointer).apply {
                extensionReceiver = expression.dispatchReceiver
            }
        }

        return when (descriptor) {
            interop.cPointerRawValue.getter ->
                // Replace by the intrinsic call to be handled by code generator:
                builder.irCall(symbols.interopCPointerGetRawValue).apply {
                    extensionReceiver = expression.dispatchReceiver
                }

            interop.bitsToFloat -> {
                val argument = expression.getValueArgument(0)
                if (argument is IrConst<*> && argument.kind == IrConstKind.Int) {
                    val floatValue = kotlinx.cinterop.bitsToFloat(argument.value as Int)
                    builder.irFloat(floatValue)
                } else {
                    expression
                }
            }

            interop.bitsToDouble -> {
                val argument = expression.getValueArgument(0)
                if (argument is IrConst<*> && argument.kind == IrConstKind.Long) {
                    val doubleValue = kotlinx.cinterop.bitsToDouble(argument.value as Long)
                    builder.irDouble(doubleValue)
                } else {
                    expression
                }
            }

            in interop.staticCFunction -> {
                val irCallableReference = unwrapStaticFunctionArgument(expression.getValueArgument(0)!!)

                if (irCallableReference == null || irCallableReference.getArguments().isNotEmpty()) {
                    context.reportCompilationError(
                            "${descriptor.fqNameSafe} must take an unbound, non-capturing function or lambda",
                            irFile, expression
                    )
                    // TODO: should probably be reported during analysis.
                }

                val targetSymbol = irCallableReference.symbol
                val target = targetSymbol.descriptor
                val signatureTypes = target.allParameters.map { it.type } + target.returnType!!

                signatureTypes.forEachIndexed { index, type ->
                    type.ensureSupportedInCallbacks(
                            isReturnType = (index == signatureTypes.lastIndex),
                            reportError = { context.reportCompilationError(it, irFile, expression) }
                    )
                }

                descriptor.typeParameters.forEachIndexed { index, typeParameterDescriptor ->
                    val typeArgument = expression.getTypeArgument(typeParameterDescriptor)!!
                    val signatureType = signatureTypes[index]
                    if (typeArgument != signatureType) {
                        context.reportCompilationError(
                                "C function signature element mismatch: expected '$signatureType', got '$typeArgument'",
                                irFile, expression
                        )
                    }
                }

                IrFunctionReferenceImpl(
                        builder.startOffset, builder.endOffset,
                        expression.type,
                        targetSymbol, target,
                        typeArguments = null)
            }

            interop.signExtend, interop.narrow -> {

                val integerTypePredicates = arrayOf(
                        KotlinBuiltIns::isByte, KotlinBuiltIns::isShort, KotlinBuiltIns::isInt, KotlinBuiltIns::isLong
                )

                val receiver = expression.extensionReceiver!!
                val typeOperand = expression.getSingleTypeArgument()

                val receiverTypeIndex = integerTypePredicates.indexOfFirst { it(receiver.type) }
                val typeOperandIndex = integerTypePredicates.indexOfFirst { it(typeOperand) }

                if (receiverTypeIndex == -1) {
                    context.reportCompilationError("Receiver's type ${receiver.type} is not an integer type",
                            irFile, receiver)
                }

                if (typeOperandIndex == -1) {
                    context.reportCompilationError("Type argument $typeOperand is not an integer type",
                            irFile, expression)
                }

                when (descriptor) {
                    interop.signExtend -> if (receiverTypeIndex > typeOperandIndex) {
                        context.reportCompilationError("unable to sign extend ${receiver.type} to $typeOperand",
                                irFile, expression)
                    }

                    interop.narrow -> if (receiverTypeIndex < typeOperandIndex) {
                        context.reportCompilationError("unable to narrow ${receiver.type} to $typeOperand",
                                irFile, expression)
                    }

                    else -> throw Error()
                }

                val receiverClass = symbols.integerClasses.single {
                    receiver.type.isSubtypeOf(it.owner.defaultType)
                }
                val conversionSymbol = receiverClass.functions.single {
                    it.descriptor.name == Name.identifier("to$typeOperand")
                }

                builder.irCall(conversionSymbol).apply {
                    dispatchReceiver = receiver
                }
            }

            else -> expression
        }
    }

    private fun KotlinType.ensureSupportedInCallbacks(isReturnType: Boolean, reportError: (String) -> Nothing) {
        if (isReturnType && KotlinBuiltIns.isUnit(this)) {
            return
        }

        if (KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(this)) {
            if (!this.isMarkedNullable) {
                return
            }
            reportError("Type $this must not be nullable when used in callback signature")
        }

        if (TypeUtils.getClassDescriptor(this) == interop.cPointer) {
            if (this.isMarkedNullable) {
                return
            }

            reportError("Type $this must be nullable when used in callback signature")
        }

        reportError("Type $this is not supported in callback signature")
    }

    private fun unwrapStaticFunctionArgument(argument: IrExpression): IrFunctionReference? {
        if (argument is IrFunctionReference) {
            return argument
        }

        // Otherwise check whether it is a lambda:

        // 1. It is a container with two statements and expected origin:

        if (argument !is IrContainerExpression || argument.statements.size != 2) {
            return null
        }
        if (argument.origin != IrStatementOrigin.LAMBDA && argument.origin != IrStatementOrigin.ANONYMOUS_FUNCTION) {
            return null
        }

        // 2. First statement is an empty container (created during local functions lowering):

        val firstStatement = argument.statements.first()

        if (firstStatement !is IrContainerExpression || firstStatement.statements.size != 0) {
            return null
        }

        // 3. Second statement is IrCallableReference:

        return argument.statements.last() as? IrFunctionReference
    }
}

private fun IrCall.getSingleTypeArgument(): KotlinType {
    val typeParameter = descriptor.original.typeParameters.single()
    return getTypeArgument(typeParameter)!!
}

private fun IrBuilder.irFloat(value: Float) =
        IrConstImpl.float(startOffset, endOffset, context.builtIns.floatType, value)

private fun IrBuilder.irDouble(value: Double) =
        IrConstImpl.double(startOffset, endOffset, context.builtIns.doubleType, value)