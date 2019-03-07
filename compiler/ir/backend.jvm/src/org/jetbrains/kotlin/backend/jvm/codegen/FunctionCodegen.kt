/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDescriptorWithExtraFlags
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class FunctionCodegen(private val irFunction: IrFunction, private val classCodegen: ClassCodegen) {

    val state = classCodegen.state

    val descriptor = irFunction.descriptor

    fun generate(): JvmMethodGenericSignature =
        try {
            doGenerate()
        } catch (e: Throwable) {
            throw RuntimeException("${e.message} while generating code for:\n${irFunction.dump()}", e)
        }

    private fun doGenerate(): JvmMethodGenericSignature {
        val signature = classCodegen.typeMapper.mapSignatureWithGeneric(descriptor, OwnerKind.IMPLEMENTATION)

        val flags = calculateMethodFlags(irFunction.isStatic)
        val methodVisitor = createMethod(flags, signature)

        if (irFunction.origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            AnnotationCodegen(classCodegen, state, methodVisitor::visitAnnotation).genAnnotations(irFunction, signature.asmMethod.returnType)
            FunctionCodegen.generateParameterAnnotations(descriptor, methodVisitor, signature, DummyOldInnerClassConsumer(), state)
        }

        if (!state.classBuilderMode.generateBodies || flags.and(Opcodes.ACC_ABSTRACT) != 0 || irFunction.isExternal) {
            generateAnnotationDefaultValueIfNeeded(methodVisitor)
            methodVisitor.visitEnd()
        } else {
            val frameMap = createFrameMapWithReceivers(classCodegen.state, irFunction, signature)
            ExpressionCodegen(irFunction, frameMap, InstructionAdapter(methodVisitor), classCodegen).generate()
        }

        return signature
    }

    private fun calculateMethodFlags(isStatic: Boolean): Int {
        if (irFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            return Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC.let {
                if (irFunction is IrConstructor) it else it or Opcodes.ACC_BRIDGE or Opcodes.ACC_STATIC
            }
        }

        val visibility = AsmUtil.getVisibilityAccessFlag(irFunction.visibility) ?: error("Unmapped visibility ${irFunction.visibility}")
        val staticFlag = if (isStatic) Opcodes.ACC_STATIC else 0
        val varargFlag = if (irFunction.valueParameters.any { it.varargElementType != null }) Opcodes.ACC_VARARGS else 0
        val deprecation = if (irFunction.hasAnnotation(FQ_NAMES.deprecated)) Opcodes.ACC_DEPRECATED else 0
        val bridgeFlag = if (
            irFunction.origin == IrDeclarationOrigin.BRIDGE ||
            irFunction.origin == IrDeclarationOrigin.BRIDGE_SPECIAL
        ) Opcodes.ACC_BRIDGE else 0
        val modalityFlag = when ((irFunction as? IrSimpleFunction)?.modality) {
            Modality.FINAL -> if (!classCodegen.irClass.isAnnotationClass || irFunction.isStatic) Opcodes.ACC_FINAL else Opcodes.ACC_ABSTRACT
            Modality.ABSTRACT -> Opcodes.ACC_ABSTRACT
            else -> if (classCodegen.irClass.isJvmInterface && irFunction.body == null) Opcodes.ACC_ABSTRACT else 0 //TODO transform interface modality on lowering to DefaultImpls
        }
        val nativeFlag = if (irFunction.isExternal) Opcodes.ACC_NATIVE else 0
        val syntheticFlag = if (irFunction.origin.isSynthetic) Opcodes.ACC_SYNTHETIC else 0
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
        getAnnotationDefaultValueExpression()?.let { defaultValueExpression ->
            val annotationCodegen = AnnotationCodegen(classCodegen, state) { _, _ -> methodVisitor.visitAnnotationDefault() }
            annotationCodegen.generateAnnotationDefaultValue(defaultValueExpression)
        }
    }

    private fun getAnnotationDefaultValueExpression(): IrExpression? {
        if (!classCodegen.irClass.isAnnotationClass) return null
        // TODO: any simpler way to get to the value expression?
        // Are there other valid IR structures that represent the default value?
        return irFunction.safeAs<IrSimpleFunction>()
            ?.correspondingProperty
            ?.backingField
            ?.initializer.safeAs<IrExpressionBody>()
            ?.expression?.safeAs<IrGetValue>()
            ?.symbol?.owner?.safeAs<IrValueParameter>()
            ?.defaultValue?.safeAs<IrExpressionBody>()
            ?.expression
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

/**/// TODO: temporary, to allow calling the old FunctionCodegen.generateParameterAnnotations
private class DummyOldInnerClassConsumer()
    : org.jetbrains.kotlin.codegen.InnerClassConsumer {

    override fun addInnerClassInfoFromAnnotation(classDescriptor: ClassDescriptor) {

    }

}