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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.descriptors.resolveAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaArrayAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaEnumValueAnnotationArgument
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.storage.StorageManager
import java.lang.annotation.Retention
import java.lang.annotation.Target
import java.util.*

public object JavaAnnotationMapper {

    private val javaTargetFqName = FqName(javaClass<Target>().canonicalName)
    private val javaRetentionFqName = FqName(javaClass<Retention>().canonicalName)
    private val javaDeprecatedFqName = FqName(javaClass<Deprecated>().canonicalName)

    public fun mapJavaAnnotation(annotation: JavaAnnotation, c: LazyJavaResolverContext): AnnotationDescriptor? =
            when (annotation.classId) {
                ClassId.topLevel(javaTargetFqName) -> JavaTargetAnnotationDescriptor(annotation, c)
                ClassId.topLevel(javaRetentionFqName) -> JavaRetentionAnnotationDescriptor(annotation, c)
                ClassId.topLevel(javaDeprecatedFqName) -> JavaDeprecatedAnnotationDescriptor(annotation, c)
                else -> null
            }

    public val kotlinToJavaNameMap: Map<FqName, FqName> =
            mapOf(KotlinBuiltIns.FQ_NAMES.target to javaTargetFqName,
                  KotlinBuiltIns.FQ_NAMES.annotation to javaRetentionFqName,
                  KotlinBuiltIns.FQ_NAMES.deprecated to javaDeprecatedFqName)

    public val javaToKotlinNameMap: Map<FqName, FqName> =
            mapOf(javaTargetFqName to KotlinBuiltIns.FQ_NAMES.target,
                  javaRetentionFqName to KotlinBuiltIns.FQ_NAMES.annotation,
                  javaDeprecatedFqName to KotlinBuiltIns.FQ_NAMES.deprecated)
}

abstract class AbstractJavaAnnotationDescriptor(
        annotation: JavaAnnotation,
        private val kotlinAnnotationClassDescriptor: ClassDescriptor
): AnnotationDescriptor {
    override fun getType() = kotlinAnnotationClassDescriptor.defaultType

    protected val valueParameters: List<ValueParameterDescriptor>
            get() = kotlinAnnotationClassDescriptor.constructors.single().valueParameters

    protected val firstArgument: JavaAnnotationArgument? = annotation.arguments.firstOrNull()
}

class JavaDeprecatedAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext
): AbstractJavaAnnotationDescriptor(annotation, c.module.builtIns.deprecatedAnnotation) {

    private val valueArguments = c.storageManager.createLazyValue {
        val parameterDescriptor = valueParameters.firstOrNull {
            it.name == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME
        }
        parameterDescriptor?.let { mapOf(it to ConstantValueFactory(c.module.builtIns).createConstantValue("Deprecated in Java")) } ?: emptyMap()
    }

    override fun getAllValueArguments() = valueArguments()
}

class JavaRetentionAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext
): AbstractJavaAnnotationDescriptor(annotation, c.module.builtIns.annotationAnnotation) {

    private val valueArguments = c.storageManager.createLazyValue {
        val targetArgument = when (firstArgument) {
            is JavaEnumValueAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaRetentionArgument(firstArgument, c.module.builtIns)
            else -> return@createLazyValue emptyMap<ValueParameterDescriptor, ConstantValue<*>>()
        }
        val parameterDescriptor = valueParameters.firstOrNull {
            it.name == JvmAnnotationNames.RETENTION_ANNOTATION_PARAMETER_NAME
        }
        parameterDescriptor?.let { mapOf(it to targetArgument) } ?: emptyMap()
    }

    override fun getAllValueArguments() = valueArguments()
}

class JavaTargetAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext
): AbstractJavaAnnotationDescriptor(annotation, c.module.builtIns.targetAnnotation) {

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
