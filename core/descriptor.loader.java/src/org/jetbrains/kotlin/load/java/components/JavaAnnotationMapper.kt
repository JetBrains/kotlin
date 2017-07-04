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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.SimpleType
import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.Target
import java.util.*

object JavaAnnotationMapper {

    private val JAVA_TARGET_FQ_NAME = FqName(Target::class.java.canonicalName)
    private val JAVA_RETENTION_FQ_NAME = FqName(Retention::class.java.canonicalName)
    private val JAVA_DEPRECATED_FQ_NAME = FqName(java.lang.Deprecated::class.java.canonicalName)
    private val JAVA_DOCUMENTED_FQ_NAME = FqName(Documented::class.java.canonicalName)
    // Java8-specific thing
    private val JAVA_REPEATABLE_FQ_NAME = FqName("java.lang.annotation.Repeatable")

    internal val DEPRECATED_ANNOTATION_MESSAGE = Name.identifier("message")
    internal val TARGET_ANNOTATION_ALLOWED_TARGETS = Name.identifier("allowedTargets")
    internal val RETENTION_ANNOTATION_VALUE = Name.identifier("value")

    fun mapOrResolveJavaAnnotation(annotation: JavaAnnotation, c: LazyJavaResolverContext): AnnotationDescriptor? =
            when (annotation.classId) {
                ClassId.topLevel(JAVA_TARGET_FQ_NAME) -> JavaTargetAnnotationDescriptor(annotation, c)
                ClassId.topLevel(JAVA_RETENTION_FQ_NAME) -> JavaRetentionAnnotationDescriptor(annotation, c)
                ClassId.topLevel(JAVA_REPEATABLE_FQ_NAME) -> JavaAnnotationDescriptor(c, annotation, KotlinBuiltIns.FQ_NAMES.repeatable)
                ClassId.topLevel(JAVA_DOCUMENTED_FQ_NAME) -> JavaAnnotationDescriptor(c, annotation, KotlinBuiltIns.FQ_NAMES.mustBeDocumented)
                ClassId.topLevel(JAVA_DEPRECATED_FQ_NAME) -> null
                else -> LazyJavaAnnotationDescriptor(c, annotation)
            }

    fun findMappedJavaAnnotation(
            kotlinName: FqName,
            annotationOwner: JavaAnnotationOwner,
            c: LazyJavaResolverContext
    ): AnnotationDescriptor? {
        if (kotlinName == KotlinBuiltIns.FQ_NAMES.deprecated) {
            val javaAnnotation = annotationOwner.findAnnotation(JAVA_DEPRECATED_FQ_NAME)
            if (javaAnnotation != null || annotationOwner.isDeprecatedInJavaDoc) {
                return JavaDeprecatedAnnotationDescriptor(javaAnnotation, c)
            }
        }
        return kotlinToJavaNameMap[kotlinName]?.let {
            annotationOwner.findAnnotation(it)?.let {
                mapOrResolveJavaAnnotation(it, c)
            }
        }
    }

    // kotlin.annotation.annotation is treated separately
    private val kotlinToJavaNameMap: Map<FqName, FqName> =
            mapOf(KotlinBuiltIns.FQ_NAMES.target to JAVA_TARGET_FQ_NAME,
                  KotlinBuiltIns.FQ_NAMES.retention to JAVA_RETENTION_FQ_NAME,
                  KotlinBuiltIns.FQ_NAMES.repeatable to JAVA_REPEATABLE_FQ_NAME,
                  KotlinBuiltIns.FQ_NAMES.mustBeDocumented to JAVA_DOCUMENTED_FQ_NAME)

    val javaToKotlinNameMap: Map<FqName, FqName> =
            mapOf(JAVA_TARGET_FQ_NAME     to KotlinBuiltIns.FQ_NAMES.target,
                  JAVA_RETENTION_FQ_NAME  to KotlinBuiltIns.FQ_NAMES.retention,
                  JAVA_DEPRECATED_FQ_NAME to KotlinBuiltIns.FQ_NAMES.deprecated,
                  JAVA_REPEATABLE_FQ_NAME to KotlinBuiltIns.FQ_NAMES.repeatable,
                  JAVA_DOCUMENTED_FQ_NAME to KotlinBuiltIns.FQ_NAMES.mustBeDocumented)
}

open class JavaAnnotationDescriptor(
        c: LazyJavaResolverContext,
        annotation: JavaAnnotation?,
        override val fqName: FqName
): AnnotationDescriptor {
    override val source: SourceElement = annotation?.let { c.components.sourceElementFactory.source(it) } ?: SourceElement.NO_SOURCE

    override val type: SimpleType by c.storageManager.createLazyValue { c.module.builtIns.getBuiltInClassByFqName(fqName).defaultType }

    protected val firstArgument: JavaAnnotationArgument? = annotation?.arguments?.firstOrNull()

    override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
}

