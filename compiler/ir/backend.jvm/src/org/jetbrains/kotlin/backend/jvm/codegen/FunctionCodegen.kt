/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.lower.BOUND_RECEIVER_PARAMETER
import org.jetbrains.kotlin.backend.common.lower.BOUND_VALUE_PARAMETER
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.MethodBodyVisitor
import org.jetbrains.kotlin.codegen.inline.SMAP
import org.jetbrains.kotlin.codegen.inline.SMAPAndMethodNode
import org.jetbrains.kotlin.codegen.inline.wrapWithMaxLocalCalc
import org.jetbrains.kotlin.codegen.mangleNameIfNeeded
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.visitAnnotableParameterCount
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.JVM_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.STRICTFP_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.SYNCHRONIZED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class FunctionCodegen(
    private val irFunction: IrFunction,
    private val classCodegen: ClassCodegen,
    private val inlinedInto: ExpressionCodegen? = null
) {
    private val context = classCodegen.context

    fun generate(): SMAPAndMethodNode =
        try {
            doGenerate()
        } catch (e: Throwable) {
            throw RuntimeException("Exception while generating code for:\n${irFunction.dump()}", e)
        }

    private fun doGenerate(): SMAPAndMethodNode {
        val signature = context.methodSignatureMapper.mapSignatureWithGeneric(irFunction)
        val flags = irFunction.calculateMethodFlags()
        val methodNode = MethodNode(
            Opcodes.API_VERSION,
            flags,
            signature.asmMethod.name,
            signature.asmMethod.descriptor,
            signature.genericsSignature.takeIf { flags.and(Opcodes.ACC_SYNTHETIC) == 0 },
            getThrownExceptions(irFunction)?.toTypedArray()
        )
        val methodVisitor: MethodVisitor = wrapWithMaxLocalCalc(methodNode)

        if (context.state.generateParametersMetadata && flags.and(Opcodes.ACC_SYNTHETIC) == 0) {
            generateParameterNames(irFunction, methodVisitor, signature, context.state)
        }

        if (irFunction.origin !in methodOriginsWithoutAnnotations) {
            val skipNullabilityAnnotations = flags and Opcodes.ACC_PRIVATE != 0 || flags and Opcodes.ACC_SYNTHETIC != 0
            object : AnnotationCodegen(classCodegen, context, skipNullabilityAnnotations) {
                override fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor {
                    return methodVisitor.visitAnnotation(descr, visible)
                }

                override fun visitTypeAnnotation(descr: String?, path: TypePath?, visible: Boolean): AnnotationVisitor {
                    return methodVisitor.visitTypeAnnotation(
                        TypeReference.newTypeReference(TypeReference.METHOD_RETURN).value, path, descr, visible
                    )
                }
            }.genAnnotations(irFunction, signature.asmMethod.returnType, irFunction.returnType)
            if (shouldGenerateAnnotationsOnValueParameters()) {
                generateParameterAnnotations(irFunction, methodVisitor, signature, classCodegen, context, skipNullabilityAnnotations)
            }
        }

        // `$$forInline` versions of suspend functions have the same bodies as the originals, but with different
        // name/flags/annotations and with no state machine.
        val notForInline = irFunction.suspendForInlineToOriginal()
        val smap = if (!context.state.classBuilderMode.generateBodies || flags.and(Opcodes.ACC_ABSTRACT) != 0 || irFunction.isExternal) {
            generateAnnotationDefaultValueIfNeeded(methodVisitor)
            SMAP(listOf())
        } else if (notForInline != null) {
            val (originalNode, smap) = classCodegen.generateMethodNode(notForInline)
            originalNode.accept(MethodBodyVisitor(methodVisitor))
            smap
        } else {
            val sourceMapper = context.getSourceMapper(classCodegen.irClass)
            val frameMap = irFunction.createFrameMapWithReceivers()
            context.state.globalInlineContext.enterDeclaration(irFunction.suspendFunctionOriginal().toIrBasedDescriptor())
            try {
                val adapter = InstructionAdapter(methodVisitor)
                ExpressionCodegen(irFunction, signature, frameMap, adapter, classCodegen, inlinedInto, sourceMapper).generate()
            } finally {
                context.state.globalInlineContext.exitDeclaration()
            }
            methodVisitor.visitMaxs(-1, -1)
            SMAP(sourceMapper.resultMappings)
        }
        methodVisitor.visitEnd()
        return SMAPAndMethodNode(methodNode, smap)
    }

    private fun shouldGenerateAnnotationsOnValueParameters(): Boolean =
        when {
            irFunction.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS ->
                false
            irFunction is IrConstructor && irFunction.parentAsClass.shouldNotGenerateConstructorParameterAnnotations() ->
                // Not generating parameter annotations for default stubs fixes KT-7892, though
                // this certainly looks like a workaround for a javac bug.
                false
            else ->
                true
        }

    // Since the only arguments to anonymous object constructors are captured variables and complex
    // super constructor arguments, there shouldn't be any annotations on them other than @NonNull,
    // and those are meaningless on synthetic parameters. (Also, the inliner cannot handle them and
    // will throw an exception if we generate any.)
    // The same applies for continuations.
    private fun IrClass.shouldNotGenerateConstructorParameterAnnotations() =
        isAnonymousObject || origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS || origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

    private fun IrFunction.getVisibilityForDefaultArgumentStub(): Int =
        when (visibility) {
            DescriptorVisibilities.PUBLIC -> Opcodes.ACC_PUBLIC
            JavaDescriptorVisibilities.PACKAGE_VISIBILITY -> AsmUtil.NO_FLAG_PACKAGE_PRIVATE
            else -> throw IllegalStateException("Default argument stub should be either public or package private: ${ir2string(this)}")
        }

    private fun IrFunction.calculateMethodFlags(): Int {
        if (origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            return getVisibilityForDefaultArgumentStub() or Opcodes.ACC_SYNTHETIC or functionDeprecationFlags.let {
                if (this is IrConstructor) it else it or Opcodes.ACC_STATIC
            }
        }

        val isVararg = valueParameters.lastOrNull()?.varargElementType != null
        val isBridge = origin == IrDeclarationOrigin.BRIDGE || origin == IrDeclarationOrigin.BRIDGE_SPECIAL
        val modalityFlag = when ((this as? IrSimpleFunction)?.modality) {
            Modality.FINAL -> when {
                origin == JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER -> 0
                origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER -> 0
                parentAsClass.isInterface && body != null -> 0
                parentAsClass.isAnnotationClass && !isStatic -> Opcodes.ACC_ABSTRACT
                else -> Opcodes.ACC_FINAL
            }
            Modality.ABSTRACT -> Opcodes.ACC_ABSTRACT
            // TODO transform interface modality on lowering to DefaultImpls
            else -> if (parentAsClass.isJvmInterface && body == null) Opcodes.ACC_ABSTRACT else 0
        }
        val isSynthetic = origin.isSynthetic || hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME) ||
                (isSuspend && DescriptorVisibilities.isPrivate(visibility) && !isInline) || isReifiable()
        val isStrict = hasAnnotation(STRICTFP_ANNOTATION_FQ_NAME)
        val isSynchronized = hasAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME)

        return getVisibilityAccessFlag() or modalityFlag or functionDeprecationFlags or
                (if (isStatic) Opcodes.ACC_STATIC else 0) or
                (if (isVararg) Opcodes.ACC_VARARGS else 0) or
                (if (isExternal) Opcodes.ACC_NATIVE else 0) or
                (if (isBridge) Opcodes.ACC_BRIDGE else 0) or
                (if (isSynthetic) Opcodes.ACC_SYNTHETIC else 0) or
                (if (isStrict) Opcodes.ACC_STRICT else 0) or
                (if (isSynchronized) Opcodes.ACC_SYNCHRONIZED else 0)
    }

    private fun getThrownExceptions(function: IrFunction): List<String>? {
        if (context.state.languageVersionSettings.supportsFeature(LanguageFeature.DoNotGenerateThrowsForDelegatedKotlinMembers) &&
            function.origin == IrDeclarationOrigin.DELEGATED_MEMBER
        ) return null

        // @Throws(vararg exceptionClasses: KClass<out Throwable>)
        val exceptionClasses = function.getAnnotation(JVM_THROWS_ANNOTATION_FQ_NAME)?.getValueArgument(0) ?: return null
        return (exceptionClasses as IrVararg).elements.map { exceptionClass ->
            context.typeMapper.mapType((exceptionClass as IrClassReference).classType).internalName
        }
    }

    private fun generateAnnotationDefaultValueIfNeeded(methodVisitor: MethodVisitor) {
        getAnnotationDefaultValueExpression()?.let { defaultValueExpression ->
            val annotationCodegen = object : AnnotationCodegen(classCodegen, context) {
                override fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor {
                    return methodVisitor.visitAnnotationDefault()
                }
            }
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

    private fun IrFunction.createFrameMapWithReceivers(): IrFrameMap {
        val frameMap = IrFrameMap()
        val receiver = if (this is IrConstructor) parentAsClass.thisReceiver else dispatchReceiverParameter
        receiver?.let {
            frameMap.enter(it, context.typeMapper.mapTypeAsDeclaration(it.type))
        }
        extensionReceiverParameter?.let {
            frameMap.enter(it, context.typeMapper.mapType(it))
        }
        for (parameter in valueParameters) {
            frameMap.enter(parameter, context.typeMapper.mapType(parameter.type))
        }
        return frameMap
    }

    // Borrowed from org.jetbrains.kotlin.codegen.FunctionCodegen.java
    private fun generateParameterAnnotations(
        irFunction: IrFunction,
        mv: MethodVisitor,
        jvmSignature: JvmMethodSignature,
        innerClassConsumer: InnerClassConsumer,
        context: JvmBackendContext,
        skipNullabilityAnnotations: Boolean = false
    ) {
        val iterator = irFunction.valueParameters.iterator()
        val kotlinParameterTypes = jvmSignature.valueParameters
        val syntheticParameterCount = kotlinParameterTypes.count { it.kind.isSkippedInGenericSignature }

        visitAnnotableParameterCount(mv, kotlinParameterTypes.size - syntheticParameterCount)

        kotlinParameterTypes.forEachIndexed { i, parameterSignature ->
            val kind = parameterSignature.kind
            val annotated = when (kind) {
                JvmMethodParameterKind.RECEIVER -> irFunction.extensionReceiverParameter
                else -> iterator.next()
            }

            if (annotated != null && !kind.isSkippedInGenericSignature && !annotated.isSyntheticMarkerParameter()) {
                object : AnnotationCodegen(innerClassConsumer, context, skipNullabilityAnnotations) {
                    override fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor {
                        return mv.visitParameterAnnotation(
                            i - syntheticParameterCount,
                            descr,
                            visible
                        )
                    }

                    override fun visitTypeAnnotation(descr: String?, path: TypePath?, visible: Boolean): AnnotationVisitor {
                        return mv.visitTypeAnnotation(
                            TypeReference.newFormalParameterReference(i - syntheticParameterCount).value,
                            path, descr, visible
                        )
                    }
                }.genAnnotations(annotated, parameterSignature.asmType, annotated.type)
            }
        }
    }

    companion object {
        internal val methodOriginsWithoutAnnotations =
            setOf(
                IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER,
                JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER,
                IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER,
                IrDeclarationOrigin.BRIDGE,
                IrDeclarationOrigin.BRIDGE_SPECIAL,
                JvmLoweredDeclarationOrigin.ABSTRACT_BRIDGE_STUB,
                JvmLoweredDeclarationOrigin.TO_ARRAY,
                IrDeclarationOrigin.IR_BUILTINS_STUB,
            )
    }
}


private fun IrValueParameter.isSyntheticMarkerParameter(): Boolean =
    origin == IrDeclarationOrigin.DEFAULT_CONSTRUCTOR_MARKER ||
            origin == JvmLoweredDeclarationOrigin.SYNTHETIC_MARKER_PARAMETER

private fun generateParameterNames(irFunction: IrFunction, mv: MethodVisitor, jvmSignature: JvmMethodSignature, state: GenerationState) {
    val iterator = irFunction.valueParameters.iterator()
    for (parameterSignature in jvmSignature.valueParameters) {
        val irParameter = when (parameterSignature.kind) {
            JvmMethodParameterKind.RECEIVER -> irFunction.extensionReceiverParameter!!
            else -> iterator.next()
        }
        val name = when (parameterSignature.kind) {
            JvmMethodParameterKind.RECEIVER -> getNameForReceiverParameter(irFunction, state)
            else -> irParameter.name.asString()
        }
        // A construct emitted by a Java compiler must be marked as synthetic if it does not correspond to a construct declared
        // explicitly or implicitly in source code, unless the emitted construct is a class initialization method (JVMS §2.9).
        // A construct emitted by a Java compiler must be marked as mandated if it corresponds to a formal parameter
        // declared implicitly in source code (§8.8.1, §8.8.9, §8.9.3, §15.9.5.1).
        val access = when {
            irParameter == irFunction.extensionReceiverParameter -> Opcodes.ACC_MANDATED
            irParameter.origin == JvmLoweredDeclarationOrigin.FIELD_FOR_OUTER_THIS -> Opcodes.ACC_MANDATED
            // TODO mark these backend-common origins as synthetic? (note: ExpressionCodegen is still expected
            //      to generate LVT entries for them)
            irParameter.origin == IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER -> Opcodes.ACC_MANDATED
            irParameter.origin == IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER -> Opcodes.ACC_SYNTHETIC
            irParameter.origin == BOUND_VALUE_PARAMETER -> Opcodes.ACC_SYNTHETIC
            irParameter.origin == BOUND_RECEIVER_PARAMETER -> Opcodes.ACC_SYNTHETIC
            irParameter.origin.isSynthetic -> Opcodes.ACC_SYNTHETIC
            else -> 0
        }
        mv.visitParameter(name, access)
    }
}

private fun getNameForReceiverParameter(irFunction: IrFunction, state: GenerationState): String {
    if (!state.languageVersionSettings.supportsFeature(LanguageFeature.NewCapturedReceiverFieldNamingConvention)) {
        return AsmUtil.RECEIVER_PARAMETER_NAME
    }

    // Current codegen never touches CALL_LABEL_FOR_LAMBDA_ARGUMENT
//    if (irFunction is IrSimpleFunction) {
//        val labelName = bindingContext.get(CodegenBinding.CALL_LABEL_FOR_LAMBDA_ARGUMENT, irFunction.descriptor)
//        if (labelName != null) {
//            return getLabeledThisName(labelName, prefix, defaultName)
//        }
//    }

    val callableName = irFunction.safeAs<IrSimpleFunction>()?.correspondingPropertySymbol?.owner?.name ?: irFunction.name
    return if (callableName.isSpecial || !Name.isValidIdentifier(callableName.asString()))
        AsmUtil.RECEIVER_PARAMETER_NAME
    else
        AsmUtil.LABELED_THIS_PARAMETER + mangleNameIfNeeded(callableName.asString())
}
