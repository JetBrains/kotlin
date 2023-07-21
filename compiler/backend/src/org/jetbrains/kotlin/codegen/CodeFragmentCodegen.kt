/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.codegen.CalculatedCodeFragmentCodegenInfo.CalculatedParameter
import org.jetbrains.kotlin.codegen.CodeFragmentCodegenInfo.IParameter
import org.jetbrains.kotlin.codegen.binding.MutableClosure
import org.jetbrains.kotlin.codegen.context.*
import org.jetbrains.kotlin.codegen.context.LocalLookup.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.Companion.NO_ORIGIN
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension.Context as InCo

class CodeFragmentCodegenInfo(
    val classDescriptor: ClassDescriptor,
    val methodDescriptor: FunctionDescriptor,
    val parameters: List<IParameter>
) {
    val classType: Type = Type.getObjectType(classDescriptor.name.asString())

    interface IParameter {
        val targetDescriptor: DeclarationDescriptor
        val targetType: KotlinType
        val isLValue: Boolean
    }
}

class CodeFragmentCodegen private constructor(
    private val codeFragment: KtCodeFragment,
    private val info: CodeFragmentCodegenInfo,
    private val calculatedInfo: CalculatedCodeFragmentCodegenInfo,
    private val classContext: CodeFragmentContext,
    state: GenerationState,
    builder: ClassBuilder
) : MemberCodegen<KtCodeFragment>(state, null, classContext, codeFragment, builder) {
    private val methodDescriptor = info.methodDescriptor

    override fun generateDeclaration() {
        v.defineClass(
            codeFragment,
            state.config.classFileVersion,
            ACC_PUBLIC or ACC_SUPER,
            info.classType.internalName,
            null,
            "java/lang/Object",
            emptyArray()
        )
    }

    override fun generateBody() {
        genConstructor()

        val methodContext = object : MethodContext(methodDescriptor, classContext.contextKind, classContext, null, false) {
            override fun <D : CallableMemberDescriptor> getAccessorForSuperCallIfNeeded(
                descriptor: D,
                superCallTarget: ClassDescriptor?,
                state: GenerationState
            ): D {
                return descriptor
            }
        }

        genMethod(methodContext)
    }

    override fun generateKotlinMetadataAnnotation() {
        writeSyntheticClassMetadata(v, state, false)
    }

    private fun genConstructor() {
        val mv = v.newMethod(NO_ORIGIN, ACC_PUBLIC, "<init>", "()V", null, null)

        if (state.classBuilderMode.generateBodies) {
            mv.visitCode()

            val iv = InstructionAdapter(mv)
            iv.load(0, info.classType)
            iv.invokespecial("java/lang/Object", "<init>", "()V", false)
            iv.areturn(Type.VOID_TYPE)
        }

        mv.visitMaxs(-1, -1)
        mv.visitEnd()
    }

    private fun genMethod(methodContext: MethodContext) {
        val returnType = calculatedInfo.returnAsmType
        val parameters = calculatedInfo.parameters

        val methodDesc = Type.getMethodDescriptor(returnType, *parameters.map { it.asmType }.toTypedArray())

        val mv = v.newMethod(
            OtherOrigin(codeFragment, methodContext.functionDescriptor),
            ACC_PUBLIC or ACC_STATIC,
            methodDescriptor.name.asString(), methodDesc,
            null, null
        )

        if (state.classBuilderMode.generateBodies) {
            mv.visitCode()

            val frameMap = FrameMap()
            parameters.forEach { frameMap.enter(it.targetDescriptor, it.asmType) }

            val codegen = object : ExpressionCodegen(mv, frameMap, returnType, methodContext, state, this) {
                override fun findCapturedValue(descriptor: DeclarationDescriptor): StackValue? {
                    val parameter = calculatedInfo.findParameter(descriptor) ?: return super.findCapturedValue(descriptor)
                    return parameter.stackValue
                }

                override fun generateThisOrOuter(calleeContainingClass: ClassDescriptor, isSuper: Boolean): StackValue {
                    findCapturedValue(calleeContainingClass)?.let { return it }
                    return super.generateThisOrOuter(calleeContainingClass, isSuper)
                }

                override fun generateExtensionReceiver(descriptor: CallableDescriptor): StackValue {
                    val receiverParameter = descriptor.extensionReceiverParameter
                    if (receiverParameter != null) {
                        findCapturedValue(receiverParameter)?.let { return it }
                    }

                    return super.generateExtensionReceiver(descriptor)
                }

                override fun generateNonIntrinsicSimpleNameExpression(
                    expression: KtSimpleNameExpression, receiver: StackValue,
                    descriptor: DeclarationDescriptor, resolvedCall: ResolvedCall<*>?, isSyntheticField: Boolean
                ): StackValue {
                    val resultingDescriptor = resolvedCall?.resultingDescriptor
                    if (resultingDescriptor != null) {
                        findCapturedValue(resultingDescriptor)?.let { return it }
                    }
                    return super.generateNonIntrinsicSimpleNameExpression(expression, receiver, descriptor, resolvedCall, isSyntheticField)
                }

                override fun visitThisExpression(expression: KtThisExpression, receiver: StackValue?): StackValue {
                    val instanceReference = expression.instanceReference
                    val target = bindingContext[BindingContext.REFERENCE_TARGET, instanceReference]
                    if (target != null) {
                        findCapturedValue(target)?.let { return it }
                    }

                    return super.visitThisExpression(expression, receiver)
                }
            }

            codegen.gen(codeFragment.getContentElement(), returnType)
            codegen.v.areturn(returnType)

            parameters.asReversed().forEach { frameMap.leave(it.targetDescriptor) }
        }

        mv.visitMaxs(-1, -1)
        mv.visitEnd()
    }

    companion object {
        private val INFO_USERDATA_KEY = Key.create<CodeFragmentCodegenInfo>("CODE_FRAGMENT_CODEGEN_INFO")

        fun setCodeFragmentInfo(codeFragment: KtCodeFragment, info: CodeFragmentCodegenInfo) {
            codeFragment.putUserData(INFO_USERDATA_KEY, info)
        }

        fun clearCodeFragmentInfo(codeFragment: KtCodeFragment) {
            codeFragment.putUserData(INFO_USERDATA_KEY, null)
        }

        @JvmStatic
        fun getCodeFragmentInfo(codeFragment: KtCodeFragment): CodeFragmentCodegenInfo {
            return codeFragment.getUserData(INFO_USERDATA_KEY) ?: error("Codegen info user data is not set")
        }

        @JvmStatic
        fun createCodegen(
            declaration: KtCodeFragment,
            state: GenerationState,
            parentContext: CodegenContext<*>
        ): CodeFragmentCodegen {
            val info = getCodeFragmentInfo(declaration)
            val classDescriptor = info.classDescriptor
            val builder = state.factory.newVisitor(OtherOrigin(declaration, classDescriptor), info.classType, declaration.containingFile)
            val calculatedInfo = calculateInfo(info, state.typeMapper)
            val classContext = CodeFragmentContext(state.typeMapper, classDescriptor, parentContext, calculatedInfo)
            return CodeFragmentCodegen(declaration, info, calculatedInfo, classContext, state, builder)
        }

        private fun calculateInfo(info: CodeFragmentCodegenInfo, typeMapper: KotlinTypeMapper): CalculatedCodeFragmentCodegenInfo {
            val methodSignature = typeMapper.mapSignatureSkipGeneric(info.methodDescriptor)
            require(info.parameters.size == methodSignature.valueParameters.size)
            require(info.parameters.size == info.methodDescriptor.valueParameters.size)

            var stackIndex = 0
            val parameters = mutableListOf<CalculatedParameter>()

            for (parameterIndex in 0 until info.parameters.size) {
                val parameter = info.parameters[parameterIndex]
                val asmParameter = methodSignature.valueParameters[parameterIndex]
                val parameterDescriptor = info.methodDescriptor.valueParameters[parameterIndex]

                val asmType: Type
                val stackValue: StackValue

                val sharedAsmType = getSharedTypeIfApplicable(parameter, typeMapper)
                if (sharedAsmType != null) {
                    asmType = sharedAsmType
                    val unwrappedType = typeMapper.mapType(parameter.targetType)
                    stackValue = StackValue.shared(stackIndex, unwrappedType)
                } else {
                    asmType = asmParameter.asmType
                    stackValue = StackValue.local(stackIndex, asmType)
                }

                val calculatedParameter = CalculatedParameter(parameter, asmType, stackValue, parameterDescriptor)
                parameters += calculatedParameter

                stackIndex += if (asmType == Type.DOUBLE_TYPE || asmType == Type.LONG_TYPE) 2 else 1
            }

            return CalculatedCodeFragmentCodegenInfo(parameters, methodSignature.returnType)
        }

        fun getSharedTypeIfApplicable(parameter: IParameter, typeMapper: KotlinTypeMapper): Type? {
            return when (val descriptor = parameter.targetDescriptor) {
                is LocalVariableDescriptor -> {
                    var result = typeMapper.getSharedVarType(descriptor)
                    if (result == null && parameter.isLValue) {
                        result = StackValue.sharedTypeForType(typeMapper.mapType(descriptor.type))
                    }
                    result
                }
                else -> null
            }
        }
    }
}

