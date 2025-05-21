/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.getArrayElementType
import org.jetbrains.kotlin.ir.util.isBoxedArray
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.Method
import java.lang.invoke.LambdaMetafactory

/**
 * Lowers [IrTypeOperatorCall]s to (implicit) casts and instanceof checks.
 *
 * After this pass runs, there are only four kinds of [IrTypeOperatorCall]s left:
 *
 * - `IMPLICIT_CAST`
 * - `SAFE_CAST` with reified type parameters
 * - `INSTANCEOF` with non-nullable type operand or reified type parameters
 * - `CAST` with non-nullable argument, nullable type operand, or reified type parameters
 *
 * The latter two correspond to the `instanceof`/`checkcast` instructions on the JVM, except for the presence of reified type parameters.
 */
@PhaseDescription(name = "TypeOperatorLowering")
internal class TypeOperatorLowering(private val backendContext: JvmBackendContext) :
    FileLoweringPass, IrBuildingTransformer(backendContext) {

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    private fun IrExpression.transformVoid() = transform(this@TypeOperatorLowering, null)

    private fun lowerInstanceOf(argument: IrExpression, type: IrType) = with(builder) {
        when {
            type.isReifiedTypeParameter ->
                irIs(argument, type)
            argument.type.isNullable() && type.isNullable() -> {
                irLetS(argument, irType = context.irBuiltIns.anyNType) { valueSymbol ->
                    context.oror(
                        irEqualsNull(irGet(valueSymbol.owner)),
                        irIs(irGet(valueSymbol.owner), type.makeNotNull())
                    )
                }
            }
            argument.type.isNullable() && !type.isNullable() && argument.type.erasedUpperBound == type.erasedUpperBound ->
                irNotEquals(argument, irNull())
            else -> irIs(argument, type.makeNotNull())
        }
    }

    private fun lowerCast(argument: IrExpression, type: IrType): IrExpression =
        when {
            type.isReifiedTypeParameter ->
                builder.irAs(argument, type)
            argument.type.isInlineClassType() && argument.type.isSubtypeOfClass(type.erasedUpperBound.symbol) ->
                argument
            isCompatibleArrayType(argument.type, type) ->
                argument
            type.isNullable() || argument.isDefinitelyNotNull() ->
                builder.irAs(argument, type)
            else -> {
                with(builder) {
                    irLetS(argument, irType = context.irBuiltIns.anyNType) { tmp ->
                        val message = irString("null cannot be cast to non-null type ${type.render()}")
                        if (backendContext.config.unifiedNullChecks) {
                            // Avoid branching to improve code coverage (KT-27427).
                            // We have to generate a null check here, because even if argument is of non-null type,
                            // it can be uninitialized value, which is 'null' for reference types in JMM.
                            // Most of such null checks will never actually throw, but we can't do anything about it.
                            irBlock(resultType = type) {
                                +irCall(backendContext.symbols.checkNotNullWithMessage).apply {
                                    arguments[0] = irGet(tmp.owner)
                                    arguments[1] = message
                                }
                                +irAs(irGet(tmp.owner), type.makeNullable())
                            }
                        } else {
                            irIfNull(
                                type,
                                irGet(tmp.owner),
                                irCall(throwTypeCastException).apply {
                                    arguments[0] = message
                                },
                                irAs(irGet(tmp.owner), type.makeNullable())
                            )
                        }
                    }
                }
            }
        }

    private fun isCompatibleArrayType(actualType: IrType, expectedType: IrType): Boolean {
        var actual = actualType
        var expected = expectedType
        while ((actual.isArray() || actual.isNullableArray()) && (expected.isArray() || expected.isNullableArray())) {
            actual = actual.getArrayElementLowerType()
            expected = expected.getArrayElementLowerType()
        }
        if (actual == actualType || expected == expectedType) return false
        return actual.isSubtypeOfClass(expected.erasedUpperBound.symbol)
    }

    private fun IrType.getArrayElementLowerType(): IrType =
        if (isBoxedArray && this is IrSimpleType && (arguments.singleOrNull() as? IrTypeProjection)?.variance == Variance.IN_VARIANCE)
            backendContext.irBuiltIns.anyNType
        else getArrayElementType(backendContext.irBuiltIns)

    // TODO extract null check elimination on IR somewhere?
    private fun IrExpression.isDefinitelyNotNull(): Boolean =
        when (this) {
            is IrGetValue ->
                this.symbol.owner.isDefinitelyNotNullVal()
            is IrGetClass,
            is IrConstructorCall ->
                true
            is IrCall ->
                this.symbol == backendContext.irBuiltIns.checkNotNullSymbol
            else ->
                false
        }

    private fun IrValueDeclaration.isDefinitelyNotNullVal(): Boolean {
        val irVariable = this as? IrVariable ?: return false
        return !irVariable.isVar && irVariable.initializer?.isDefinitelyNotNull() == true
    }

    private val jvmIndyLambdaMetafactoryIntrinsic = backendContext.symbols.indyLambdaMetafactoryIntrinsic

    private fun JvmIrBuilder.jvmInvokeDynamic(
        dynamicCall: IrCall,
        bootstrapMethodHandle: Handle,
        bootstrapMethodArguments: List<IrExpression>
    ) =
        irCall(backendContext.symbols.jvmIndyIntrinsic, dynamicCall.type).apply {
            typeArguments[0] = dynamicCall.type
            arguments[0] = dynamicCall
            arguments[1] = jvmMethodHandle(bootstrapMethodHandle)
            arguments[2] = irVararg(context.irBuiltIns.anyType, bootstrapMethodArguments)
        }

    private fun JvmIrBuilder.jvmOriginalMethodType(methodSymbol: IrFunctionSymbol) =
        irCall(backendContext.symbols.jvmOriginalMethodTypeIntrinsic, context.irBuiltIns.anyType).apply {
            arguments[0] = irRawFunctionReference(context.irBuiltIns.anyType, methodSymbol)
        }

    /**
     * @see java.lang.invoke.LambdaMetafactory.metafactory
     */
    private val jdkMetafactoryHandle =
        Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(" +
                    "Ljava/lang/invoke/MethodHandles\$Lookup;" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/invoke/MethodType;" +
                    "Ljava/lang/invoke/MethodType;" +
                    "Ljava/lang/invoke/MethodHandle;" +
                    "Ljava/lang/invoke/MethodType;" +
                    ")Ljava/lang/invoke/CallSite;",
            false
        )

    /**
     * @see java.lang.invoke.LambdaMetafactory.altMetafactory
     */
    private val jdkAltMetafactoryHandle =
        Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "altMetafactory",
            "(" +
                    "Ljava/lang/invoke/MethodHandles\$Lookup;" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/invoke/MethodType;" +
                    "[Ljava/lang/Object;" +
                    ")Ljava/lang/invoke/CallSite;",
            false
        )

    override fun visitCall(expression: IrCall): IrExpression {
        return when (expression.symbol) {
            jvmIndyLambdaMetafactoryIntrinsic -> {
                expression.transformChildrenVoid()
                rewriteIndyLambdaMetafactoryCall(expression)
            }
            else -> super.visitCall(expression)
        }
    }

    private class SerializableMethodRefInfo(
        val samType: IrType,
        val samMethodSymbol: IrSimpleFunctionSymbol,
        val implFunSymbol: IrFunctionSymbol,
        val instanceFunSymbol: IrFunctionSymbol,
        val requiredBridges: Collection<IrSimpleFunction>,
        val dynamicCallSymbol: IrSimpleFunctionSymbol
    )

    private class ClassContext {
        val serializableMethodRefInfos = ArrayList<SerializableMethodRefInfo>()
    }

    private val classContextStack = ArrayDeque<ClassContext>()

    private fun enterClass(): ClassContext {
        return ClassContext().also {
            classContextStack.push(it)
        }
    }

    private fun leaveClass() {
        classContextStack.pop()
    }

    private fun getClassContext(): ClassContext {
        if (classContextStack.isEmpty()) {
            throw AssertionError("No class context")
        }
        return classContextStack.last()
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val context = enterClass()
        val result = super.visitClass(declaration)
        if (context.serializableMethodRefInfos.isNotEmpty()) {
            generateDeserializeLambdaMethod(declaration, context.serializableMethodRefInfos)
        }
        leaveClass()
        return result
    }

    private data class DeserializedLambdaInfo(
        val functionalInterfaceClass: String,
        val implMethodHandle: Handle,
        val functionalInterfaceMethod: Method
    )

    private fun generateDeserializeLambdaMethod(
        irClass: IrClass,
        serializableMethodRefInfos: List<SerializableMethodRefInfo>
    ) {
        //  fun `$deserializeLambda$`(lambda: java.lang.invoke.SerializedLambda): Object {
        //      val tmp = lambda.getImplMethodName()
        //      when {
        //          ...
        //          tmp == NAME_i -> {
        //              when {
        //                  ...
        //                  lambda.getImplMethodKind() == [LAMBDA_i_k].implMethodKind &&
        //                  lambda.getFunctionalInterfaceClass() == [LAMBDA_i_k].functionalInterfaceClass &&
        //                  lambda.getFunctionalInterfaceMethodName() == [LAMBDA_i_k].functionalInterfaceMethodName &&
        //                  lambda.getFunctionalInterfaceMethodSignature() == [LAMBDA_i_k].functionalInterfaceMethodSignature &&
        //                  lambda.getImplClass() == [LAMBDA_i_k].implClass &&
        //                  lambda.getImplMethodSignature() = [LAMBDA_i_k].implMethodSignature ->
        //                      `<jvm-indy>`([LAMBDA_i_k])
        //                  ...
        //              }
        //          }
        //          ...
        //      }
        //      throw IllegalArgumentException("Invalid lambda deserialization")
        //  }

        val groupedByImplMethodName = HashMap<String, HashMap<DeserializedLambdaInfo, SerializableMethodRefInfo>>()
        for (serializableMethodRefInfo in serializableMethodRefInfos) {
            val deserializedLambdaInfo = mapDeserializedLambda(serializableMethodRefInfo)
            val implMethodName = deserializedLambdaInfo.implMethodHandle.name
            val byDeserializedLambdaInfo = groupedByImplMethodName.getOrPut(implMethodName) { HashMap() }
            byDeserializedLambdaInfo[deserializedLambdaInfo] = serializableMethodRefInfo
        }

        val deserializeLambdaFun = backendContext.irFactory.buildFun {
            name = Name.identifier("\$deserializeLambda\$")
            visibility = DescriptorVisibilities.PRIVATE
            origin = JvmLoweredDeclarationOrigin.DESERIALIZE_LAMBDA_FUN
        }
        deserializeLambdaFun.parent = irClass
        val lambdaParameter = deserializeLambdaFun.addValueParameter("lambda", backendContext.symbols.serializedLambda.irType)
        deserializeLambdaFun.returnType = backendContext.irBuiltIns.anyType
        deserializeLambdaFun.body = backendContext.createJvmIrBuilder(deserializeLambdaFun.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET).run {
            irBlockBody {
                val tmp = irTemporary(
                    irCall(backendContext.symbols.serializedLambda.getImplMethodName).apply {
                        arguments[0] = irGet(lambdaParameter)
                    }
                )
                +irWhen(
                    backendContext.irBuiltIns.unitType,
                    groupedByImplMethodName.entries.map { (implMethodName, infos) ->
                        irBranch(
                            irEquals(irGet(tmp), irString(implMethodName)),
                            irWhen(
                                backendContext.irBuiltIns.unitType,
                                infos.entries.map { (deserializedLambdaInfo, serializedMethodRefInfo) ->
                                    irBranch(
                                        generateSerializedLambdaEquals(lambdaParameter, deserializedLambdaInfo),
                                        irReturn(generateCreateDeserializedMethodRef(lambdaParameter, serializedMethodRefInfo))
                                    )
                                }
                            )
                        )
                    }
                )

                +irThrow(
                    irCall(backendContext.symbols.illegalArgumentExceptionCtorString).also { ctorCall ->
                        // Replace argument with:
                        //  irCall(backendContext.irBuiltIns.anyClass.getSimpleFunction("toString")!!).apply {
                        //      arguments[0] = irGet(lambdaParameter)
                        //  }
                        // for debugging "Invalid lambda deserialization" exceptions.
                        ctorCall.arguments[0] = irString("Invalid lambda deserialization")
                    }
                )
            }
        }

        irClass.declarations.add(deserializeLambdaFun)
    }

    private fun mapDeserializedLambda(info: SerializableMethodRefInfo) =
        DeserializedLambdaInfo(
            functionalInterfaceClass = backendContext.defaultTypeMapper.mapType(info.samType).internalName,
            implMethodHandle = backendContext.defaultMethodSignatureMapper.mapToMethodHandle(info.implFunSymbol.owner),
            functionalInterfaceMethod = backendContext.defaultMethodSignatureMapper.mapAsmMethod(info.samMethodSymbol.owner)
        )

    private fun JvmIrBuilder.generateSerializedLambdaEquals(
        lambdaParameter: IrValueParameter,
        deserializedLambdaInfo: DeserializedLambdaInfo
    ): IrExpression {
        val functionalInterfaceClass = deserializedLambdaInfo.functionalInterfaceClass
        val implMethodHandle = deserializedLambdaInfo.implMethodHandle
        val samMethod = deserializedLambdaInfo.functionalInterfaceMethod

        fun irGetLambdaProperty(getter: IrSimpleFunction) =
            irCall(getter).apply { arguments[0] = irGet(lambdaParameter) }

        return irAndAnd(
            irEquals(
                irGetLambdaProperty(backendContext.symbols.serializedLambda.getImplMethodKind),
                irInt(implMethodHandle.tag)
            ),
            irObjectEquals(
                irGetLambdaProperty(backendContext.symbols.serializedLambda.getFunctionalInterfaceClass),
                irString(functionalInterfaceClass)
            ),
            irObjectEquals(
                irGetLambdaProperty(backendContext.symbols.serializedLambda.getFunctionalInterfaceMethodName),
                irString(samMethod.name)
            ),
            irObjectEquals(
                irGetLambdaProperty(backendContext.symbols.serializedLambda.getFunctionalInterfaceMethodSignature),
                irString(samMethod.descriptor)
            ),
            irObjectEquals(
                irGetLambdaProperty(backendContext.symbols.serializedLambda.getImplClass),
                irString(implMethodHandle.owner)
            ),
            irObjectEquals(
                irGetLambdaProperty(backendContext.symbols.serializedLambda.getImplMethodSignature),
                irString(implMethodHandle.desc)
            )
        )
    }

    private val equalsAny = backendContext.irBuiltIns.anyClass.getSimpleFunction("equals")!!

    private fun JvmIrBuilder.irObjectEquals(receiver: IrExpression, arg: IrExpression) =
        irCall(equalsAny).apply {
            arguments[0] = receiver
            arguments[1] = arg
        }

    private fun JvmIrBuilder.irAndAnd(vararg args: IrExpression): IrExpression {
        if (args.isEmpty()) throw AssertionError("At least one argument expected")
        var result = args[0]
        for (i in 1 until args.size) {
            result = irCall(backendContext.irBuiltIns.andandSymbol).apply {
                arguments[0] = result
                arguments[1] = args[i]
            }
        }
        return result
    }

    private fun JvmIrBuilder.generateCreateDeserializedMethodRef(
        lambdaParameter: IrValueParameter,
        info: SerializableMethodRefInfo
    ): IrExpression {
        val dynamicCall = irCall(info.dynamicCallSymbol)
        for ((index, dynamicValueParameter) in info.dynamicCallSymbol.owner.parameters.withIndex()) {
            val capturedArg = irCall(backendContext.symbols.serializedLambda.getCapturedArg).also { call ->
                call.arguments[0] = irGet(lambdaParameter)
                call.arguments[1] = irInt(index)
            }
            val expectedType = dynamicValueParameter.type
            val downcastArg =
                if (expectedType.isInlineClassType()) {
                    // Inline class type arguments are stored as their underlying representation.
                    val unboxedType = expectedType.unboxInlineClass()
                    irCall(backendContext.symbols.unsafeCoerceIntrinsic).also { coercion ->
                        coercion.typeArguments[0] = unboxedType
                        coercion.typeArguments[1] = expectedType
                        coercion.arguments[0] = capturedArg
                    }
                } else {
                    irAs(capturedArg, expectedType)
                }
            dynamicCall.arguments[index] = downcastArg
        }

        return createLambdaMetafactoryCall(
            info.samMethodSymbol, info.implFunSymbol, info.instanceFunSymbol, true, info.requiredBridges, dynamicCall
        )
    }

    /**
     * @see FunctionReferenceLowering.wrapWithIndySamConversion
     */
    private fun rewriteIndyLambdaMetafactoryCall(call: IrCall): IrCall {
        fun fail(message: String): Nothing =
            throw AssertionError("$message, call:\n${call.dump()}")

        val startOffset = call.startOffset
        val endOffset = call.endOffset

        val samType = call.typeArguments[0] as? IrSimpleType
            ?: fail("'samType' is expected to be a simple type")

        val samMethodRef = call.arguments[0] as? IrRawFunctionReference
            ?: fail("'samMethodType' should be 'IrRawFunctionReference'")
        val implFunRef = call.arguments[1] as? IrFunctionReference
            ?: fail("'implMethodReference' is expected to be 'IrFunctionReference'")
        val implFunSymbol = implFunRef.symbol
        val instanceMethodRef = call.arguments[2] as? IrRawFunctionReference
            ?: fail("'instantiatedMethodType' is expected to be 'IrRawFunctionReference'")

        val extraOverriddenMethods = run {
            val extraOverriddenMethodVararg = call.arguments[3] as? IrVararg
                ?: fail("'extraOverriddenMethodTypes' is expected to be 'IrVararg'")
            extraOverriddenMethodVararg.elements.map {
                val ref = it as? IrRawFunctionReference
                    ?: fail("'extraOverriddenMethodTypes' elements are expected to be 'IrRawFunctionReference'")
                ref.symbol.owner as? IrSimpleFunction
                    ?: fail("Extra overridden method is expected to be 'IrSimpleFunction': ${ref.symbol.owner.render()}")
            }
        }

        val shouldBeSerializable = call.getBooleanConstArgument(4)

        val samMethod = samMethodRef.symbol.owner as? IrSimpleFunction
            ?: fail("SAM method is expected to be 'IrSimpleFunction': ${samMethodRef.symbol.owner.render()}")
        val instanceMethod = instanceMethodRef.symbol.owner as? IrSimpleFunction
            ?: fail("Instance method is expected to be 'IrSimpleFunction': ${instanceMethodRef.symbol.owner.render()}")

        val dynamicCall = wrapClosureInDynamicCall(samType, samMethod, implFunRef)

        val requiredBridges = getOverriddenMethodsRequiringBridges(instanceMethod, samMethod, extraOverriddenMethods)

        if (shouldBeSerializable) {
            getClassContext().serializableMethodRefInfos.add(
                SerializableMethodRefInfo(
                    samType, samMethod.symbol, implFunSymbol, instanceMethodRef.symbol, requiredBridges, dynamicCall.symbol
                )
            )
        }

        return backendContext.createJvmIrBuilder(implFunSymbol, startOffset, endOffset)
            .createLambdaMetafactoryCall(
                samMethod.symbol, implFunSymbol, instanceMethodRef.symbol, shouldBeSerializable, requiredBridges, dynamicCall
            )
    }

    private fun JvmIrBuilder.createLambdaMetafactoryCall(
        samMethodSymbol: IrSimpleFunctionSymbol,
        implFunSymbol: IrFunctionSymbol,
        instanceMethodSymbol: IrFunctionSymbol,
        shouldBeSerializable: Boolean,
        requiredBridges: Collection<IrSimpleFunction>,
        dynamicCall: IrCall
    ): IrCall {
        val samMethodType = jvmOriginalMethodType(samMethodSymbol)
        val implFunRawRef = irRawFunctionReference(context.irBuiltIns.anyType, implFunSymbol)
        val instanceMethodType = jvmOriginalMethodType(instanceMethodSymbol)

        var bootstrapMethod = jdkMetafactoryHandle
        val bootstrapMethodArguments = arrayListOf<IrExpression>(
            samMethodType,
            implFunRawRef,
            instanceMethodType
        )
        var bridgeMethodTypes = emptyList<IrExpression>()

        var flags = 0

        if (shouldBeSerializable) {
            flags += LambdaMetafactory.FLAG_SERIALIZABLE
        }

        if (requiredBridges.isNotEmpty()) {
            flags += LambdaMetafactory.FLAG_BRIDGES
            bridgeMethodTypes = requiredBridges.map { jvmOriginalMethodType(it.symbol) }
        }

        if (flags != 0) {
            bootstrapMethod = jdkAltMetafactoryHandle
            // Bootstrap arguments for LambdaMetafactory#altMetafactory should be:
            //     MethodType samMethodType,
            //     MethodHandle implMethod,
            //     MethodType instantiatedMethodType,
            //     // ---------------------------- same as LambdaMetafactory#metafactory until here
            //     int flags,                   // combination of LambdaMetafactory.{ FLAG_SERIALIZABLE, FLAG_MARKERS, FLAG_BRIDGES }
            //     int markerInterfaceCount,    // IF flags has MARKERS set
            //     Class... markerInterfaces,   // IF flags has MARKERS set
            //     int bridgeCount,             // IF flags has BRIDGES set
            //     MethodType... bridges        // IF flags has BRIDGES set

            // `flags`
            bootstrapMethodArguments.add(irInt(flags))

            // `markerInterfaceCount`, `markerInterfaces`
            // Currently, it looks like there's no way a Kotlin function expression should be SAM-converted to a type implementing
            // additional marker interfaces (such as `Runnable r = (Runnable & Marker) () -> { ... }` in Java).
            // If such additional marker interfaces would appear, they should go here, between `flags` and `bridgeCount`.

            if (bridgeMethodTypes.isNotEmpty()) {
                // `bridgeCount`, `bridges`
                bootstrapMethodArguments.add(irInt(bridgeMethodTypes.size))
                bootstrapMethodArguments.addAll(bridgeMethodTypes)
            }
        }

        return jvmInvokeDynamic(dynamicCall, bootstrapMethod, bootstrapMethodArguments)
    }

    private fun getOverriddenMethodsRequiringBridges(
        instanceMethod: IrSimpleFunction,
        samMethod: IrSimpleFunction,
        extraOverriddenMethods: List<IrSimpleFunction>
    ): Collection<IrSimpleFunction> {
        val jvmInstanceMethod = backendContext.defaultMethodSignatureMapper.mapAsmMethod(instanceMethod)
        val jvmSamMethod = backendContext.defaultMethodSignatureMapper.mapAsmMethod(samMethod)

        val signatureToNonFakeOverride = LinkedHashMap<Method, IrSimpleFunction>()
        for (overridden in extraOverriddenMethods) {
            val jvmOverriddenMethod = backendContext.defaultMethodSignatureMapper.mapAsmMethod(overridden)
            if (jvmOverriddenMethod != jvmInstanceMethod && jvmOverriddenMethod != jvmSamMethod) {
                signatureToNonFakeOverride[jvmOverriddenMethod] = overridden
            }
        }
        return signatureToNonFakeOverride.values
    }

    private fun wrapClosureInDynamicCall(
        erasedSamType: IrSimpleType,
        samMethod: IrSimpleFunction,
        targetRef: IrFunctionReference
    ): IrCall {
        fun fail(message: String): Nothing =
            throw AssertionError("$message, targetRef:\n${targetRef.dump()}")

        val dynamicCallArguments = mutableListOf<IrExpression>()

        val irDynamicCallTarget = backendContext.irFactory.buildFun {
            origin = JvmLoweredDeclarationOrigin.INVOKEDYNAMIC_CALL_TARGET
            name = samMethod.name
            returnType = erasedSamType
        }.apply {
            parent = backendContext.symbols.kotlinJvmInternalInvokeDynamicPackage

            val targetFun = targetRef.symbol.owner

            var syntheticParameterIndex = 0

            var argumentStart = 0
            parameters = (targetFun.parameters zip targetRef.arguments).mapNotNull { (parameter, argument) ->
                if (argument == null) return@mapNotNull null
                val (newParameterType, newArgument) = when (parameter.kind) {
                    IrParameterKind.DispatchReceiver -> when (targetFun) {
                        is IrSimpleFunction -> {
                            // Fake overrides may have inexact dispatch receiver type.
                            targetFun.parentAsClass.defaultType to argument
                        }
                        is IrConstructor -> {
                            // At this point, outer class instances in inner class constructors are represented as regular value parameters.
                            // However, in a function reference to such constructors, bound receiver value is stored as a dispatch receiver.
                            argumentStart++
                            targetFun.parameters[0].type to argument
                        }
                    }
                    IrParameterKind.Context, IrParameterKind.ExtensionReceiver -> {
                        argumentStart++
                        parameter.type to argument
                    }
                    IrParameterKind.Regular -> {
                        val capturedValueArgument = targetRef.arguments[argumentStart]
                            ?: fail("Captured value argument #$argumentStart (${parameter.render()}) not provided")
                        argumentStart++
                        parameter.type to capturedValueArgument
                    }
                }

                dynamicCallArguments.add(newArgument)

                buildValueParameter(this) {
                    name = Name.identifier("p${syntheticParameterIndex++}")
                    type = newParameterType
                    kind = IrParameterKind.Regular
                }
            }
        }

        if (dynamicCallArguments.size != irDynamicCallTarget.parameters.size) {
            throw AssertionError(
                "Dynamic call target parameters (${irDynamicCallTarget.parameters.size}) " +
                        "don't match dynamic call arguments (${dynamicCallArguments.size}):\n" +
                        "irDynamicCallTarget:\n" +
                        irDynamicCallTarget.dump() +
                        "dynamicCallArguments:\n" +
                        dynamicCallArguments
                            .withIndex()
                            .joinToString(separator = "\n ", prefix = "[\n ", postfix = "\n]") { (index, irArg) ->
                                "#$index: ${irArg.dump()}"
                            }
            )
        }

        return backendContext.createJvmIrBuilder(irDynamicCallTarget.symbol)
            .irCall(irDynamicCallTarget.symbol)
            .apply { arguments.assignFrom(dynamicCallArguments) }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression = with(builder) {
        at(expression)
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
                irComposite(resultType = expression.type) {
                    +expression.argument.transformVoid()
                    // TODO: Don't generate these casts in the first place
                    if (!expression.argument.type.isSubtypeOf(expression.type.makeNullable(), backendContext.typeSystem)) {
                        +IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, expression.type)
                    }
                }

            // There is no difference between IMPLICIT_CAST and IMPLICIT_INTEGER_COERCION on JVM_IR.
            // Instead, this is handled in StackValue.coerce.
            IrTypeOperator.IMPLICIT_INTEGER_COERCION ->
                irImplicitCast(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.CAST ->
                lowerCast(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.SAFE_CAST ->
                if (expression.typeOperand.isReifiedTypeParameter) {
                    expression.transformChildrenVoid()
                    expression
                } else {
                    irLetS(
                        expression.argument.transformVoid(),
                        IrStatementOrigin.SAFE_CALL,
                        irType = context.irBuiltIns.anyNType
                    ) { valueSymbol ->
                        val thenPart =
                            if (valueSymbol.owner.type.isInlineClassType())
                                lowerCast(irGet(valueSymbol.owner), expression.typeOperand)
                            else
                                irGet(valueSymbol.owner)
                        irIfThenElse(
                            expression.type,
                            lowerInstanceOf(irGet(valueSymbol.owner), expression.typeOperand.makeNotNull()),
                            thenPart,
                            irNull(expression.type)
                        )
                    }
                }

            IrTypeOperator.INSTANCEOF ->
                lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.NOT_INSTANCEOF ->
                irNot(lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand))

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val text = computeNotNullAssertionText(expression)

                irLetS(expression.argument.transformVoid(), irType = context.irBuiltIns.anyNType) { valueSymbol ->
                    irComposite(resultType = expression.type) {
                        if (text != null) {
                            +irCall(checkExpressionValueIsNotNull).apply {
                                arguments[0] = irGet(valueSymbol.owner)
                                arguments[1] = irString(text.trimForRuntimeAssertion())
                            }
                        } else {
                            +irCall(backendContext.symbols.checkNotNull).apply {
                                arguments[0] = irGet(valueSymbol.owner)
                            }
                        }
                        +irGet(valueSymbol.owner)
                    }
                }
            }

            else -> {
                expression.transformChildrenVoid()
                expression
            }
        }
    }

    private fun IrBuilderWithScope.computeNotNullAssertionText(typeOperatorCall: IrTypeOperatorCall): String? {
        if (backendContext.config.noSourceCodeInNotNullAssertionExceptions) {
            return when (val argument = typeOperatorCall.argument) {
                is IrCall -> "${argument.symbol.owner.name.asString()}(...)"
                is IrGetField -> {
                    val field = argument.symbol.owner
                    field.name.asString().takeUnless { field.origin.isSynthetic }
                }
                else -> null
            }
        }

        val owner = scope.scopeOwnerSymbol.owner
        if (owner is IrFunction && owner.isDelegated())
            return "${owner.name.asString()}(...)"

        val declarationParent = parent as? IrDeclaration
        val sourceView = declarationParent?.let(::sourceViewFor)
        val (startOffset, endOffset) = typeOperatorCall.extents()
        return if (sourceView?.validSourcePosition(startOffset, endOffset) == true) {
            sourceView.subSequence(startOffset, endOffset).toString()
        } else {
            // Fallback for inconsistent line numbers
            (declarationParent as? IrDeclarationWithName)?.name?.asString() ?: "Unknown Declaration"
        }
    }

    private fun String.trimForRuntimeAssertion() = StringUtil.trimMiddle(this, 50)

    private fun IrFunction.isDelegated() =
        origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR ||
                origin == IrDeclarationOrigin.DELEGATED_MEMBER

    private fun CharSequence.validSourcePosition(startOffset: Int, endOffset: Int): Boolean =
        startOffset in 0 until endOffset && endOffset < length

    private fun IrElement.extents(): Pair<Int, Int> {
        var startOffset = Int.MAX_VALUE
        var endOffset = 0
        acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
                if (element.startOffset in 0 until startOffset)
                    startOffset = element.startOffset
                if (endOffset < element.endOffset)
                    endOffset = element.endOffset
            }
        })
        return startOffset to endOffset
    }

    private fun sourceViewFor(declaration: IrDeclaration): CharSequence? =
        declaration.fileParent.getKtFile()?.viewProvider?.contents

    private val throwTypeCastException: IrSimpleFunctionSymbol =
        backendContext.symbols.throwTypeCastException

    private val checkExpressionValueIsNotNull: IrSimpleFunctionSymbol =
        if (backendContext.config.unifiedNullChecks)
            backendContext.symbols.checkNotNullExpressionValue
        else
            backendContext.symbols.checkExpressionValueIsNotNull
}
