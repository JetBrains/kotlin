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
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_IMPL_NAME_SUFFIX
import org.jetbrains.kotlin.codegen.mangleNameIfNeeded
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.visitAnnotableParameterCount
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.STRICTFP_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.SYNCHRONIZED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class FunctionCodegen(
    private val irFunction: IrFunction,
    private val classCodegen: ClassCodegen,
    private val inlinedInto: ExpressionCodegen? = null,
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
        val signature = classCodegen.methodSignatureMapper.mapSignatureWithGeneric(irFunction)

        val flags = calculateMethodFlags(irFunction.isStatic)
        var methodVisitor = createMethod(flags, signature)

        if (state.generateParametersMetadata && flags.and(Opcodes.ACC_SYNTHETIC) == 0) {
            generateParameterNames(irFunction, methodVisitor, signature, state)
        }

        if (irFunction.origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            object : AnnotationCodegen(classCodegen, context) {
                override fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor {
                    return methodVisitor.visitAnnotation(descr, visible)
                }

                override fun visitTypeAnnotation(descr: String?, path: TypePath?, visible: Boolean): AnnotationVisitor {
                    return methodVisitor.visitTypeAnnotation(
                        TypeReference.newTypeReference(TypeReference.METHOD_RETURN).value, path, descr, visible
                    )
                }
            }.genAnnotations(
                irFunction,
                signature.asmMethod.returnType,
                irFunction.returnType
            )
            // Not generating parameter annotations for default stubs fixes KT-7892, though
            // this certainly looks like a workaround for a javac bug.
            if (irFunction !is IrConstructor || !irFunction.parentAsClass.shouldNotGenerateConstructorParameterAnnotations()) {
                generateParameterAnnotations(irFunction, methodVisitor, signature, classCodegen, context)
            }
        }

        if (!state.classBuilderMode.generateBodies || flags.and(Opcodes.ACC_ABSTRACT) != 0 || irFunction.isExternal) {
            generateAnnotationDefaultValueIfNeeded(methodVisitor)
        } else {
            val frameMap = createFrameMapWithReceivers()
            methodVisitor = when {
                irFunction.hasContinuation() -> {
                    generateStateMachineForNamedFunction(
                        irFunction, classCodegen, methodVisitor,
                        access = flags,
                        signature = signature,
                        obtainContinuationClassBuilder = {
                            context.continuationClassBuilders[continuationClass().attributeOwnerId]!!
                        },
                        element = psiElement()
                    )
                }
                irFunction.isInvokeSuspendOfLambda() -> generateStateMachineForLambda(
                    classCodegen, methodVisitor, flags, signature, psiElement()
                )
                else -> methodVisitor
            }
            ExpressionCodegen(irFunction, signature, frameMap, InstructionAdapter(methodVisitor), classCodegen, inlinedInto).generate()
            methodVisitor.visitMaxs(-1, -1)
            if (irFunction.hasContinuation()) {
                context.continuationClassBuilders[continuationClass().attributeOwnerId].sure {
                    "Could not find continuation class builder for ${continuationClass().render()}"
                }.done()
            }
        }
        methodVisitor.visitEnd()

        return signature
    }

    // Since the only arguments to anonymous object constructors are captured variables and complex
    // super constructor arguments, there shouldn't be any annotations on them other than @NonNull,
    // and those are meaningless on synthetic parameters. (Also, the inliner cannot handle them and
    // will throw an exception if we generate any.)
    // The same applies for continuations.
    private fun IrClass.shouldNotGenerateConstructorParameterAnnotations() =
        isAnonymousObject || origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS || origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

    private fun psiElement(): KtElement =
        (if (irFunction.isSuspend)
            irFunction.symbol.descriptor.psiElement ?: irFunction.parentAsClass.descriptor.psiElement
        else
            context.suspendLambdaToOriginalFunctionMap[irFunction.parentAsClass.attributeOwnerId]!!.symbol.descriptor.psiElement)
                as KtElement

    private fun IrFunction.hasContinuation(): Boolean = isSuspend &&
            // We do not generate continuation and state-machine for synthetic accessors, bridges, and delegated members,
            // in a sense, they are tail-call
            !isKnownToBeTailCall() &&
            // This is suspend lambda parameter of inline function
            origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA &&
            // This is just a template for inliner
            origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE &&
            // Continuations are generated for suspendImpls
            parentAsClass.functions.none {
                it.name.asString() == name.asString() + SUSPEND_IMPL_NAME_SUFFIX &&
                        it.attributeOwnerId == (this as? IrAttributeContainer)?.attributeOwnerId
            } &&
            // $$forInline functions never have a continuation
            origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE

    private fun continuationClass() =
        irFunction.body!!.statements.firstIsInstance<IrClass>()

    private fun IrFunction.getVisibilityForDefaultArgumentStub(): Int =
        when (visibility) {
            Visibilities.PUBLIC -> Opcodes.ACC_PUBLIC
            JavaVisibilities.PACKAGE_VISIBILITY -> AsmUtil.NO_FLAG_PACKAGE_PRIVATE
            else -> throw IllegalStateException("Default argument stub should be either public or package private: ${ir2string(this)}")
        }

    private fun calculateMethodFlags(isStatic: Boolean): Int {
        if (irFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            return irFunction.getVisibilityForDefaultArgumentStub() or Opcodes.ACC_SYNTHETIC.let {
                if (irFunction is IrConstructor) it else it or Opcodes.ACC_STATIC
            }
        }

        val visibility = irFunction.getVisibilityAccessFlag()
        val staticFlag = if (isStatic) Opcodes.ACC_STATIC else 0
        val varargFlag = if (irFunction.valueParameters.lastOrNull()?.varargElementType != null) Opcodes.ACC_VARARGS else 0
        val deprecation = irFunction.deprecationFlags
        val bridgeFlag = if (
            irFunction.origin == IrDeclarationOrigin.BRIDGE ||
            irFunction.origin == IrDeclarationOrigin.BRIDGE_SPECIAL
        ) Opcodes.ACC_BRIDGE else 0
        val modalityFlag = when ((irFunction as? IrSimpleFunction)?.modality) {
            Modality.FINAL -> when {
                irFunction.origin == JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER -> 0
                classCodegen.irClass.isInterface && irFunction.body != null -> 0
                !classCodegen.irClass.isAnnotationClass || irFunction.isStatic -> Opcodes.ACC_FINAL
                else -> Opcodes.ACC_ABSTRACT
            }
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

    protected open fun createMethod(flags: Int, signature: JvmMethodGenericSignature): MethodVisitor =
        classCodegen.visitor.newMethod(
            irFunction.OtherOrigin,
            flags,
            signature.asmMethod.name, signature.asmMethod.descriptor,
            if (flags.and(Opcodes.ACC_SYNTHETIC) != 0) null else signature.genericsSignature,
            getThrownExceptions(irFunction)?.toTypedArray()
        )

    private fun getThrownExceptions(function: IrFunction): List<String>? {
        if (state.languageVersionSettings.supportsFeature(LanguageFeature.DoNotGenerateThrowsForDelegatedKotlinMembers) &&
            function.origin == IrDeclarationOrigin.DELEGATED_MEMBER
        ) return null

        // @Throws(vararg exceptionClasses: KClass<out Throwable>)
        val exceptionClasses = function.getAnnotation(FqName("kotlin.jvm.Throws"))?.getValueArgument(0) ?: return null
        return (exceptionClasses as IrVararg).elements.map { exceptionClass ->
            classCodegen.typeMapper.mapType((exceptionClass as IrClassReference).classType).internalName
        }
    }

    private fun generateAnnotationDefaultValueIfNeeded(methodVisitor: MethodVisitor) {
        getAnnotationDefaultValueExpression()?.let { defaultValueExpression ->
            val annotationCodegen = object: AnnotationCodegen(
                classCodegen,
                context
            ) {
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

    private fun IrFrameMap.enterDispatchReceiver(parameter: IrValueParameter) {
        val type = classCodegen.typeMapper.mapTypeAsDeclaration(parameter.type)
        enter(parameter, type)
    }

    private fun createFrameMapWithReceivers(): IrFrameMap {
        val frameMap = IrFrameMap()

        if (irFunction is IrConstructor) {
            frameMap.enterDispatchReceiver(irFunction.constructedClass.thisReceiver!!)
        } else if (irFunction.dispatchReceiverParameter != null) {
            frameMap.enterDispatchReceiver(irFunction.dispatchReceiverParameter!!)
        }
        irFunction.extensionReceiverParameter?.let {
            frameMap.enter(it, classCodegen.typeMapper.mapType(it))
        }
        for (parameter in irFunction.valueParameters) {
            frameMap.enter(parameter, classCodegen.typeMapper.mapType(parameter.type))
        }

        return frameMap
    }
}

// Borrowed from org.jetbrains.kotlin.codegen.FunctionCodegen.java
private fun generateParameterAnnotations(
    irFunction: IrFunction,
    mv: MethodVisitor,
    jvmSignature: JvmMethodSignature,
    innerClassConsumer: InnerClassConsumer,
    context: JvmBackendContext
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

        if (!kind.isSkippedInGenericSignature) {
            object : AnnotationCodegen(innerClassConsumer, context) {
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
            }.genAnnotations(annotated, parameterSignature.asmType, annotated?.type)
        }
    }
}

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
