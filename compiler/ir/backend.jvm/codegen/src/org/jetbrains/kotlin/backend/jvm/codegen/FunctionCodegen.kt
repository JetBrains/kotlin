/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.backend.common.lower.BOUND_RECEIVER_PARAMETER
import org.jetbrains.kotlin.backend.common.lower.BOUND_VALUE_PARAMETER
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.mapping.mapType
import org.jetbrains.kotlin.backend.jvm.mapping.mapTypeAsDeclaration
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.visitAnnotableParameterCount
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.JvmNames.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.JvmNames.STRICTFP_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.JvmNames.SYNCHRONIZED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.JVM_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class FunctionCodegen(private val irFunction: IrFunction, private val classCodegen: ClassCodegen) {
    private val context = classCodegen.context

    fun generate(
        reifiedTypeParameters: ReifiedTypeParametersUsages = classCodegen.reifiedTypeParametersUsages
    ): SMAPAndMethodNode =
        try {
            doGenerate(reifiedTypeParameters)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            throw RuntimeException("Exception while generating code for:\n${irFunction.dump()}", e)
        }

    private fun doGenerate(reifiedTypeParameters: ReifiedTypeParametersUsages): SMAPAndMethodNode {
        val signature = classCodegen.methodSignatureMapper.mapSignatureWithGeneric(irFunction)
        val flags = irFunction.calculateMethodFlags()
        val isSynthetic = flags.and(Opcodes.ACC_SYNTHETIC) != 0
        val methodNode = MethodNode(
            Opcodes.API_VERSION,
            flags,
            signature.asmMethod.name,
            signature.asmMethod.descriptor,
            signature.genericsSignature
                .takeIf {
                    (irFunction.isInline && irFunction.origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) ||
                            (!isSynthetic && irFunction.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) ||
                            (irFunction.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION)
                },
            getThrownExceptions(irFunction)?.toTypedArray()
        )
        val methodVisitor: MethodVisitor = wrapWithMaxLocalCalc(methodNode)

        if (context.state.generateParametersMetadata && !isSynthetic) {
            generateParameterNames(irFunction, methodVisitor, context.state)
        }

        if (irFunction.isWithAnnotations) {
            val skipNullabilityAnnotations = flags and Opcodes.ACC_PRIVATE != 0 || flags and Opcodes.ACC_SYNTHETIC != 0
            object : AnnotationCodegen(classCodegen, skipNullabilityAnnotations) {
                override fun visitAnnotation(descr: String, visible: Boolean): AnnotationVisitor {
                    return methodVisitor.visitAnnotation(descr, visible)
                }

                override fun visitTypeAnnotation(descr: String, path: TypePath?, visible: Boolean): AnnotationVisitor {
                    return methodVisitor.visitTypeAnnotation(
                        TypeReference.newTypeReference(TypeReference.METHOD_RETURN).value, path, descr, visible
                    )
                }
            }.genAnnotations(irFunction, signature.asmMethod.returnType, irFunction.returnType)

            AnnotationCodegen.genAnnotationsOnTypeParametersAndBounds(
                context,
                irFunction,
                classCodegen,
                TypeReference.METHOD_TYPE_PARAMETER,
                TypeReference.METHOD_TYPE_PARAMETER_BOUND
            ) { typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean ->
                methodVisitor.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
            }

            if (shouldGenerateAnnotationsOnValueParameters()) {
                generateParameterAnnotations(irFunction, methodVisitor, signature, classCodegen, skipNullabilityAnnotations)
            }
        }

        // `$$forInline` versions of suspend functions have the same bodies as the originals, but with different
        // name/flags/annotations and with no state machine.
        val notForInline = irFunction.suspendForInlineToOriginal()
        val smap = if (flags.and(Opcodes.ACC_ABSTRACT) != 0 || irFunction.isExternal) {
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
                ExpressionCodegen(irFunction, signature, frameMap, adapter, classCodegen, sourceMapper, reifiedTypeParameters).generate()
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
            irFunction.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS ->
                false
            irFunction is IrConstructor && irFunction.parentAsClass.shouldNotGenerateConstructorParameterAnnotations() ->
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
        when {
            // TODO: maybe best to generate private default in interface as private
            visibility == DescriptorVisibilities.PUBLIC || parentAsClass.isJvmInterface -> Opcodes.ACC_PUBLIC
            visibility == JavaDescriptorVisibilities.PACKAGE_VISIBILITY -> AsmUtil.NO_FLAG_PACKAGE_PRIVATE
            else -> throw IllegalStateException("Default argument stub should be either public or package private: ${ir2string(this)}")
        }

    private fun IrFunction.calculateMethodFlags(): Int {
        if (origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            return getVisibilityForDefaultArgumentStub() or Opcodes.ACC_SYNTHETIC or
                    (if (isDeprecatedFunction(context)) Opcodes.ACC_DEPRECATED else 0) or
                    (if (this is IrConstructor) 0 else Opcodes.ACC_STATIC)
        }

        val isVararg = valueParameters.lastOrNull()?.varargElementType != null && !isBridge()
        val modalityFlag =
            if (parentAsClass.isAnnotationClass) {
                if (isStatic) 0 else Opcodes.ACC_ABSTRACT
            } else {
                when ((this as? IrSimpleFunction)?.modality) {
                    Modality.FINAL -> when {
                        origin == JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER -> 0
                        origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER -> 0
                        parentAsClass.isInterface && body != null -> 0
                        else -> Opcodes.ACC_FINAL
                    }
                    Modality.ABSTRACT -> Opcodes.ACC_ABSTRACT
                    // TODO transform interface modality on lowering to DefaultImpls
                    else -> if (parentAsClass.isJvmInterface && body == null) Opcodes.ACC_ABSTRACT else 0
                }
            }
        val isSynthetic = origin.isSynthetic ||
                hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME) ||
                isReifiable() ||
                isDeprecatedHidden()

        val isStrict = hasAnnotation(STRICTFP_ANNOTATION_FQ_NAME) && origin != JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
        val isSynchronized = hasAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME) && origin != JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER

        return getVisibilityAccessFlag() or modalityFlag or
                (if (isDeprecatedFunction(context)) Opcodes.ACC_DEPRECATED else 0) or
                (if (isStatic) Opcodes.ACC_STATIC else 0) or
                (if (isVararg) Opcodes.ACC_VARARGS else 0) or
                (if (isExternal) Opcodes.ACC_NATIVE else 0) or
                (if (isBridge()) Opcodes.ACC_BRIDGE else 0) or
                (if (isSynthetic) Opcodes.ACC_SYNTHETIC else 0) or
                (if (isStrict) Opcodes.ACC_STRICT else 0) or
                (if (isSynchronized) Opcodes.ACC_SYNCHRONIZED else 0)
    }

    private fun IrFunction.isDeprecatedHidden(): Boolean {
        val mightBeDeprecated = if (this is IrSimpleFunction) {
            allOverridden(true).any {
                it.isAnnotatedWithDeprecated || it.correspondingPropertySymbol?.owner?.isAnnotatedWithDeprecated == true
            }
        } else {
            isAnnotatedWithDeprecated
        }
        return mightBeDeprecated && context.state.deprecationProvider.isDeprecatedHidden(toIrBasedDescriptor())
    }

    private fun getThrownExceptions(function: IrFunction): List<String>? {
        if (context.state.languageVersionSettings.supportsFeature(LanguageFeature.DoNotGenerateThrowsForDelegatedKotlinMembers) &&
            function.origin == IrDeclarationOrigin.DELEGATED_MEMBER
        ) return null

        // @Throws(vararg exceptionClasses: KClass<out Throwable>)
        val exceptionClasses = function.getAnnotation(JVM_THROWS_ANNOTATION_FQ_NAME)?.getValueArgument(0) ?: return null
        return (exceptionClasses as IrVararg).elements.map { exceptionClass ->
            classCodegen.typeMapper.mapType((exceptionClass as IrClassReference).classType).internalName
        }
    }

    private fun generateAnnotationDefaultValueIfNeeded(methodVisitor: MethodVisitor) {
        getAnnotationDefaultValueExpression()?.let { defaultValueExpression ->
            val annotationCodegen = object : AnnotationCodegen(classCodegen) {
                override fun visitAnnotation(descr: String, visible: Boolean): AnnotationVisitor {
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
        val backingField = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.backingField
        val getValue = backingField?.initializer?.expression as? IrGetValue
        val parameter = getValue?.symbol?.owner as? IrValueParameter
        return parameter?.defaultValue?.expression
    }

    private fun IrFunction.createFrameMapWithReceivers(): IrFrameMap {
        val frameMap = IrFrameMap()
        val receiver = if (this is IrConstructor) parentAsClass.thisReceiver else dispatchReceiverParameter
        receiver?.let {
            frameMap.enter(it, classCodegen.typeMapper.mapTypeAsDeclaration(it.type))
        }
        val contextReceivers = valueParameters.subList(0, contextReceiverParametersCount)
        for (contextReceiver in contextReceivers) {
            frameMap.enter(contextReceiver, classCodegen.typeMapper.mapType(contextReceiver.type))
        }
        extensionReceiverParameter?.let {
            frameMap.enter(it, classCodegen.typeMapper.mapType(it))
        }
        val regularParameters = valueParameters.subList(contextReceiverParametersCount, valueParameters.size)
        for (parameter in regularParameters) {
            frameMap.enter(parameter, classCodegen.typeMapper.mapType(parameter.type))
        }
        return frameMap
    }

    // Borrowed from org.jetbrains.kotlin.codegen.FunctionCodegen.java
    private fun generateParameterAnnotations(
        irFunction: IrFunction,
        mv: MethodVisitor,
        jvmSignature: JvmMethodSignature,
        classCodegen: ClassCodegen,
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
                object : AnnotationCodegen(classCodegen, skipNullabilityAnnotations) {
                    override fun visitAnnotation(descr: String, visible: Boolean): AnnotationVisitor {
                        return mv.visitParameterAnnotation(
                            i - syntheticParameterCount,
                            descr,
                            visible
                        )
                    }

                    override fun visitTypeAnnotation(descr: String, path: TypePath?, visible: Boolean): AnnotationVisitor {
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
        private val methodOriginsWithoutAnnotations =
            setOf(
                // Not generating parameter annotations for default stubs fixes KT-7892, though
                // this certainly looks like a workaround for a javac bug.
                IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER,
                JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                IrDeclarationOrigin.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER,
                IrDeclarationOrigin.BRIDGE,
                IrDeclarationOrigin.BRIDGE_SPECIAL,
                JvmLoweredDeclarationOrigin.ABSTRACT_BRIDGE_STUB,
                JvmLoweredDeclarationOrigin.TO_ARRAY,
                IrDeclarationOrigin.IR_BUILTINS_STUB,
                IrDeclarationOrigin.PROPERTY_DELEGATE,
            )

        private val IrFunction.isWithAnnotations: Boolean
            get() = when (origin) {
                in methodOriginsWithoutAnnotations -> false
                IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER -> name.asString() == "<get-entries>"
                else -> true
            }
    }
}


private fun IrValueParameter.isSyntheticMarkerParameter(): Boolean =
    origin == IrDeclarationOrigin.DEFAULT_CONSTRUCTOR_MARKER ||
            origin == JvmLoweredDeclarationOrigin.SYNTHETIC_MARKER_PARAMETER

private fun generateParameterNames(irFunction: IrFunction, mv: MethodVisitor, state: GenerationState) {
    irFunction.extensionReceiverParameter?.let {
        mv.visitParameter(irFunction.extensionReceiverName(state), Opcodes.ACC_MANDATED)
    }
    for (irParameter in irFunction.valueParameters) {
        // A construct emitted by a Java compiler must be marked as synthetic if it does not correspond to a construct declared
        // explicitly or implicitly in source code, unless the emitted construct is a class initialization method (JVMS §2.9).
        // A construct emitted by a Java compiler must be marked as mandated if it corresponds to a formal parameter
        // declared implicitly in source code (§8.8.1, §8.8.9, §8.9.3, §15.9.5.1).
        val access = when {
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
        mv.visitParameter(irParameter.name.asString(), access)
    }
}
