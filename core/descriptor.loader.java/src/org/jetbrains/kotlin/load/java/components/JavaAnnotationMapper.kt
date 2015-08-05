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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.descriptors.resolveAnnotation
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.ErrorUtils
import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.Target
import java.util.EnumSet

public object JavaAnnotationMapper {

    private val JAVA_TARGET_FQ_NAME = FqName(javaClass<Target>().canonicalName)
    private val JAVA_RETENTION_FQ_NAME = FqName(javaClass<Retention>().canonicalName)
    private val JAVA_DEPRECATED_FQ_NAME = FqName(javaClass<Deprecated>().canonicalName)
    private val JAVA_DOCUMENTED_FQ_NAME = FqName(javaClass<Documented>().canonicalName)
    // Java8-specific thing
    private val JAVA_REPEATABLE_FQ_NAME = FqName("java.lang.annotation.Repeatable")

    public fun mapOrResolveJavaAnnotation(annotation: JavaAnnotation, c: LazyJavaResolverContext): AnnotationDescriptor? =
            when (annotation.classId) {
                ClassId.topLevel(JAVA_TARGET_FQ_NAME) -> JavaTargetAnnotationDescriptor(annotation, c)
                ClassId.topLevel(JAVA_RETENTION_FQ_NAME), ClassId.topLevel(JAVA_DEPRECATED_FQ_NAME) -> null
                else -> c.resolveAnnotation(annotation)
            }

    public fun findMappedJavaAnnotation(kotlinName: FqName,
                                        annotationOwner: JavaAnnotationOwner,
                                        c: LazyJavaResolverContext
    ): AnnotationDescriptor? {
        if (kotlinName == KotlinBuiltIns.FQ_NAMES.annotation) {
            // Construct kotlin.annotation.annotation from Retention & Repeatable
            val retentionAnnotation = annotationOwner.findAnnotation(JAVA_RETENTION_FQ_NAME)
            val repeatableAnnotation = annotationOwner.findAnnotation(JAVA_REPEATABLE_FQ_NAME)
            val documentedAnnotation = annotationOwner.findAnnotation(JAVA_DOCUMENTED_FQ_NAME)
            return if (retentionAnnotation != null || repeatableAnnotation != null || documentedAnnotation != null) {
                JavaRetentionRepeatableAnnotationDescriptor(retentionAnnotation, repeatableAnnotation != null,
                                                            documentedAnnotation != null, c)
            }
            else {
                null
            }
        }
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
            mapOf(KotlinBuiltIns.FQ_NAMES.target to JAVA_TARGET_FQ_NAME)

    public val javaToKotlinNameMap: Map<FqName, FqName> =
            mapOf(JAVA_TARGET_FQ_NAME     to KotlinBuiltIns.FQ_NAMES.target,
                  JAVA_RETENTION_FQ_NAME  to KotlinBuiltIns.FQ_NAMES.annotation,
                  JAVA_DEPRECATED_FQ_NAME to KotlinBuiltIns.FQ_NAMES.deprecated,
                  JAVA_REPEATABLE_FQ_NAME to KotlinBuiltIns.FQ_NAMES.annotation,
                  JAVA_DOCUMENTED_FQ_NAME to KotlinBuiltIns.FQ_NAMES.annotation)
}

abstract class AbstractJavaAnnotationDescriptor(
        c: LazyJavaResolverContext,
        annotation: JavaAnnotation?,
        private val kotlinAnnotationClassDescriptor: ClassDescriptor
): AnnotationDescriptor {
    private val source = annotation?.let { c.sourceElementFactory.source(it) } ?: SourceElement.NO_SOURCE

    override fun getType() = kotlinAnnotationClassDescriptor.defaultType

    override fun getSource() = source

    protected val valueParameters: List<ValueParameterDescriptor>
            get() = kotlinAnnotationClassDescriptor.constructors.single().valueParameters

    protected val firstArgument: JavaAnnotationArgument? = annotation?.arguments?.firstOrNull()
}

class JavaDeprecatedAnnotationDescriptor(
        annotation: JavaAnnotation?,
        c: LazyJavaResolverContext
): AbstractJavaAnnotationDescriptor(c, annotation, c.module.builtIns.deprecatedAnnotation) {

    private val valueArguments = c.storageManager.createLazyValue {
        val parameterDescriptor = valueParameters.firstOrNull {
            it.name == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME
        }
        parameterDescriptor?.let { mapOf(it to ConstantValueFactory(c.module.builtIns).createConstantValue("Deprecated in Java")) } ?: emptyMap()
    }

    override fun getAllValueArguments() = valueArguments()
}

class JavaRetentionRepeatableAnnotationDescriptor(
        retentionAnnotation: JavaAnnotation?,
        repeatable: Boolean,
        documented: Boolean,
        c: LazyJavaResolverContext
): AbstractJavaAnnotationDescriptor(c, retentionAnnotation, c.module.builtIns.annotationAnnotation) {

    private val valueArguments = c.storageManager.createLazyValue {
        val retentionArgument = when (firstArgument) {
            is JavaEnumValueAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaRetentionArgument(firstArgument, c.module.builtIns)
            else -> null
        }
        val retentionParameterDescriptor = valueParameters.first {
            it.name == JvmAnnotationNames.RETENTION_ANNOTATION_PARAMETER_NAME
        }
        val repeatableArgument = if (repeatable) BooleanValue(true, c.module.builtIns) else null
        val repeatableParameterDescriptor = valueParameters.first {
            it.name == JvmAnnotationNames.REPEATABLE_ANNOTATION_PARAMETER_NAME
        }
        val documentedArgument = if (documented) BooleanValue(true, c.module.builtIns) else null
        val documentedParameterDescriptor = valueParameters.first {
            it.name == JvmAnnotationNames.DOCUMENTED_ANNOTATION_PARAMETER_NAME
        }
        (retentionArgument?.let { mapOf(retentionParameterDescriptor to it) } ?: emptyMap()) +
        (repeatableArgument?.let { mapOf(repeatableParameterDescriptor to it) } ?: emptyMap()) +
        (documentedArgument?.let { mapOf(documentedParameterDescriptor to it) } ?: emptyMap())
    }

    override fun getAllValueArguments() = valueArguments()
}

class JavaTargetAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext
): AbstractJavaAnnotationDescriptor(c, annotation, c.module.builtIns.targetAnnotation) {

    private val valueArguments = c.storageManager.createLazyValue {
        val targetArgument = when (firstArgument) {
            is JavaArrayAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(firstArgument.getElements(), c.module.builtIns)
            is JavaEnumValueAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(listOf(firstArgument), c.module.builtIns)
            else -> return@createLazyValue emptyMap<ValueParameterDescriptor, ConstantValue<*>>()
        }
        mapOf(valueParameters.single() to targetArgument)
    }

    override fun getAllValueArguments() = valueArguments()
}

