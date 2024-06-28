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
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.backend.jvm.ir.isOptionalAnnotationClass
import org.jetbrains.kotlin.backend.jvm.ir.isWithFlexibleNullability
import org.jetbrains.kotlin.backend.jvm.mapping.MethodSignatureMapper
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.TypePath
import org.jetbrains.org.objectweb.asm.TypeReference
import java.lang.annotation.RetentionPolicy

abstract class AnnotationCodegen(private val classCodegen: ClassCodegen) {
    private val context = classCodegen.context
    private val typeMapper = classCodegen.typeMapper
    private val methodSignatureMapper = classCodegen.methodSignatureMapper

    private val annotationDescriptorsAlreadyPresent = mutableSetOf<String>()

    fun genAnnotations(annotated: IrDeclaration) {
        for (annotation in annotated.annotations) {
            val applicableTargets = annotation.annotationClass.applicableTargetSet()
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
                annotated.visibility == DescriptorVisibilities.LOCAL &&
                KotlinTarget.CLASS !in applicableTargets &&
                KotlinTarget.ANNOTATION_CLASS !in applicableTargets
            ) {
                assert(KotlinTarget.EXPRESSION in applicableTargets) {
                    "Inconsistent target list for object literal annotation: $applicableTargets on $annotated"
                }
                continue
            }

            genAnnotation(annotation, null, false)?.let { descriptor ->
                annotationDescriptorsAlreadyPresent.add(descriptor)
            }
        }
    }

    abstract fun visitAnnotation(descr: String, visible: Boolean): AnnotationVisitor

    open fun visitTypeAnnotation(descr: String, path: TypePath?, visible: Boolean): AnnotationVisitor {
        throw RuntimeException("Not implemented")
    }

    internal fun generateNullabilityAnnotation(declaration: IrDeclaration) {
        if (isInvisibleForNullabilityAnalysis(declaration)) return
        if (declaration is IrValueParameter) {
            val parent = declaration.parent as IrDeclaration
            if (isInvisibleForNullabilityAnalysis(parent)) return
            if (isMovedReceiverParameterOfStaticValueClassReplacement(declaration, parent)) return
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
        if (type.isWithFlexibleNullability()) return

        val annotationClass = if (type.isNullable()) Nullable::class.java else NotNull::class.java

        val descriptor = Type.getType(annotationClass).descriptor
        if (descriptor !in annotationDescriptorsAlreadyPresent) {
            visitAnnotation(descriptor, false).visitEnd()
        }
    }

    private fun isMovedReceiverParameterOfStaticValueClassReplacement(parameter: IrValueParameter, parent: IrDeclaration): Boolean =
        (parent.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT || parent.origin == JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT) &&
                parameter.origin == IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER

    fun generateAnnotationDefaultValue(value: IrExpression) {
        val visitor = visitAnnotation("", false)  // Parameters are unimportant
        genCompileTimeValue(null, value, visitor)
        visitor.visitEnd()
    }

    private fun genAnnotation(annotation: IrConstructorCall, path: TypePath?, isTypeAnnotation: Boolean): String? {
        val annotationClass = annotation.annotationClass
        val retentionPolicy = getRetentionPolicy(annotationClass)
        if (retentionPolicy == RetentionPolicy.SOURCE && !context.state.classBuilderMode.generateSourceRetentionAnnotations) return null

        // FlexibleNullability is an internal annotation, used only inside the compiler
        if (annotationClass.fqNameWhenAvailable in internalAnnotations) return null

        // We do not generate annotations whose classes are optional (annotated with `@OptionalExpectation`) because if an annotation entry
        // is resolved to the expected declaration, this means that annotation has no actual class, and thus should not be generated.
        // (Otherwise we would've resolved the entry to the actual annotation class.)
        if (annotationClass.isOptionalAnnotationClass) return null

        classCodegen.addInnerClassInfo(annotationClass)

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
            else if (context.state.classBuilderMode.generateBodies) //skip error for KAPT
                error("No value for annotation parameter ${param.render()}")
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
                        annotationClassType.classOrNull?.owner?.let(classCodegen::addInnerClassInfo)
                        genAnnotationArguments(value, visitor)
                        visitor.visitEnd()
                    }
                    else -> error("Not supported as annotation! ${ir2string(value)}")
                }
            }
            is IrGetEnumValue -> {
                val enumEntry = value.symbol.owner
                val enumClass = enumEntry.parentAsClass
                classCodegen.addInnerClassInfo(enumClass)
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
                val classType = value.classType
                classType.classOrNull?.owner?.let(classCodegen::addInnerClassInfo)
                val mappedType =
                    if (classType.isInlineClassType()) typeMapper.mapClass(classType.erasedUpperBound)
                    else typeMapper.mapType(classType)
                annotationVisitor.visit(name, mappedType)
            }
            is IrErrorExpression -> error("Don't know how to compile annotation value ${ir2string(value)}")
            else -> error("Unsupported compile-time value ${ir2string(value)}")
        }
    }

    companion object {
        fun genAnnotationsOnTypeParametersAndBounds(
            context: JvmBackendContext,
            typeParameterContainer: IrTypeParametersContainer,
            classCodegen: ClassCodegen,
            referenceType: Int,
            boundType: Int,
            visitor: (typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean) -> AnnotationVisitor
        ) {
            for ((index, typeParameter) in typeParameterContainer.typeParameters.withIndex()) {
                object : AnnotationCodegen(classCodegen) {
                    override fun visitAnnotation(descr: String, visible: Boolean): AnnotationVisitor {
                        val typeReference = TypeReference.newTypeParameterReference(referenceType, index)
                        return visitor(typeReference.value, null, descr, visible)
                    }

                    override fun visitTypeAnnotation(descr: String, path: TypePath?, visible: Boolean): AnnotationVisitor {
                        error("Type annotation cannot be on a type parameter: ${typeParameterContainer.render()}")
                    }
                }.genAnnotations(typeParameter)

                if (!context.config.emitJvmTypeAnnotations) continue

                var superInterfaceIndex = 1
                for (superType in typeParameter.superTypes) {
                    val isClassOrTypeParameter = !superType.isInterface() && !superType.isAnnotation()
                    val superIndex = if (isClassOrTypeParameter) 0 else superInterfaceIndex++
                    object : AnnotationCodegen(classCodegen) {
                        override fun visitAnnotation(descr: String, visible: Boolean): AnnotationVisitor {
                            error("Only type annotation can be on a type parameter bound: ${typeParameterContainer.render()}")
                        }

                        override fun visitTypeAnnotation(descr: String, path: TypePath?, visible: Boolean): AnnotationVisitor {
                            val typeReference = TypeReference.newTypeParameterBoundReference(boundType, index, superIndex)
                            return visitor(typeReference.value, path, descr, visible)
                        }
                    }.generateTypeAnnotations(superType, TypeAnnotationPosition.TypeParameterBoundType)
                }
            }
        }

        private fun isInvisibleForNullabilityAnalysis(declaration: IrDeclaration): Boolean =
            when {
                (declaration.parent as? IrClass)?.isLocal == true -> true
                declaration.origin.isSynthetic ->
                    true
                declaration.origin == JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD ||
                        declaration.origin == JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD ||
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
            JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME,
            JvmSymbols.FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME,
            JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION,
            JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME
        )

        private fun getRetentionPolicy(irClass: IrClass): RetentionPolicy {
            val retention = irClass.getAnnotationRetention()
            if (retention != null) {
                @Suppress("MapGetWithNotNullAssertionOperator")
                return annotationRetentionMap[retention]!!
            }
            irClass.getAnnotation(FqName(java.lang.annotation.Retention::class.java.name))?.let { retentionAnnotation ->
                val value = retentionAnnotation.getValueArgument(0)
                if (value is IrDeclarationReference) {
                    val symbol = value.symbol
                    if (symbol is IrEnumEntrySymbol) {
                        val entry = symbol.owner
                        val enumClassFqName = entry.parentAsClass.fqNameWhenAvailable
                        if (RetentionPolicy::class.java.name == enumClassFqName?.asString()) {
                            return RetentionPolicy.valueOf(entry.name.asString())
                        }
                    }
                }
            }

            return RetentionPolicy.RUNTIME
        }

        val IrConstructorCall.annotationClass: IrClass get() = symbol.owner.parentAsClass
    }

    internal fun generateTypeAnnotations(type: IrType, position: TypeAnnotationPosition) {
        if (!context.config.emitJvmTypeAnnotations) return
        val mode = when (position) {
            is TypeAnnotationPosition.FunctionReturnType ->
                MethodSignatureMapper.getTypeMappingModeForReturnType(context.typeSystem, position.function, position.function.returnType)
            is TypeAnnotationPosition.ValueParameterType ->
                MethodSignatureMapper.getTypeMappingModeForParameter(context.typeSystem, position.parameter.parent as IrDeclaration, type)
            is TypeAnnotationPosition.FieldType ->
                // If a field is final, its generic signature is written as if it's a function return type, otherwise as if it's a value
                // parameter type. See `MethodSignatureMapper.mapFieldSignature`.
                if (position.field.isFinal) {
                    MethodSignatureMapper.getTypeMappingModeForReturnType(context.typeSystem, position.field, position.field.type)
                } else {
                    MethodSignatureMapper.getTypeMappingModeForParameter(context.typeSystem, position.field, position.field.type)
                }
            is TypeAnnotationPosition.TypeParameterBoundType ->
                TypeMappingMode.GENERIC_ARGUMENT
            is TypeAnnotationPosition.Supertype ->
                TypeMappingMode.SUPER_TYPE
        }
        for (info in IrTypeAnnotationCollector(context).collectTypeAnnotations(type, mode)) {
            for (annotation in info.annotations) {
                genAnnotation(annotation, info.path, true)
            }
        }
    }
}

internal sealed class TypeAnnotationPosition {
    class FunctionReturnType(val function: IrFunction) : TypeAnnotationPosition()
    class ValueParameterType(val parameter: IrValueParameter) : TypeAnnotationPosition()
    class FieldType(val field: IrField) : TypeAnnotationPosition()
    data object TypeParameterBoundType : TypeAnnotationPosition()
    data object Supertype : TypeAnnotationPosition()
}

private fun isBareTypeParameterWithNullableUpperBound(type: IrType): Boolean {
    return type.classifierOrNull?.owner is IrTypeParameter && !type.isMarkedNullable() && type.isNullable()
}

private fun IrClass.applicableTargetSet(): Set<KotlinTarget> {
    val valueArgument = getAnnotation(StandardNames.FqNames.target)
        ?.getValueArgument(StandardClassIds.Annotations.ParameterNames.targetAllowedTargets) as? IrVararg
        ?: return KotlinTarget.DEFAULT_TARGET_SET
    return valueArgument.elements.filterIsInstance<IrGetEnumValue>().mapNotNull {
        KotlinTarget.valueOrNull(it.symbol.owner.name.asString())
    }.toSet()
}
