/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.lower.DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDescriptorWithExtraFlags
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class FunctionCodegen(private val irFunction: IrFunction, private val classCodegen: ClassCodegen) {

    val state = classCodegen.state

    val descriptor = irFunction.descriptor

    fun generate() {
        try {
            doGenerate()
        } catch (e: Throwable) {
            throw RuntimeException("${e.message} while generating code for:\n${irFunction.dump()}", e)
        }
    }

    private fun doGenerate() {
        val signature = classCodegen.typeMapper.mapSignatureWithGeneric(descriptor, OwnerKind.IMPLEMENTATION)

        val flags = calculateMethodFlags(irFunction.isStatic)
        val methodVisitor = createMethod(flags, signature)

        AnnotationCodegen.forMethod(methodVisitor, classCodegen, state).genAnnotations(descriptor, signature.asmMethod.returnType)
        FunctionCodegen.generateParameterAnnotations(descriptor, methodVisitor, signature, classCodegen, state)

        if (!state.classBuilderMode.generateBodies || flags.and(Opcodes.ACC_ABSTRACT) != 0 || irFunction.isExternal) {
            generateAnnotationDefaultValueIfNeeded(methodVisitor)
            methodVisitor.visitEnd()
            return
        }

        val frameMap = createFrameMapWithReceivers(classCodegen.state, irFunction, signature)
        ExpressionCodegen(irFunction, frameMap, InstructionAdapter(methodVisitor), classCodegen).generate()
    }

    private fun calculateMethodFlags(isStatic: Boolean): Int {
        if (irFunction.origin == DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER) {
            return Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC.let {
                if (irFunction is IrConstructor) it else it or Opcodes.ACC_BRIDGE or Opcodes.ACC_STATIC
            }
        }

        val visibility = AsmUtil.getVisibilityAccessFlag(irFunction.visibility) ?: error("Unmapped visibility ${irFunction.visibility}")
        val staticFlag = if (isStatic) Opcodes.ACC_STATIC else 0
        val varargFlag = if (irFunction.valueParameters.any { it.varargElementType != null }) Opcodes.ACC_VARARGS else 0
        val deprecation = if (irFunction.hasAnnotation(FQ_NAMES.deprecated)) Opcodes.ACC_DEPRECATED else 0
        val bridgeFlag = 0 //TODO
        val modalityFlag = when ((irFunction as? IrSimpleFunction)?.modality) {
            Modality.FINAL -> if (!classCodegen.irClass.isAnnotationClass || irFunction.isStatic) Opcodes.ACC_FINAL else Opcodes.ACC_ABSTRACT
            Modality.ABSTRACT -> Opcodes.ACC_ABSTRACT
            else -> if (classCodegen.irClass.isJvmInterface && irFunction.body == null) Opcodes.ACC_ABSTRACT else 0 //TODO transform interface modality on lowering to DefaultImpls
        }
        val nativeFlag = if (irFunction.isExternal) Opcodes.ACC_NATIVE else 0
        val syntheticFlag = if (irFunction.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR) Opcodes.ACC_SYNTHETIC else 0
        return visibility or
                modalityFlag or
                staticFlag or
                varargFlag or
                deprecation or
                nativeFlag or
                bridgeFlag or
                syntheticFlag or
                (if (descriptor is JvmDescriptorWithExtraFlags) descriptor.extraFlags else 0)
    }

    protected open fun createMethod(flags: Int, signature: JvmMethodGenericSignature): MethodVisitor {
        return classCodegen.visitor.newMethod(
            irFunction.OtherOrigin,
            flags,
            signature.asmMethod.name, signature.asmMethod.descriptor,
            if (irFunction.origin == IrDeclarationOrigin.BRIDGE) null else signature.genericsSignature,
            null/*TODO support exception*/
        )
    }

    private fun generateAnnotationDefaultValueIfNeeded(methodVisitor: MethodVisitor) {
        if (classCodegen.irClass.isAnnotationClass) {
            val source = JvmCodegenUtil.getDirectMember(descriptor).source
            (source.getPsi() as? KtParameter)?.defaultValue?.apply {
                val defaultValue = this
                val constant = org.jetbrains.kotlin.codegen.ExpressionCodegen.getCompileTimeConstant(
                    defaultValue, state.bindingContext, true, state.shouldInlineConstVals
                )
                assert(!state.classBuilderMode.generateBodies || constant != null) { "Default value for annotation parameter should be compile time value: " + defaultValue.text }
                if (constant != null) {
                    val annotationCodegen = AnnotationCodegen.forAnnotationDefaultValue(methodVisitor, classCodegen, state)
                    annotationCodegen.generateAnnotationDefaultValue(constant, descriptor.returnType!!)
                }
            }
        }
    }
}

private fun createFrameMapWithReceivers(
    state: GenerationState,
    irFunction: IrFunction,
    signature: JvmMethodSignature
): IrFrameMap {
    val frameMap = IrFrameMap()
    if (irFunction is IrConstructor) {
        frameMap.enter((irFunction.parent as IrClass).thisReceiver!!, AsmTypes.OBJECT_TYPE)
    } else if (irFunction.dispatchReceiverParameter != null) {
        frameMap.enter(irFunction.dispatchReceiverParameter!!, AsmTypes.OBJECT_TYPE)
    }

    for (parameter in signature.valueParameters) {
        if (parameter.kind == JvmMethodParameterKind.RECEIVER) {
            val receiverParameter = irFunction.extensionReceiverParameter
            if (receiverParameter?.descriptor != null) {
                frameMap.enter(receiverParameter, state.typeMapper.mapType(receiverParameter.descriptor))
            } else {
                frameMap.enterTemp(parameter.asmType)
            }
        } else if (parameter.kind != JvmMethodParameterKind.VALUE) {
            frameMap.enterTemp(parameter.asmType)
        }
    }

    for (parameter in irFunction.valueParameters) {
        frameMap.enter(parameter, state.typeMapper.mapType(parameter.type.toKotlinType()))
    }

    return frameMap
}