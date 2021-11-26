/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.ir.findSuperDeclaration
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

object JvmInvokeDynamic : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        fun fail(message: String): Nothing =
            throw AssertionError("$message; expression:\n${expression.dump()}")

        val dynamicCall = expression.getValueArgument(0) as? IrCall
            ?: fail("'dynamicCall' is expected to be a call")
        val dynamicCallee = dynamicCall.symbol.owner
        if (dynamicCallee.parent != codegen.context.ir.symbols.kotlinJvmInternalInvokeDynamicPackage ||
            dynamicCallee.origin != JvmLoweredDeclarationOrigin.INVOKEDYNAMIC_CALL_TARGET
        )
            fail("Unexpected dynamicCallee: '${dynamicCallee.render()}'")

        val bootstrapMethodHandleArg = expression.getValueArgument(1) as? IrCall
            ?: fail("'bootstrapMethodHandle' should be a call")
        val bootstrapMethodHandle = evalMethodHandle(bootstrapMethodHandleArg)

        val bootstrapMethodArgs = (expression.getValueArgument(2)?.safeAs<IrVararg>()
            ?: fail("'bootstrapMethodArgs' is expected to be a vararg"))
        val asmBootstrapMethodArgs = bootstrapMethodArgs.elements
            .map { generateBootstrapMethodArg(it, codegen) }
            .toTypedArray()

        val dynamicCalleeMethod = codegen.methodSignatureMapper.mapAsmMethod(dynamicCallee)
        val dynamicCallGenerator = IrCallGenerator.DefaultCallGenerator
        val dynamicCalleeArgumentTypes = dynamicCalleeMethod.argumentTypes
        for (i in dynamicCallee.valueParameters.indices) {
            val dynamicCalleeParameter = dynamicCallee.valueParameters[i]
            val dynamicCalleeArgument = dynamicCall.getValueArgument(i)
                ?: fail("No argument #$i in 'dynamicCall'")
            val dynamicCalleeArgumentType = dynamicCalleeArgumentTypes.getOrElse(i) {
                fail("No argument type #$i in dynamic callee: $dynamicCalleeMethod")
            }
            dynamicCallGenerator.genValueAndPut(dynamicCalleeParameter, dynamicCalleeArgument, dynamicCalleeArgumentType, codegen, data)
        }

        codegen.mv.invokedynamic(dynamicCalleeMethod.name, dynamicCalleeMethod.descriptor, bootstrapMethodHandle, asmBootstrapMethodArgs)

        return MaterialValue(codegen, dynamicCalleeMethod.returnType, expression.type)
    }

    private fun generateBootstrapMethodArg(element: IrVarargElement, codegen: ExpressionCodegen): Any =
        when (element) {
            is IrRawFunctionReference ->
                generateMethodHandle(element, codegen)
            is IrCall ->
                evalBootstrapArgumentIntrinsicCall(element, codegen)
                    ?: throw AssertionError("Unexpected callee in bootstrap method argument:\n${element.dump()}")
            is IrConst<*> ->
                when (element.kind) {
                    IrConstKind.Byte -> (element.value as Byte).toInt()
                    IrConstKind.Short -> (element.value as Short).toInt()
                    IrConstKind.Int -> element.value as Int
                    IrConstKind.Long -> element.value as Long
                    IrConstKind.Float -> element.value as Float
                    IrConstKind.Double -> element.value as Double
                    IrConstKind.String -> element.value as String
                    else ->
                        throw AssertionError("Unexpected constant expression in bootstrap method argument:\n${element.dump()}")
                }
            else ->
                throw AssertionError("Unexpected bootstrap method argument:\n${element.dump()}")
        }

    private fun evalBootstrapArgumentIntrinsicCall(irCall: IrCall, codegen: ExpressionCodegen): Any? {
        return when (irCall.symbol) {
            codegen.context.ir.symbols.jvmOriginalMethodTypeIntrinsic ->
                evalOriginalMethodType(irCall, codegen)
            codegen.context.ir.symbols.jvmMethodType ->
                evalMethodType(irCall)
            codegen.context.ir.symbols.jvmMethodHandle ->
                evalMethodHandle(irCall)
            else ->
                null
        }
    }

    private fun evalMethodType(irCall: IrCall): Type {
        val descriptor = irCall.getStringConstArgument(0)
        return Type.getMethodType(descriptor)
    }

    private fun evalMethodHandle(irCall: IrCall): Handle {
        val tag = irCall.getIntConstArgument(0)
        val owner = irCall.getStringConstArgument(1)
        val name = irCall.getStringConstArgument(2)
        val descriptor = irCall.getStringConstArgument(3)
        val isInterface = irCall.getBooleanConstArgument(4)
        return Handle(tag, owner, name, descriptor, isInterface)
    }

    fun generateMethodHandle(irRawFunctionReference: IrRawFunctionReference, codegen: ExpressionCodegen): Handle {
        return generateMethodHandle(codegen.context, irRawFunctionReference)
    }

    private fun evalOriginalMethodType(irCall: IrCall, codegen: ExpressionCodegen): Type {
        val irRawFunRef = irCall.getValueArgument(0) as? IrRawFunctionReference
            ?: throw AssertionError(
                "Argument in ${irCall.symbol.owner.name} call is expected to be a raw function reference:\n" +
                        irCall.dump()
            )
        val irFun = irRawFunRef.symbol.owner
        val asmMethod = codegen.methodSignatureMapper.mapAsmMethod(irFun)
        return Type.getMethodType(asmMethod.descriptor)
    }

}