public object JavaAnnotationTargetMapper {
    private val targetNameLists = mapOf("PACKAGE"         to EnumSet.of(KotlinTarget.PACKAGE),
                                        "TYPE"            to EnumSet.of(KotlinTarget.CLASSIFIER),
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

    public fun mapJavaTargetArgumentByName(argumentName: String?): Set<KotlinTarget> = targetNameLists[argumentName] ?: emptySet()

    public fun mapJavaTargetArguments(arguments: List<JavaAnnotationArgument>, builtIns: KotlinBuiltIns): ConstantValue<*>? {
        // Map arguments: java.lang.annotation.Target -> kotlin.annotation.target
        val kotlinTargets = arguments.filterIsInstance<JavaEnumValueAnnotationArgument>()
                .flatMap { mapJavaTargetArgumentByName(it.resolve()?.name?.asString()) }
                .map { builtIns.getAnnotationTargetEnumEntry(it) }
                .filterNotNull()
                .map { EnumValue(it) }
        val parameterDescriptor = DescriptorResolverUtils.getAnnotationParameterByName(JvmAnnotationNames.TARGET_ANNOTATION_MEMBER_NAME,
                                                                                       builtIns.targetAnnotation)
        return ArrayValue(kotlinTargets, parameterDescriptor?.type ?: ErrorUtils.createErrorType("Error: AnnotationTarget[]"), builtIns)
    }

    private val retentionNameList = mapOf("RUNTIME" to KotlinRetention.RUNTIME,
                                          "CLASS"   to KotlinRetention.BINARY,
                                          "SOURCE"  to KotlinRetention.SOURCE
    )

    public fun mapJavaRetentionArgument(element: JavaAnnotationArgument, builtIns: KotlinBuiltIns): ConstantValue<*>? {
        // Map argument: java.lang.annotation.Retention -> kotlin.annotation.annotation
        return (element as? JavaEnumValueAnnotationArgument)?.let {
            retentionNameList[it.resolve()?.name?.asString()]?.let {
                (builtIns.getAnnotationRetentionEnumEntry(it) as? ClassDescriptor)?.let { EnumValue(it) }
            }
        }
    }
}
