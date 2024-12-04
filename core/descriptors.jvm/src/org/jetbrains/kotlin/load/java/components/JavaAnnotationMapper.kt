/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.kotlin.load.java.descriptors.PossiblyExternalAnnotationDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.SimpleType
import java.util.*

object JavaAnnotationMapper {
    internal val DEPRECATED_ANNOTATION_MESSAGE = Name.identifier("message")
    internal val TARGET_ANNOTATION_ALLOWED_TARGETS = Name.identifier("allowedTargets")
    internal val RETENTION_ANNOTATION_VALUE = Name.identifier("value")

    fun mapOrResolveJavaAnnotation(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext,
        isFreshlySupportedAnnotation: Boolean = false
    ): AnnotationDescriptor? =
        when (annotation.classId) {
            ClassId.topLevel(TARGET_ANNOTATION) -> JavaTargetAnnotationDescriptor(annotation, c)
            ClassId.topLevel(RETENTION_ANNOTATION) -> JavaRetentionAnnotationDescriptor(annotation, c)
            ClassId.topLevel(DOCUMENTED_ANNOTATION) -> JavaAnnotationDescriptor(c, annotation, StandardNames.FqNames.mustBeDocumented)
            ClassId.topLevel(DEPRECATED_ANNOTATION) -> null
            else -> LazyJavaAnnotationDescriptor(c, annotation, isFreshlySupportedAnnotation)
        }

    fun findMappedJavaAnnotation(
        kotlinName: FqName,
        annotationOwner: JavaAnnotationOwner,
        c: LazyJavaResolverContext
    ): AnnotationDescriptor? {
        if (kotlinName == StandardNames.FqNames.deprecated) {
            val javaAnnotation = annotationOwner.findAnnotation(DEPRECATED_ANNOTATION)
            if (javaAnnotation != null || annotationOwner.isDeprecatedInJavaDoc) {
                return JavaDeprecatedAnnotationDescriptor(javaAnnotation, c)
            }
        }
        return kotlinToJavaNameMap[kotlinName]?.let { javaName ->
            annotationOwner.findAnnotation(javaName)?.let { annotation ->
                mapOrResolveJavaAnnotation(annotation, c)
            }
        }
    }

    private val kotlinToJavaNameMap: Map<FqName, FqName> =
        mapOf(
            StandardNames.FqNames.target to TARGET_ANNOTATION,
            StandardNames.FqNames.retention to RETENTION_ANNOTATION,
            StandardNames.FqNames.mustBeDocumented to DOCUMENTED_ANNOTATION
        )
}

open class JavaAnnotationDescriptor(
    c: LazyJavaResolverContext,
    annotation: JavaAnnotation?,
    override val fqName: FqName
) : AnnotationDescriptor, PossiblyExternalAnnotationDescriptor {
    override val source: SourceElement = annotation?.let { c.components.sourceElementFactory.source(it) } ?: SourceElement.NO_SOURCE

    override val type: SimpleType by c.storageManager.createLazyValue { c.module.builtIns.getBuiltInClassByFqName(fqName).defaultType }

    protected val firstArgument: JavaAnnotationArgument? = annotation?.arguments?.firstOrNull()

    override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()

    override val isIdeExternalAnnotation: Boolean = annotation?.isIdeExternalAnnotation == true
}

class JavaDeprecatedAnnotationDescriptor(
    annotation: JavaAnnotation?,
    c: LazyJavaResolverContext
) : JavaAnnotationDescriptor(c, annotation, StandardNames.FqNames.deprecated) {
    override val allValueArguments: Map<Name, ConstantValue<*>> by c.storageManager.createLazyValue {
        mapOf(JavaAnnotationMapper.DEPRECATED_ANNOTATION_MESSAGE to StringValue("Deprecated in Java"))
    }
}

