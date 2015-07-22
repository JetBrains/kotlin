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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.StorageManager
import java.lang.annotation.Target
import java.util.*
import kotlin.annotation

public object JavaAnnotationMapper {

    private val targetClassId = ClassId.topLevel(FqName(javaClass<Target>().getCanonicalName()))

    public fun mapJavaAnnotation(annotation: JavaAnnotation, c: LazyJavaResolverContext): AnnotationDescriptor? =
            when (annotation.classId) {
                targetClassId -> JavaTargetAnnotationDescriptor(annotation, c, c.module.builtIns)
                else -> null
            }

    public val kotlinToJavaNameMap: Map<FqName, FqName> =
            mapOf(KotlinBuiltIns.FQ_NAMES.target to FqName(javaClass<Target>().getCanonicalName()))

    public val javaToKotlinNameMap: Map<FqName, FqName> = kotlinToJavaNameMap.map { it.value to it.key }.toMap()
}

class JavaTargetAnnotationDescriptor(
        annotation: JavaAnnotation,
        c: LazyJavaResolverContext,
        val builtIns: KotlinBuiltIns
): AnnotationDescriptor {

    private val valueArguments = c.storageManager.createLazyValue {
        val firstArgument = annotation.arguments.firstOrNull()
        val targetArgument = when (firstArgument) {
            is JavaArrayAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(firstArgument.getElements(), builtIns)
            is JavaEnumValueAnnotationArgument -> JavaAnnotationTargetMapper.mapJavaTargetArguments(listOf(firstArgument), builtIns)
            else -> return@createLazyValue emptyMap<ValueParameterDescriptor, ConstantValue<*>>()
        }
        val parameterDescriptor = builtIns.targetAnnotation.constructors.firstOrNull()?.valueParameters?.firstOrNull()
        parameterDescriptor?.let { mapOf(it to targetArgument) } ?: emptyMap()
    }

    override fun getAllValueArguments() = valueArguments()

    override fun getType() = builtIns.targetAnnotation.defaultType
}

public object JavaAnnotationTargetMapper {
    private val targetNameLists = mapOf("PACKAGE"         to EnumSet.of(AnnotationTarget.PACKAGE),
                                        "TYPE"            to EnumSet.of(AnnotationTarget.CLASSIFIER),
                                        "ANNOTATION_TYPE" to EnumSet.of(AnnotationTarget.ANNOTATION_CLASS),
                                        "TYPE_PARAMETER"  to EnumSet.of(AnnotationTarget.TYPE_PARAMETER),
                                        "FIELD"           to EnumSet.of(AnnotationTarget.FIELD),
                                        "LOCAL_VARIABLE"  to EnumSet.of(AnnotationTarget.LOCAL_VARIABLE),
                                        "PARAMETER"       to EnumSet.of(AnnotationTarget.VALUE_PARAMETER),
                                        "CONSTRUCTOR"     to EnumSet.of(AnnotationTarget.CONSTRUCTOR),
                                        "METHOD"          to EnumSet.of(AnnotationTarget.FUNCTION,
                                                                        AnnotationTarget.PROPERTY_GETTER,
                                                                        AnnotationTarget.PROPERTY_SETTER),
                                        "TYPE_USE"        to EnumSet.of(AnnotationTarget.TYPE)
    )

    public fun mapJavaTargetArgumentByName(argumentName: String?): Set<AnnotationTarget> = targetNameLists[argumentName] ?: emptySet()

    public fun mapJavaTargetArguments(arguments: List<JavaAnnotationArgument>, builtIns: KotlinBuiltIns): ConstantValue<*>? {
        // Map arguments: java.lang.annotation.Target -> kotlin.annotation.target
        val kotlinTargets = arguments.filterIsInstance<JavaEnumValueAnnotationArgument>()
                .flatMap { mapJavaTargetArgumentByName(it.resolve()?.name?.asString()) }
                .map { builtIns.getAnnotationTargetEnumEntry(Name.identifier(it.name())) }
                .filterNotNull()
                .map { EnumValue(it) }
        val parameterDescriptor = DescriptorResolverUtils.getAnnotationParameterByName(JvmAnnotationNames.TARGET_ANNOTATION_MEMBER_NAME,
                                                                                       builtIns.targetAnnotation)
        return ArrayValue(kotlinTargets, parameterDescriptor?.type ?: ErrorUtils.createErrorType("Error: AnnotationTarget[]"), builtIns)
    }
}
