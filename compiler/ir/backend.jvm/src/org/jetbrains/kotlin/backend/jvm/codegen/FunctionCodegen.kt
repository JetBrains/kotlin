/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.lower.DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDescriptorWithExtraFlags
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.dump
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

        FunctionCodegen.generateMethodAnnotations(descriptor, signature.asmMethod, methodVisitor, classCodegen, state.typeMapper)
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
        var flags = AsmUtil.getMethodAsmFlags(descriptor, OwnerKind.IMPLEMENTATION, state).or(if (isStatic) Opcodes.ACC_STATIC else 0).xor(
            if (classCodegen.irClass.isAnnotationClass) Opcodes.ACC_FINAL else 0/*TODO*/
        ).or(if (descriptor is JvmDescriptorWithExtraFlags) descriptor.extraFlags else 0)

        if (irFunction.origin == DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER) {
            flags = flags.xor(AsmUtil.getVisibilityAccessFlag(descriptor)).or(Opcodes.ACC_PUBLIC)
        }

        if (classCodegen.irClass.isJvmInterface && InitializersLowering.clinitName == irFunction.name) {
            //reset abstract flag for <clinit>
            flags = flags.xor(Opcodes.ACC_ABSTRACT)
        }
        return flags
    }

    protected open fun createMethod(flags: Int, signature: JvmMethodGenericSignature): MethodVisitor {
        return classCodegen.visitor.newMethod(
            irFunction.OtherOrigin,
            flags,
            signature.asmMethod.name, signature.asmMethod.descriptor,
            signature.genericsSignature, null/*TODO support exception*/
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
                    val annotationCodegen = AnnotationCodegen.forAnnotationDefaultValue(methodVisitor, classCodegen, state.typeMapper)
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
        frameMap.enter(parameter, state.typeMapper.mapType(parameter.type))
    }

    return frameMap
}