fun IrCall.getIntConstArgument(i: Int) =
    getValueArgument(i)?.getIntConst()
        ?: throw AssertionError("Value argument #$i should be an Int const: ${dump()}")

fun IrCall.getStringConstArgument(i: Int) =
    getValueArgument(i)?.getStringConst()
        ?: throw AssertionError("Value argument #$i should be a String const: ${dump()}")

fun IrCall.getBooleanConstArgument(i: Int) =
    getValueArgument(i)?.getBooleanConst()
        ?: throw AssertionError("Value argument #$i should be a Boolean const: ${dump()}")


internal fun IrExpression.getIntConst() =
    if (this is IrConst<*> && kind == IrConstKind.Int)
        this.value as Int
    else
        null

internal fun IrExpression.getStringConst() =
    if (this is IrConst<*> && kind == IrConstKind.String)
        this.value as String
    else
        null

internal fun IrExpression.getBooleanConst() =
    if (this is IrConst<*> && kind == IrConstKind.Boolean)
        this.value as Boolean
    else
        null

internal fun generateMethodHandle(jvmBackendContext: JvmBackendContext, irRawFunctionReference: IrRawFunctionReference): Handle {
    return generateMethodHandle(jvmBackendContext, irRawFunctionReference.symbol.owner)
}

fun generateMethodHandle(jvmBackendContext: JvmBackendContext, irFun: IrFunction): Handle {
    val irNonFakeFun = when (irFun) {
        is IrConstructor ->
            irFun
        is IrSimpleFunction -> {
            findSuperDeclaration(irFun, false, jvmBackendContext.state.jvmDefaultMode)
        }
        else ->
            throw java.lang.AssertionError("Simple function or constructor expected: ${irFun.render()}")
    }

    val irParentClass = irNonFakeFun.parent as? IrClass
        ?: throw AssertionError("Unexpected parent: ${irNonFakeFun.parent.render()}")
    val owner = jvmBackendContext.typeMapper.mapOwner(irParentClass)

    val asmMethod = jvmBackendContext.methodSignatureMapper.mapAsmMethod(irNonFakeFun)

    val handleTag = getMethodKindTag(irNonFakeFun)

    return Handle(handleTag, owner.internalName, asmMethod.name, asmMethod.descriptor, irParentClass.isJvmInterface)
}

internal fun getMethodKindTag(irFun: IrFunction) =
    when {
        irFun is IrConstructor ->
            Opcodes.H_NEWINVOKESPECIAL
        irFun.dispatchReceiverParameter == null ->
            Opcodes.H_INVOKESTATIC
        irFun.parentAsClass.isJvmInterface ->
            Opcodes.H_INVOKEINTERFACE
        else ->
            Opcodes.H_INVOKEVIRTUAL
    }


