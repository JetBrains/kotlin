/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.visitAnnotableParameterCount
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.STRICTFP_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.SYNCHRONIZED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class FunctionCodegen(
    private val irFunction: IrFunction,
    private val classCodegen: ClassCodegen,
    private val isInlineLambda: Boolean = false
) {
    val context = classCodegen.context
    val state = classCodegen.state

    fun generate(): JvmMethodGenericSignature =
        try {
            doGenerate()
        } catch (e: Throwable) {
            throw RuntimeException("Exception while generating code for:\n${irFunction.dump()}", e)
        }

    private fun doGenerate(): JvmMethodGenericSignature {
        val signature = classCodegen.methodSignatureMapper.mapSignatureWithGeneric(irFunction, OwnerKind.IMPLEMENTATION)

        val flags = calculateMethodFlags(irFunction.isStatic)
        var methodVisitor = createMethod(flags, signature)

        val hasSyntheticFlag = flags.and(Opcodes.ACC_SYNTHETIC) != 0
        if (state.generateParametersMetadata && !hasSyntheticFlag) {
            generateParameterNames(irFunction, methodVisitor, signature, state)
        }

        if (irFunction.origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            AnnotationCodegen(classCodegen, context, methodVisitor::visitAnnotation).genAnnotations(
                irFunction,
                signature.asmMethod.returnType
            )
        }

        // FIXME: The following test is a workaround for a bug in anonymous object regeneration.
        //        We currently need to avoid parameter annotations on the (synthetic) constructors of inlined anonymous objects,
        //        since otherwise anonymous object regeneration can fail with an ArrayIndexOutOfBounds exception if the number
        //        or arguments to the constructor changes.
        if (!hasSyntheticFlag ||
            irFunction.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS ||
            //TODO: investigate this case: annotation here is generated twice in lowered function and in interface method overload
            irFunction.origin == JvmLoweredDeclarationOrigin.GENERATED_SAM_IMPLEMENTATION
        ) {
            generateParameterAnnotations(irFunction, methodVisitor, signature, classCodegen, context)
        }

        if (!state.classBuilderMode.generateBodies || flags.and(Opcodes.ACC_ABSTRACT) != 0 || irFunction.isExternal) {
            generateAnnotationDefaultValueIfNeeded(methodVisitor)
        } else {
            val frameMap = createFrameMapWithReceivers(signature)
            val irClass = context.suspendFunctionContinuations[irFunction]
            val element = (irFunction.symbol.descriptor.psiElement
                ?: context.suspendLambdaToOriginalFunctionMap[irFunction.parent]?.symbol?.descriptor?.psiElement) as? KtElement
            val continuationClassBuilder = context.continuationClassBuilders[irClass]
            methodVisitor = when {
                irFunction.isSuspend -> generateStateMachineForNamedFunction(
                    irFunction, classCodegen, methodVisitor, flags, signature, continuationClassBuilder, element!!
                )
                irFunction.isInvokeSuspendOfLambda(context) -> generateStateMachineForLambda(
                    classCodegen, methodVisitor, flags, signature, element!!
                )
                else -> methodVisitor
            }
            ExpressionCodegen(irFunction, frameMap, InstructionAdapter(methodVisitor), classCodegen, isInlineLambda).generate()
            methodVisitor.visitMaxs(-1, -1)
            continuationClassBuilder?.done()
        }
        methodVisitor.visitEnd()

        return signature
    }

    private fun calculateMethodFlags(isStatic: Boolean): Int {
        if (irFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            return Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC.let {
                if (irFunction is IrConstructor) it else it or Opcodes.ACC_STATIC
            }
        }

        val visibility = AsmUtil.getVisibilityAccessFlag(irFunction.visibility) ?: error("Unmapped visibility ${irFunction.visibility}")
        val staticFlag = if (isStatic) Opcodes.ACC_STATIC else 0
        val varargFlag = if (irFunction.valueParameters.any { it.varargElementType != null }) Opcodes.ACC_VARARGS else 0
        val deprecation = irFunction.deprecationFlags
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
        val syntheticFlag =
            if (irFunction.origin.isSynthetic || irFunction.hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)) Opcodes.ACC_SYNTHETIC
            else 0
        val strictFpFlag = if (irFunction.hasAnnotation(STRICTFP_ANNOTATION_FQ_NAME)) Opcodes.ACC_STRICT else 0
        val synchronizedFlag = if (irFunction.hasAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME)) Opcodes.ACC_SYNCHRONIZED else 0

        return visibility or
                modalityFlag or
                staticFlag or
                varargFlag or
                deprecation or
                nativeFlag or
                bridgeFlag or
                syntheticFlag or
                strictFpFlag or
                synchronizedFlag
    }

    protected open fun createMethod(flags: Int, signature: JvmMethodGenericSignature): MethodVisitor {
        // @Throws(vararg exceptionClasses: KClass<out Throwable>)
        val exceptions = irFunction.getAnnotation(FqName("kotlin.jvm.Throws"))?.getValueArgument(0)?.let {
            (it as IrVararg).elements.map { exceptionClass ->
                classCodegen.typeMapper.mapType((exceptionClass as IrClassReference).classType).internalName
            }.toTypedArray()
        }

        return classCodegen.visitor.newMethod(
            irFunction.OtherOrigin,
            flags,
            signature.asmMethod.name, signature.asmMethod.descriptor,
            if (flags.and(Opcodes.ACC_SYNTHETIC) != 0) null else signature.genericsSignature,
            exceptions
        )
    }

    private fun generateAnnotationDefaultValueIfNeeded(methodVisitor: MethodVisitor) {
        getAnnotationDefaultValueExpression()?.let { defaultValueExpression ->
            val annotationCodegen = AnnotationCodegen(classCodegen, context) { _, _ -> methodVisitor.visitAnnotationDefault() }
            annotationCodegen.generateAnnotationDefaultValue(defaultValueExpression)
        }
    }

    private fun getAnnotationDefaultValueExpression(): IrExpression? {
        if (!classCodegen.irClass.isAnnotationClass) return null
        // TODO: any simpler way to get to the value expression?
        // Are there other valid IR structures that represent the default value?
        return irFunction.safeAs<IrSimpleFunction>()
            ?.correspondingPropertySymbol?.owner
            ?.backingField
            ?.initializer.safeAs<IrExpressionBody>()
            ?.expression?.safeAs<IrGetValue>()
            ?.symbol?.owner?.safeAs<IrValueParameter>()
            ?.defaultValue?.safeAs<IrExpressionBody>()
            ?.expression
    }

    private fun IrFrameMap.enterDispatchReceiver(parameter: IrValueParameter) {
        val type = classCodegen.typeMapper.mapTypeAsDeclaration(parameter.type)
        enter(parameter, type)
    }

    private fun createFrameMapWithReceivers(signature: JvmMethodSignature): IrFrameMap {
        val frameMap = IrFrameMap()
        if (irFunction is IrConstructor) {
            frameMap.enterDispatchReceiver(irFunction.constructedClass.thisReceiver!!)
        } else if (irFunction.dispatchReceiverParameter != null) {
            frameMap.enterDispatchReceiver(irFunction.dispatchReceiverParameter!!)
        }

        for (parameter in signature.valueParameters) {
            if (parameter.kind == JvmMethodParameterKind.RECEIVER) {
                val receiverParameter = irFunction.extensionReceiverParameter
                if (receiverParameter != null) {
                    frameMap.enter(receiverParameter, classCodegen.typeMapper.mapType(receiverParameter))
                } else {
                    frameMap.enterTemp(parameter.asmType)
                }
            } else if (parameter.kind != JvmMethodParameterKind.VALUE) {
                frameMap.enterTemp(parameter.asmType)
            }
        }

        for (parameter in irFunction.valueParameters) {
            frameMap.enter(parameter, classCodegen.typeMapper.mapType(parameter.type))
        }

        return frameMap
    }
}