class JavaTargetAnnotationDescriptor(
    annotation: JavaAnnotation,
    c: LazyJavaResolverContext
) : JavaAnnotationDescriptor(c, annotation, StandardNames.FqNames.target) {
    override val allValueArguments by c.storageManager.createLazyValue {
        val targetArgument = when (firstArgument) {
            is JavaArrayAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(firstArgument.getElements())
            is JavaEnumValueAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(listOf(firstArgument))
            else -> null
        }
        targetArgument?.let { mapOf(JavaAnnotationMapper.TARGET_ANNOTATION_ALLOWED_TARGETS to it) }.orEmpty()
    }
}

class JavaRetentionAnnotationDescriptor(
    annotation: JavaAnnotation,
    c: LazyJavaResolverContext
) : JavaAnnotationDescriptor(c, annotation, StandardNames.FqNames.retention) {
    override val allValueArguments by c.storageManager.createLazyValue {
        val retentionArgument = JavaAnnotationTargetMapper.mapJavaRetentionArgument(firstArgument)
        retentionArgument?.let { mapOf(JavaAnnotationMapper.RETENTION_ANNOTATION_VALUE to it) }.orEmpty()
    }
}

object JavaAnnotationTargetMapper {
    private val targetNameLists = mapOf(
        "PACKAGE" to EnumSet.noneOf(KotlinTarget::class.java),
        "TYPE" to EnumSet.of(KotlinTarget.CLASS, KotlinTarget.FILE),
        "ANNOTATION_TYPE" to EnumSet.of(KotlinTarget.ANNOTATION_CLASS),
        "TYPE_PARAMETER" to EnumSet.of(KotlinTarget.TYPE_PARAMETER),
        "FIELD" to EnumSet.of(KotlinTarget.FIELD),
        "LOCAL_VARIABLE" to EnumSet.of(KotlinTarget.LOCAL_VARIABLE),
        "PARAMETER" to EnumSet.of(KotlinTarget.VALUE_PARAMETER),
        "CONSTRUCTOR" to EnumSet.of(KotlinTarget.CONSTRUCTOR),
        "METHOD" to EnumSet.of(KotlinTarget.FUNCTION, KotlinTarget.PROPERTY_GETTER, KotlinTarget.PROPERTY_SETTER),
        "TYPE_USE" to EnumSet.of(KotlinTarget.TYPE)
    )

    fun mapJavaTargetArgumentByName(argumentName: String?): Set<KotlinTarget> = targetNameLists[argumentName] ?: emptySet()

    internal fun mapJavaTargetArguments(arguments: List<JavaAnnotationArgument>): ConstantValue<*> {
        // Map arguments: java.lang.annotation.Target -> kotlin.annotation.Target
        val kotlinTargets = arguments.filterIsInstance<JavaEnumValueAnnotationArgument>()
            .flatMap { mapJavaTargetArgumentByName(it.entryName?.asString()) }
            .map { kotlinTarget ->
                EnumValue(ClassId.topLevel(StandardNames.FqNames.annotationTarget), Name.identifier(kotlinTarget.name))
            }
        return ArrayValue(kotlinTargets) { module ->
            val parameterDescriptor = DescriptorResolverUtils.getAnnotationParameterByName(
                JavaAnnotationMapper.TARGET_ANNOTATION_ALLOWED_TARGETS,
                module.builtIns.getBuiltInClassByFqName(StandardNames.FqNames.target)
            )
            parameterDescriptor?.type ?: ErrorUtils.createErrorType(ErrorTypeKind.UNMAPPED_ANNOTATION_TARGET_TYPE)
        }
    }

    private val retentionNameList = mapOf(
        "RUNTIME" to KotlinRetention.RUNTIME,
        "CLASS" to KotlinRetention.BINARY,
        "SOURCE" to KotlinRetention.SOURCE
    )

    internal fun mapJavaRetentionArgument(element: JavaAnnotationArgument?): ConstantValue<*>? {
        // Map argument: java.lang.annotation.Retention -> kotlin.annotation.Retention
        return (element as? JavaEnumValueAnnotationArgument)?.let {
            retentionNameList[it.entryName?.asString()]?.let { retention ->
                EnumValue(ClassId.topLevel(StandardNames.FqNames.annotationRetention), Name.identifier(retention.name))
            }
        }
    }
}
