/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.TypeAnnotationCollector
import org.jetbrains.kotlin.codegen.TypePathInfo
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget.JVM_1_6
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.TypePath
import java.lang.annotation.RetentionPolicy

abstract class AnnotationCodegen(
    private val innerClassConsumer: InnerClassConsumer,
    private val context: JvmBackendContext,
    private val skipNullabilityAnnotations: Boolean = false
) {
    private val typeMapper = context.typeMapper
    private val methodSignatureMapper = context.methodSignatureMapper

    /**
     * @param returnType can be null if not applicable (e.g. [annotated] is a class)
     */
    fun genAnnotations(
        annotated: IrAnnotationContainer?,
        returnType: Type?,
        typeForTypeAnnotations: IrType?
    ) {
        if (annotated == null) return

        val annotationDescriptorsAlreadyPresent = mutableSetOf<String>()

        val annotations = annotated.annotations

        for (annotation in annotations) {
            val applicableTargets = annotation.applicableTargetSet()
            if (annotated is IrSimpleFunction &&
                annotated.origin === IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA &&
                KotlinTarget.FUNCTION !in applicableTargets &&
                KotlinTarget.PROPERTY_GETTER !in applicableTargets &&
                KotlinTarget.PROPERTY_SETTER !in applicableTargets
            ) {
                assert(KotlinTarget.EXPRESSION in applicableTargets) {
                    "Inconsistent target list for lambda annotation: $applicableTargets on $annotated"
                }
                continue
            }
            if (annotated is IrClass &&
                KotlinTarget.CLASS !in applicableTargets &&
                KotlinTarget.ANNOTATION_CLASS !in applicableTargets
            ) {
                if (annotated.visibility == DescriptorVisibilities.LOCAL) {
                    assert(KotlinTarget.EXPRESSION in applicableTargets) {
                        "Inconsistent target list for object literal annotation: $applicableTargets on $annotated"
                    }
                    continue
                }
            }

            genAnnotation(annotation, null, false)?.let { descriptor ->
                annotationDescriptorsAlreadyPresent.add(descriptor)
            }
        }

        if (!skipNullabilityAnnotations && annotated is IrDeclaration && returnType != null && !AsmUtil.isPrimitive(returnType)) {
            generateNullabilityAnnotationForCallable(annotated, annotationDescriptorsAlreadyPresent)
        }

        generateTypeAnnotations(annotated, typeForTypeAnnotations)
    }

    abstract fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor

    open fun visitTypeAnnotation(
        descr: String?,
        path: TypePath?,
        visible: Boolean,
    ): AnnotationVisitor {
        throw RuntimeException("Not implemented")
    }


    private fun generateNullabilityAnnotationForCallable(
        declaration: IrDeclaration, // There is no superclass that encompasses IrFunction, IrField and nothing else.
        annotationDescriptorsAlreadyPresent: MutableSet<String>
    ) {
        if (isInvisibleForNullabilityAnalysis(declaration)) return
        if (declaration is IrValueParameter) {
            val parent = declaration.parent as IrDeclaration
            if (isInvisibleForNullabilityAnalysis(parent)) return
            if (isMovedReceiverParameterOfStaticInlineClassReplacement(declaration, parent)) return
        }

        // No need to annotate annotation methods since they're always non-null
        if (declaration is IrSimpleFunction && declaration.correspondingPropertySymbol != null &&
            declaration.parentAsClass.isAnnotationClass
        ) {
            return
        }

        val type = when (declaration) {
            is IrFunction -> declaration.returnType
            is IrValueDeclaration -> declaration.type
            is IrField ->
                if (declaration.correspondingPropertySymbol?.owner?.isLateinit == true) {
                    // Don't generate nullability annotations on lateinit fields
                    return
                } else {
                    declaration.type
                }
            else -> return
        }

        if (isBareTypeParameterWithNullableUpperBound(type)) {
            // This is to account for the case of, say
            //   class Function<R> { fun invoke(): R }
            // it would be a shame to put @Nullable on the return type of the function, and force all callers to check for null,
            // so we put no annotations
            return
        }

        // A flexible type whose lower bound in not-null and upper bound is nullable, should not be annotated
        if (type.isNullabilityFlexible()) {
            return
        }

        val annotationClass = if (type.isNullable()) Nullable::class.java else NotNull::class.java

        generateAnnotationIfNotPresent(annotationDescriptorsAlreadyPresent, annotationClass)
    }

    private fun isMovedReceiverParameterOfStaticInlineClassReplacement(parameter: IrValueParameter, parent: IrDeclaration): Boolean =
        parent.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT &&
                parameter.origin == IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER

    private fun generateAnnotationIfNotPresent(annotationDescriptorsAlreadyPresent: MutableSet<String>, annotationClass: Class<*>) {
        val descriptor = Type.getType(annotationClass).descriptor
        if (!annotationDescriptorsAlreadyPresent.contains(descriptor)) {
            visitAnnotation(descriptor, false).visitEnd()
        }
    }

    fun generateAnnotationDefaultValue(value: IrExpression) {
        val visitor = visitAnnotation("", false)  // Parameters are unimportant
        genCompileTimeValue(null, value, visitor)
        visitor.visitEnd()
    }

    private fun genAnnotation(annotation: IrConstructorCall, path: TypePath?, isTypeAnnotation: Boolean): String? {
        val annotationClass = annotation.annotationClass
        val retentionPolicy = getRetentionPolicy(annotationClass)
        if (retentionPolicy == RetentionPolicy.SOURCE) return null

        // FlexibleNullability is an internal annotation, used only inside the compiler
        if (annotationClass.fqNameWhenAvailable in internalAnnotations) return null

        // We do not generate annotations whose classes are optional (annotated with `@OptionalExpectation`) because if an annotation entry
        // is resolved to the expected declaration, this means that annotation has no actual class, and thus should not be generated.
        // (Otherwise we would've resolved the entry to the actual annotation class.)
        if (annotationClass.isOptionalAnnotationClass) {
            return null
        }

        innerClassConsumer.addInnerClassInfoFromAnnotation(annotationClass)

        val asmTypeDescriptor = typeMapper.mapType(annotation.type).descriptor
        val annotationVisitor =
            if (!isTypeAnnotation) visitAnnotation(asmTypeDescriptor, retentionPolicy == RetentionPolicy.RUNTIME) else
                visitTypeAnnotation(asmTypeDescriptor, path, retentionPolicy == RetentionPolicy.RUNTIME)

        genAnnotationArguments(annotation, annotationVisitor)
        annotationVisitor.visitEnd()

        return asmTypeDescriptor
    }

    private fun genAnnotationArguments(annotation: IrConstructorCall, annotationVisitor: AnnotationVisitor) {
        val annotationClass = annotation.annotationClass
        for (param in annotation.symbol.owner.valueParameters) {
            val value = annotation.getValueArgument(param.index)
            if (value != null)
                genCompileTimeValue(getAnnotationArgumentJvmName(annotationClass, param.name), value, annotationVisitor)
            else if (param.defaultValue != null)
                continue // Default value will be supplied by JVM at runtime.
            else
                error("No value for annotation parameter $param")
        }
    }

    private fun getAnnotationArgumentJvmName(annotationClass: IrClass?, parameterName: Name): String {
        if (annotationClass == null) return parameterName.asString()

        val propertyOrGetter = annotationClass.declarations.singleOrNull {
            // IrSimpleFunction if lowered, IrProperty with a getter if imported
            (it is IrSimpleFunction && it.correspondingPropertySymbol?.owner?.name == parameterName) ||
                    (it is IrProperty && it.name == parameterName)
        } ?: return parameterName.asString()
        val getter = propertyOrGetter as? IrSimpleFunction
            ?: (propertyOrGetter as IrProperty).getter
            ?: error("No getter for annotation property: ${propertyOrGetter.render()}")
        return methodSignatureMapper.mapFunctionName(getter)
    }

    private fun genCompileTimeValue(
        name: String?,
        value: IrExpression,
        annotationVisitor: AnnotationVisitor
    ) {
        when (value) {
            is IrConst<*> -> annotationVisitor.visit(name, value.value)
            is IrConstructorCall -> {
                val callee = value.symbol.owner
                when {
                    callee.parentAsClass.isAnnotationClass -> {
                        val annotationClassType = callee.returnType
                        val internalAnnName = typeMapper.mapType(annotationClassType).descriptor
                        val visitor = annotationVisitor.visitAnnotation(name, internalAnnName)
                        annotationClassType.classOrNull?.owner?.let(innerClassConsumer::addInnerClassInfoFromAnnotation)
                        genAnnotationArguments(value, visitor)
                        visitor.visitEnd()
                    }
                    else -> error("Not supported as annotation! ${ir2string(value)}")
                }
            }
            is IrGetEnumValue -> {
                val enumEntry = value.symbol.owner
                val enumClass = enumEntry.parentAsClass
                innerClassConsumer.addInnerClassInfoFromAnnotation(enumClass)
                annotationVisitor.visitEnum(name, typeMapper.mapClass(enumClass).descriptor, enumEntry.name.asString())
            }
            is IrVararg -> { // array constructor
                val visitor = annotationVisitor.visitArray(name)
                for (element in value.elements) {
                    genCompileTimeValue(null, element as IrExpression, visitor)
                }
                visitor.visitEnd()
            }
            is IrClassReference -> {
                var classType = value.classType
                classType.classOrNull?.owner?.let(innerClassConsumer::addInnerClassInfoFromAnnotation)
                if (classType.isInlined()) {
                    classType = classType.makeNullable()
                }
                annotationVisitor.visit(name, typeMapper.mapType(classType))
            }
            is IrErrorExpression -> error("Don't know how to compile annotation value ${ir2string(value)}")
            else -> error("Unsupported compile-time value ${ir2string(value)}")
        }
    }

    companion object {
        private fun isInvisibleForNullabilityAnalysis(declaration: IrDeclaration): Boolean =
            when {
                declaration.origin.isSynthetic ->
                    true
                declaration.origin == JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD ||
                        declaration.origin == IrDeclarationOrigin.GENERATED_SAM_IMPLEMENTATION ->
                    true
                else ->
                    false
            }

        private val annotationRetentionMap = mapOf(
            KotlinRetention.SOURCE to RetentionPolicy.SOURCE,
            KotlinRetention.BINARY to RetentionPolicy.CLASS,
            KotlinRetention.RUNTIME to RetentionPolicy.RUNTIME
        )

        internal val internalAnnotations = setOf(
            JvmGeneratorExtensions.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME,
            JvmGeneratorExtensions.ENHANCED_NULLABILITY_ANNOTATION_FQ_NAME,
            JvmGeneratorExtensions.RAW_TYPE_ANNOTATION_FQ_NAME
        )

        private fun getRetentionPolicy(irClass: IrClass): RetentionPolicy {
            val retention = irClass.getAnnotationRetention()
            if (retention != null) {
                @Suppress("MapGetWithNotNullAssertionOperator")
                return annotationRetentionMap[retention]!!
            }
            irClass.getAnnotation(FqName(java.lang.annotation.Retention::class.java.name))?.let { retentionAnnotation ->
                val value = retentionAnnotation.getValueArgument(0)
                if (value is IrEnumEntry) {
                    val enumClassFqName = value.parentAsClass.fqNameWhenAvailable
                    if (RetentionPolicy::class.java.name == enumClassFqName?.asString()) {
                        return RetentionPolicy.valueOf(value.name.asString())
                    }
                }
            }

            return RetentionPolicy.RUNTIME
        }

        /* Temporary? */
        fun IrConstructorCall.applicableTargetSet() =
            annotationClass.applicableTargetSet() ?: KotlinTarget.DEFAULT_TARGET_SET

        val IrConstructorCall.annotationClass get() = symbol.owner.parentAsClass
    }

    private fun generateTypeAnnotations(
        annotated: IrAnnotationContainer,
        type: IrType?
    ) {
        if ((annotated as? IrDeclaration)?.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR ||
            type == null || context.state.target === JVM_1_6 ||
            !context.state.configuration.getBoolean(JVMConfigurationKeys.EMIT_JVM_TYPE_ANNOTATIONS)
        ) {
            return
        }
        val infos: Iterable<TypePathInfo<IrConstructorCall>> =
            IrTypeAnnotationCollector(context.typeMapper.typeSystem).collectTypeAnnotations(type)
        for (info in infos) {
            for (annotation in info.annotations) {
                genAnnotation(annotation, info.path, true)
            }
        }
    }

    private class IrTypeAnnotationCollector(context: TypeSystemCommonBackendContext) : TypeAnnotationCollector<IrConstructorCall>(context) {

        override fun KotlinTypeMarker.extractAnnotations(): List<IrConstructorCall> {
            require(this is IrType)
            return annotations.filter {
                // We only generate annotations which have the TYPE_USE Java target.
                // Those are type annotations which were compiled with JVM target bytecode version 1.8 or greater
                (it.annotationClass.origin != IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB &&
                        it.annotationClass.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) ||
                        it.annotationClass.isCompiledToJvm8OrHigher
            }
        }

        private val IrClass.isCompiledToJvm8OrHigher: Boolean
            get() =
                (origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB || origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) &&
                        isCompiledToJvm8OrHigher(source)
    }

}