private class CalculatedCodeFragmentCodegenInfo(val parameters: List<CalculatedParameter>, val returnAsmType: Type) {
    class CalculatedParameter(
        parameter: IParameter,
        val asmType: Type,
        val stackValue: StackValue,
        val parameterDescriptor: ValueParameterDescriptor
    ) : IParameter by parameter

    fun findParameter(target: DeclarationDescriptor): CalculatedParameter? {
        for (parameter in parameters) {
            if (parameter.targetDescriptor == target || parameter.parameterDescriptor == target) {
                return parameter
            }
        }

        return null
    }
}

private class CodeFragmentContext(
    typeMapper: KotlinTypeMapper,
    contextDescriptor: ClassDescriptor,
    parentContext: CodegenContext<*>?,
    private val calculatedInfo: CalculatedCodeFragmentCodegenInfo
) : ScriptLikeContext(typeMapper, contextDescriptor, parentContext) {
    private val localLookup = object : LocalLookup {
        override fun isLocal(descriptor: DeclarationDescriptor?): Boolean {
            return calculatedInfo.parameters.any { descriptor == it.targetDescriptor || descriptor == it.parameterDescriptor }
        }
    }

    override fun getOuterReceiverExpression(prefix: StackValue?, thisOrOuterClass: ClassDescriptor): StackValue {
        val parameter = calculatedInfo.findParameter(thisOrOuterClass)
            ?: throw IllegalStateException("Can not generate outer receiver value for $thisOrOuterClass")

        return parameter.stackValue
    }

    override fun captureVariable(closure: MutableClosure, target: DeclarationDescriptor): StackValue? {
        val parameter = calculatedInfo.findParameter(target) ?: return null
        val parameterDescriptor = parameter.parameterDescriptor

        // Value is already captured
        closure.captureVariables[parameterDescriptor]?.let { return it.innerValue }

        // Capture new value
        val closureAsmType = typeMapper.mapType(closure.closureClass)
        return LocalLookupCase.VAR.innerValue(parameterDescriptor, localLookup, state, closure, closureAsmType)
    }
}