class JavaDeprecatedAnnotationDescriptor(
        annotation: JavaAnnotation?,
        c: LazyJavaResolverContext
): JavaAnnotationDescriptor(c, annotation, KotlinBuiltIns.FQ_NAMES.deprecated) {
    override val allValueArguments: Map<Name, ConstantValue<*>> by c.storageManager.createLazyValue {
        mapOf(JavaAnnotationMapper.DEPRECATED_ANNOTATION_MESSAGE to
                      ConstantValueFactory(c.module.builtIns).createStringValue("Deprecated in Java"))
    }
}

class JavaTargetAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext
): JavaAnnotationDescriptor(c, annotation, KotlinBuiltIns.FQ_NAMES.target) {
    override val allValueArguments by c.storageManager.createLazyValue {
        val targetArgument = when (firstArgument) {
            is JavaArrayAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(firstArgument.getElements(), c.module.builtIns)
            is JavaEnumValueAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(listOf(firstArgument), c.module.builtIns)
            else -> null
        }
        targetArgument?.let { mapOf(JavaAnnotationMapper.TARGET_ANNOTATION_ALLOWED_TARGETS to it) }.orEmpty()
    }
}

class JavaRetentionAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext
): JavaAnnotationDescriptor(c, annotation, KotlinBuiltIns.FQ_NAMES.retention) {
    override val allValueArguments by c.storageManager.createLazyValue {
        val retentionArgument = JavaAnnotationTargetMapper.mapJavaRetentionArgument(firstArgument, c.module.builtIns)
        retentionArgument?.let { mapOf(JavaAnnotationMapper.RETENTION_ANNOTATION_VALUE to it) }.orEmpty()
    }
}

object JavaAnnotationTargetMapper {
    private val targetNameLists = mapOf("PACKAGE"         to EnumSet.noneOf(KotlinTarget::class.java),
                                        "TYPE"            to EnumSet.of(KotlinTarget.CLASS, KotlinTarget.FILE),
                                        "ANNOTATION_TYPE" to EnumSet.of(KotlinTarget.ANNOTATION_CLASS),
                                        "TYPE_PARAMETER"  to EnumSet.of(KotlinTarget.TYPE_PARAMETER),
                                        "FIELD"           to EnumSet.of(KotlinTarget.FIELD),
                                        "LOCAL_VARIABLE"  to EnumSet.of(KotlinTarget.LOCAL_VARIABLE),
                                        "PARAMETER"       to EnumSet.of(KotlinTarget.VALUE_PARAMETER),
                                        "CONSTRUCTOR"     to EnumSet.of(KotlinTarget.CONSTRUCTOR),
                                        "METHOD"          to EnumSet.of(KotlinTarget.FUNCTION,
                                                                        KotlinTarget.PROPERTY_GETTER,
                                                                        KotlinTarget.PROPERTY_SETTER),
                                        "TYPE_USE"        to EnumSet.of(KotlinTarget.TYPE)
    )

    fun mapJavaTargetArgumentByName(argumentName: String?): Set<KotlinTarget> = targetNameLists[argumentName] ?: emptySet()

    internal fun mapJavaTargetArguments(arguments: List<JavaAnnotationArgument>, builtIns: KotlinBuiltIns): ConstantValue<*> {
        // Map arguments: java.lang.annotation.Target -> kotlin.annotation.Target
        val kotlinTargets = arguments.filterIsInstance<JavaEnumValueAnnotationArgument>()
                .flatMap { mapJavaTargetArgumentByName(it.resolve()?.name?.asString()) }
                .mapNotNull { builtIns.getAnnotationTargetEnumEntry(it) }
                .map(::EnumValue)
        val parameterDescriptor = DescriptorResolverUtils.getAnnotationParameterByName(
                JavaAnnotationMapper.TARGET_ANNOTATION_ALLOWED_TARGETS,
                builtIns.getBuiltInClassByFqName(KotlinBuiltIns.FQ_NAMES.target)
        )
        return ArrayValue(kotlinTargets, parameterDescriptor?.type ?: ErrorUtils.createErrorType("Error: AnnotationTarget[]"), builtIns)
    }

    private val retentionNameList = mapOf(
            "RUNTIME" to KotlinRetention.RUNTIME,
            "CLASS"   to KotlinRetention.BINARY,
            "SOURCE"  to KotlinRetention.SOURCE
    )

    internal fun mapJavaRetentionArgument(element: JavaAnnotationArgument?, builtIns: KotlinBuiltIns): ConstantValue<*>? {
        // Map argument: java.lang.annotation.Retention -> kotlin.annotation.annotation
        return (element as? JavaEnumValueAnnotationArgument)?.let {
            retentionNameList[it.resolve()?.name?.asString()]?.let {
                builtIns.getAnnotationRetentionEnumEntry(it)?.let(::EnumValue)
            }
        }
    }
}