interface InnerClassConsumer {
    fun addInnerClassInfoFromAnnotation(innerClass: IrClass)
}

private fun isBareTypeParameterWithNullableUpperBound(type: IrType): Boolean {
    return type.classifierOrNull?.owner is IrTypeParameter && !type.isMarkedNullable() && type.isNullable()
}

private val RETENTION_PARAMETER_NAME = Name.identifier("value")

private fun IrClass.getAnnotationRetention(): KotlinRetention? {
    val retentionArgument =
        getAnnotation(StandardNames.FqNames.retention)?.getValueArgument(RETENTION_PARAMETER_NAME)
                as? IrGetEnumValue ?: return null
    val retentionArgumentValue = retentionArgument.symbol.owner
    return KotlinRetention.valueOf(retentionArgumentValue.name.asString())
}

private fun IrType.isNullabilityFlexible(): Boolean =
    hasAnnotation(JvmGeneratorExtensions.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME)

// To be generalized to IrMemberAccessExpression as soon as properties get symbols.
private fun IrConstructorCall.getValueArgument(name: Name): IrExpression? {
    val index = symbol.owner.valueParameters.find { it.name == name }?.index ?: return null
    return getValueArgument(index)
}

// Copied and modified from AnnotationChecker.kt

private val TARGET_ALLOWED_TARGETS = Name.identifier("allowedTargets")

private fun IrClass.applicableTargetSet(): Set<KotlinTarget>? {
    val targetEntry = getAnnotation(StandardNames.FqNames.target) ?: return null
    return loadAnnotationTargets(targetEntry)
}

private fun loadAnnotationTargets(targetEntry: IrConstructorCall): Set<KotlinTarget>? {
    val valueArgument = targetEntry.getValueArgument(TARGET_ALLOWED_TARGETS)
            as? IrVararg ?: return null
    return valueArgument.elements.filterIsInstance<IrGetEnumValue>().mapNotNull {
        KotlinTarget.valueOrNull(it.symbol.owner.name.asString())
    }.toSet()
}

// From ExpectedActualDeclarationChecker
private val IrClass.isOptionalAnnotationClass: Boolean
    get() = kind == ClassKind.ANNOTATION_CLASS &&
            isExpect &&
            hasAnnotation(ExpectedActualDeclarationChecker.OPTIONAL_EXPECTATION_FQ_NAME)
