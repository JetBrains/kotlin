/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.codegen.inline.v
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.overrides.buildFakeOverrideMember
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
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
            dynamicCallee.origin != JvmLoweredDeclarationOrigin.INVOVEDYNAMIC_CALL_TARGET
        )
            fail("Unexpected dynamicCallee: '${dynamicCallee.render()}'")

        val bootstrapMethodTag = expression.getValueArgument(1)?.getIntConst()
            ?: fail("'bootstrapMethodTag' is expected to be an int const")
        val bootstrapMethodOwner = expression.getValueArgument(2)?.getStringConst()
            ?: fail("'bootstrapMethodOwner' is expected to be a string const")
        val bootstrapMethodName = expression.getValueArgument(3)?.getStringConst()
            ?: fail("'bootstrapMethodName' is expected to be a string const")
        val bootstrapMethodDesc = expression.getValueArgument(4)?.getStringConst()
            ?: fail("'bootstrapMethodDesc' is expected to be a string const")
        val bootstrapMethodArgs = (expression.getValueArgument(5)?.safeAs<IrVararg>()
            ?: fail("'bootstrapMethodArgs' is expected to be a vararg"))

        val asmBootstrapMethodArgs = bootstrapMethodArgs.elements
            .map { generateBootstrapMethodArg(it, codegen) }
            .toTypedArray()

        val dynamicCalleeMethod = codegen.methodSignatureMapper.mapAsmMethod(dynamicCallee)
        val bootstrapMethodHandle = Handle(bootstrapMethodTag, bootstrapMethodOwner, bootstrapMethodName, bootstrapMethodDesc, false)

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

        codegen.v.invokedynamic(dynamicCalleeMethod.name, dynamicCalleeMethod.descriptor, bootstrapMethodHandle, asmBootstrapMethodArgs)

        return MaterialValue(codegen, dynamicCalleeMethod.returnType, expression.type)
    }

    private fun generateBootstrapMethodArg(element: IrVarargElement, codegen: ExpressionCodegen): Any =
        when (element) {
            is IrRawFunctionReference ->
                generateMethodHandle(element, codegen)
            is IrCall ->
                when (element.symbol) {
                    codegen.context.ir.symbols.jvmOriginalMethodTypeIntrinsic ->
                        generateOriginalMethodType(element, codegen)
                    codegen.context.ir.symbols.jvmSubstitutedMethodTypeIntrinsic ->
                        generateSubstitutedMethodType(element, codegen)
                    else ->
                        throw AssertionError("Unexpected callee in bootstrap method argument:\n${element.dump()}")
                }
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

    private fun generateMethodHandle(irRawFunctionReference: IrRawFunctionReference, codegen: ExpressionCodegen): Handle {
        val irFun = when (val irFun0 = irRawFunctionReference.symbol.owner) {
            is IrConstructor ->
                irFun0
            is IrSimpleFunction -> {
                // Note that if the given function is a fake override, we emit a method handle with explicit super class.
                // This has the same binary compatibility guarantees as in Java.
                codegen.methodSignatureMapper.findSuperDeclaration(irFun0, false)
            }
            else ->
                throw java.lang.AssertionError("Simple function or constructor expected: ${irFun0.render()}")
        }

        val irParentClass = irFun.parent as? IrClass
            ?: throw AssertionError("Unexpected parent: ${irFun.parent.render()}")
        val owner = codegen.typeMapper.mapOwner(irParentClass)

        val asmMethod = codegen.methodSignatureMapper.mapAsmMethod(irFun)

        val handleTag = when {
            irFun is IrConstructor ->
                Opcodes.H_NEWINVOKESPECIAL
            irFun.dispatchReceiverParameter == null ->
                Opcodes.H_INVOKESTATIC
            irParentClass.isJvmInterface ->
                Opcodes.H_INVOKEINTERFACE
            else ->
                Opcodes.H_INVOKEVIRTUAL
        }

        return Handle(handleTag, owner.internalName, asmMethod.name, asmMethod.descriptor, irParentClass.isJvmInterface)
    }

    private fun generateOriginalMethodType(irCall: IrCall, codegen: ExpressionCodegen): Type {
        val irRawFunRef = irCall.getValueArgument(0) as? IrRawFunctionReference
            ?: throw AssertionError(
                "Argument in ${irCall.symbol.owner.name} call is expected to be a raw function reference:\n" +
                        irCall.dump()
            )
        val irFun = irRawFunRef.symbol.owner
        val asmMethod = codegen.methodSignatureMapper.mapAsmMethod(irFun)
        return Type.getMethodType(asmMethod.descriptor)
    }

    private fun generateSubstitutedMethodType(irCall: IrCall, codegen: ExpressionCodegen): Type {
        fun fail(message: String): Nothing =
            throw AssertionError("$message; irCall:\n${irCall.dump()}")

        val irRawFunRef = irCall.getValueArgument(0) as? IrRawFunctionReference
            ?: fail("Argument in ${irCall.symbol.owner.name} call is expected to be a raw function reference")
        val irOriginalFun = irRawFunRef.symbol.owner as? IrSimpleFunction
            ?: fail("IrSimpleFunction expected: ${irRawFunRef.symbol.owner.render()}")

        val superType = irCall.getTypeArgument(0) as? IrSimpleType
            ?: fail("Type argument expected")
        val patchedSuperType = replaceTypeArgumentsWithNullable(superType)

        val fakeClass = codegen.context.irFactory.buildClass { name = Name.special("<fake>") }
        fakeClass.parent = codegen.context.ir.symbols.kotlinJvmInternalInvokeDynamicPackage
        val irFakeOverride = buildFakeOverrideMember(patchedSuperType, irOriginalFun, fakeClass) as IrSimpleFunction
        irFakeOverride.overriddenSymbols = listOf(irOriginalFun.symbol)

        val asmMethod = codegen.methodSignatureMapper.mapAsmMethod(irFakeOverride)
        return Type.getMethodType(asmMethod.descriptor)
    }

    // Given the following functional interface
    //  fun interface IFoo<T> {
    //      fun foo(x: T): T
    //  }
    // To comply with java.lang.invoke.LambdaMetafactory requirements, we need an instance method that accepts references
    // (not primitives, and not unboxed inline classes).
    // In order to do so, we replace type arguments with nullable types.
    private fun replaceTypeArgumentsWithNullable(substitutedType: IrSimpleType) =
        substitutedType.classifier.typeWithArguments(
            substitutedType.arguments.map { typeArgument ->
                when (typeArgument) {
                    is IrStarProjection -> typeArgument
                    is IrTypeProjection -> {
                        val type = typeArgument.type
                        if (type !is IrSimpleType || type.hasQuestionMark)
                            typeArgument
                        else {
                            makeTypeProjection(type.withHasQuestionMark(true), typeArgument.variance)
                        }
                    }
                    else ->
                        throw AssertionError("Unexpected type argument '$typeArgument' :: ${typeArgument::class.simpleName}")
                }
            }
        )

    private fun IrExpression.getIntConst() =
        if (this is IrConst<*> && kind == IrConstKind.Int)
            this.value as Int
        else
            null

    private fun IrExpression.getStringConst() =
        if (this is IrConst<*> && kind == IrConstKind.String)
            this.value as String
        else
            null

}