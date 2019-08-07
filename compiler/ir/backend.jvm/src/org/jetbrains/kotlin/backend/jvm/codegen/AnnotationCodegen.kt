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
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.synthetic.isVisibleOutside
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Type
import java.lang.annotation.RetentionPolicy

class AnnotationCodegen(
    private val innerClassConsumer: InnerClassConsumer,
    context: JvmBackendContext,
    private val visitAnnotation: (descriptor: String, visible: Boolean) -> AnnotationVisitor
) {
    private val typeMapper = context.typeMapper
    private val methodSignatureMapper = context.methodSignatureMapper

    /**
     * @param returnType can be null if not applicable (e.g. [annotated] is a class)
     */
    fun genAnnotations(annotated: IrAnnotationContainer?, returnType: Type?) {
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
                if (annotated.visibility == Visibilities.LOCAL) {
                    assert(KotlinTarget.EXPRESSION in applicableTargets) {
                        "Inconsistent target list for object literal annotation: $applicableTargets on $annotated"
                    }
                    continue
                }
            }

            genAnnotation(annotation)?.let { descriptor ->
                annotationDescriptorsAlreadyPresent.add(descriptor)
            }
        }

        generateAdditionalAnnotations(annotated, returnType, annotationDescriptorsAlreadyPresent)
    }

    private fun generateAdditionalAnnotations(
        annotated: IrAnnotationContainer,
        returnType: Type?,
        annotationDescriptorsAlreadyPresent: MutableSet<String>
    ) {
        if (annotated is IrDeclaration) {
            if (returnType != null && !AsmUtil.isPrimitive(returnType)) {
                generateNullabilityAnnotationForCallable(annotated, annotationDescriptorsAlreadyPresent)
            }
        }
    }

    private fun generateNullabilityAnnotationForCallable(
        declaration: IrDeclaration, // There is no superclass that encompasses IrFunction, IrField and nothing else.
        annotationDescriptorsAlreadyPresent: MutableSet<String>
    ) {
        // No need to annotate privates, synthetic accessors and their parameters
        if (isInvisibleFromTheOutside(declaration)) return
        if (declaration is IrValueParameter && isInvisibleFromTheOutside(declaration.parent as? IrDeclaration)) return

        // No need to annotate annotation methods since they're always non-null
        if (declaration is IrSimpleFunction && declaration.correspondingPropertySymbol != null &&
            declaration.parentAsClass.isAnnotationClass
        ) {
            return
        }

        val type = when (declaration) {
            is IrFunction -> declaration.returnType
            is IrField -> declaration.type
            is IrValueDeclaration -> declaration.type
            else -> return
        }

        if (isBareTypeParameterWithNullableUpperBound(type)) {
            // This is to account for the case of, say
            //   class Function<R> { fun invoke(): R }
            // it would be a shame to put @Nullable on the return type of the function, and force all callers to check for null,
            // so we put no annotations
            return
        }

        val annotationClass = if (type.isNullable()) Nullable::class.java else NotNull::class.java

        generateAnnotationIfNotPresent(annotationDescriptorsAlreadyPresent, annotationClass)
    }

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

    private fun genAnnotation(annotation: IrConstructorCall): String? {
        val annotationClass = annotation.annotationClass
        val retentionPolicy = getRetentionPolicy(annotationClass)
        if (retentionPolicy == RetentionPolicy.SOURCE) return null

        innerClassConsumer.addInnerClassInfoFromAnnotation(annotationClass)

        val asmTypeDescriptor = typeMapper.mapType(annotation.type).descriptor
        val annotationVisitor = visitAnnotation(asmTypeDescriptor, retentionPolicy == RetentionPolicy.RUNTIME)

        genAnnotationArguments(annotation, annotationVisitor)
        annotationVisitor.visitEnd()

        return asmTypeDescriptor
    }

    private fun genAnnotationArguments(annotation: IrConstructorCall, annotationVisitor: AnnotationVisitor) {
        val annotationClass = annotation.annotationClass
        for (param in annotation.symbol.owner.valueParameters) {
            val value = annotation.getValueArgument(param.index)
            if (value == null) {
                if (param.defaultValue != null) continue // Default value will be supplied by JVM at runtime.
                else error("No value for annotation parameter $param")
            }
            genCompileTimeValue(getAnnotationArgumentJvmName(annotationClass, param.name), value, annotationVisitor)
        }
    }

    private fun getAnnotationArgumentJvmName(annotationClass: IrClass?, parameterName: Name): String {
        if (annotationClass == null) return parameterName.asString()

        val field =
            annotationClass.declarations.filterIsInstance<IrField>().singleOrNull { it.name == parameterName }
                ?: return parameterName.asString()

        return methodSignatureMapper.mapAnnotationParameterName(field)
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
                        val internalAnnName = typeMapper.mapType(callee.returnType).descriptor
                        val visitor = annotationVisitor.visitAnnotation(name, internalAnnName)
                        genAnnotationArguments(value, visitor)
                        visitor.visitEnd()
                    }
                    else -> error("Not supported as annotation! ${ir2string(value)}")
                }
            }
            is IrGetEnumValue -> {
                val enumClassInternalName = typeMapper.mapClass(value.symbol.owner.parentAsClass).descriptor
                val enumEntryName = value.symbol.owner.name
                annotationVisitor.visitEnum(name, enumClassInternalName, enumEntryName.asString())
            }
            is IrVararg -> { // array constructor
                val visitor = annotationVisitor.visitArray(name)
                for (element in value.elements) {
                    genCompileTimeValue(null, element as IrExpression, visitor)
                }
                visitor.visitEnd()
            }
            is IrClassReference -> {
                annotationVisitor.visit(name, typeMapper.mapType(value.classType))
            }
            is IrErrorExpression -> error("Don't know how to compile annotation value ${ir2string(value)}")
            else -> error("Unsupported compile-time value ${ir2string(value)}")
        }
    }

    companion object {
        private fun isInvisibleFromTheOutside(declaration: IrDeclaration?): Boolean {
            if (declaration is IrSimpleFunction && declaration.origin === JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR) {
                return true
            }
            if (declaration is IrDeclarationWithVisibility) {
                return !declaration.visibility.isVisibleOutside()
            }
            return false
        }

        private val annotationRetentionMap = mapOf(
            KotlinRetention.SOURCE to RetentionPolicy.SOURCE,
            KotlinRetention.BINARY to RetentionPolicy.CLASS,
            KotlinRetention.RUNTIME to RetentionPolicy.RUNTIME
        )

        private fun getRetentionPolicy(irClass: IrClass): RetentionPolicy {
            val retention = irClass.getAnnotationRetention()
            if (retention != null) {
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
        getAnnotation(KotlinBuiltIns.FQ_NAMES.retention)?.getValueArgument(RETENTION_PARAMETER_NAME)
                as? IrGetEnumValue?: return null
    val retentionArgumentValue = retentionArgument.symbol.owner
    return KotlinRetention.valueOf(retentionArgumentValue.name.asString())
}

// To be generalized to IrMemberAccessExpression as soon as properties get symbols.
private fun IrConstructorCall.getValueArgument(name: Name): IrExpression? {
    val index = symbol.owner.valueParameters.find { it.name == name }?.index ?: return null
    return getValueArgument(index)
}

// Copied and modified from AnnotationChecker.kt

private val TARGET_ALLOWED_TARGETS = Name.identifier("allowedTargets")

private fun IrClass.applicableTargetSet(): Set<KotlinTarget>? {
    val targetEntry = getAnnotation(KotlinBuiltIns.FQ_NAMES.target) ?: return null
    return loadAnnotationTargets(targetEntry)
}

private fun loadAnnotationTargets(targetEntry: IrConstructorCall): Set<KotlinTarget>? {
    val valueArgument = targetEntry.getValueArgument(TARGET_ALLOWED_TARGETS)
            as? IrVararg ?: return null
    return valueArgument.elements.filterIsInstance<IrGetEnumValue>().mapNotNull {
        KotlinTarget.valueOrNull(it.symbol.owner.name.asString())
    }.toSet()
}
