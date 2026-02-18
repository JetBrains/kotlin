/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.indy.*
import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.Method
import java.lang.invoke.LambdaMetafactory

class IndyLambdaMetafactoryLowering(val backendContext: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    private val inlineLambdaToScope = mutableMapOf<IrFunction, IrDeclaration>()


    override fun lower(irFile: IrFile) {
        irFile.findRichInlineLambdas(backendContext) { argument, _, _, scope ->
            inlineLambdaToScope[argument.invokeFunction] = scope
        }
        irFile.transformChildrenVoid()
        inlineLambdaToScope.clear()
    }


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

    override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
        val indyCallData = expression.indyCallData ?: return super.visitRichFunctionReference(expression)
        expression.transformChildrenVoid()
        getClassContext().functionsToAdd.add(expression.invokeFunction)
        return rewriteIndyLambdaMetafactoryCall(expression, indyCallData)
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
        val serializableMethodRefInfos = mutableListOf<SerializableMethodRefInfo>()
        val functionsToAdd = mutableListOf<IrFunction>()
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
        for (function in context.functionsToAdd) {
            function.visibility = DescriptorVisibilities.PRIVATE
            declaration.addChild(function)
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

    private fun rewriteIndyLambdaMetafactoryCall(
        call: IrRichFunctionReference,
        indyCallData: IndyCallData,
    ): IrCall {
        fun fail(message: String): Nothing =
            throw AssertionError("$message, call:\n${call.dump()}")

        val startOffset = call.startOffset
        val endOffset = call.endOffset

        val samType = call.type as? IrSimpleType ?: fail("'samType' is expected to be a simple type")

        val implFunSymbol = call.invokeFunction.symbol

        val generatedParameters = LambdaMetafactoryArgumentsBuilder(backendContext, emptySet())
            .getLambdaMetafactoryArguments(
                call, plainLambda = indyCallData.plainLambda, forceSerializability = indyCallData.forceSerializability
            )
        require(generatedParameters is LambdaMetafactoryArguments) {
            """Unexpected lambda metafactory arguments for ${call.dump()}, ${indyCallData}:
               $generatedParameters
            """.trimIndent()
        }

        val dynamicCall = wrapClosureInDynamicCall(samType, generatedParameters.samMethod, call)

        val requiredBridges = getOverriddenMethodsRequiringBridges(
            instanceMethod = generatedParameters.fakeInstanceMethod,
            samMethod = generatedParameters.samMethod,
            extraOverriddenMethods = generatedParameters.extraOverriddenMethods
        )

        if (generatedParameters.shouldBeSerializable) {
            getClassContext().serializableMethodRefInfos.add(
                SerializableMethodRefInfo(
                    samType = samType,
                    samMethodSymbol = generatedParameters.samMethod.symbol,
                    implFunSymbol = implFunSymbol,
                    instanceFunSymbol = generatedParameters.fakeInstanceMethod.symbol,
                    requiredBridges = requiredBridges,
                    dynamicCallSymbol = dynamicCall.symbol
                )
            )
        }

        return backendContext.createJvmIrBuilder(implFunSymbol, startOffset, endOffset)
            .createLambdaMetafactoryCall(
                generatedParameters.samMethod.symbol, implFunSymbol,
                generatedParameters.fakeInstanceMethod.symbol, generatedParameters.shouldBeSerializable, requiredBridges, dynamicCall
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
        targetRef: IrRichFunctionReference,
    ): IrCall {
        val dynamicCallArguments = mutableListOf<IrExpression>()

        val irDynamicCallTarget = backendContext.irFactory.buildFun {
            origin = JvmLoweredDeclarationOrigin.INVOKEDYNAMIC_CALL_TARGET
            name = samMethod.name
            returnType = erasedSamType
        }.apply {
            parent = backendContext.symbols.kotlinJvmInternalInvokeDynamicPackage

            val targetFun = targetRef.invokeFunction

            var syntheticParameterIndex = 0

            parameters = (targetFun.parameters zip targetRef.boundValues).map { (parameter, argument) ->
                dynamicCallArguments.add(argument)

                buildValueParameter(this) {
                    name = Name.identifier("p${syntheticParameterIndex++}")
                    type = parameter.type
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

    companion object {

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

        fun isLambdaMetafactoryIndy(symbols: JvmSymbols, call: IrCall): Boolean {
            if (call.symbol != symbols.jvmIndyIntrinsic) return false
            val boostrapMethodHandle = call.arguments.getOrNull(1) as? IrCall ?: return false
            if (boostrapMethodHandle.symbol != symbols.jvmMethodHandle) return false
            val owner = boostrapMethodHandle.arguments.getOrNull(1) as? IrConst ?: return false
            if (owner.kind != IrConstKind.String) return false
            return owner.value == jdkMetafactoryHandle.owner || owner.value == jdkAltMetafactoryHandle.owner
        }

        fun getLambdaMetafactoryIndyImplFunctionRefOrNull(symbols: JvmSymbols, call: IrCall): IrRawFunctionReference? {
            if (!isLambdaMetafactoryIndy(symbols, call)) return null
            return (call.arguments[2] as IrVararg).elements[1] as IrRawFunctionReference
        }
    }

}