// Borrowed from org.jetbrains.kotlin.codegen.FunctionCodegen.java
fun generateParameterAnnotations(
    irFunction: IrFunction,
    mv: MethodVisitor,
    jvmSignature: JvmMethodSignature,
    innerClassConsumer: InnerClassConsumer,
    context: JvmBackendContext
) {
    val iterator = irFunction.valueParameters.iterator()
    val kotlinParameterTypes = jvmSignature.valueParameters
    var syntheticParameterCount = 0
    kotlinParameterTypes.forEachIndexed { i, parameterSignature ->
        val kind = parameterSignature.kind
        if (kind.isSkippedInGenericSignature) {
            if (AsmUtil.IS_BUILT_WITH_ASM6) {
                markEnumOrInnerConstructorParameterAsSynthetic(mv, i, ClassBuilderMode.FULL)
            } else {
                syntheticParameterCount++
            }
        }
    }
    if (!AsmUtil.IS_BUILT_WITH_ASM6) {
        visitAnnotableParameterCount(mv, kotlinParameterTypes.size - syntheticParameterCount)
    }

    kotlinParameterTypes.forEachIndexed { i, parameterSignature ->
        val kind = parameterSignature.kind
        if (kind.isSkippedInGenericSignature) return@forEachIndexed

        val annotated = when (kind) {
            JvmMethodParameterKind.VALUE -> iterator.next()
            JvmMethodParameterKind.RECEIVER -> irFunction.extensionReceiverParameter
            else -> null
        }

        if (annotated != null) {
            AnnotationCodegen(innerClassConsumer, context) { descriptor, visible ->
                mv.visitParameterAnnotation(
                    if (AsmUtil.IS_BUILT_WITH_ASM6) i else i - syntheticParameterCount,
                    descriptor,
                    visible
                )
            }.genAnnotations(annotated, parameterSignature.asmType)
        }
    }
}

private fun markEnumOrInnerConstructorParameterAsSynthetic(mv: MethodVisitor, i: Int, mode: ClassBuilderMode) {
    // IDEA's ClsPsi builder fails to annotate synthetic parameters
    if (mode === ClassBuilderMode.LIGHT_CLASSES) return

    // This is needed to avoid RuntimeInvisibleParameterAnnotations error in javac:
    // see MethodWriter.visitParameterAnnotation()

    val av = mv.visitParameterAnnotation(i, "Ljava/lang/Synthetic;", true)
    av?.visitEnd()
}
