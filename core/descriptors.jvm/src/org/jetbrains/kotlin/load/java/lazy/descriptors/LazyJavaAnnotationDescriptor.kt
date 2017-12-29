/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findNonGenericClassAcrossDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.*

class LazyJavaAnnotationDescriptor(
        private val c: LazyJavaResolverContext,
        private val javaAnnotation: JavaAnnotation
) : AnnotationDescriptor {
    override val fqName by c.storageManager.createNullableLazyValue {
        javaAnnotation.classId?.asSingleFqName()
    }

    override val type by c.storageManager.createLazyValue {
        val fqName = fqName ?: return@createLazyValue ErrorUtils.createErrorType("No fqName: $javaAnnotation")
        val annotationClass = JavaToKotlinClassMap.mapJavaToKotlin(fqName, c.module.builtIns)
                              ?: javaAnnotation.resolve()?.let { javaClass -> c.components.moduleClassResolver.resolveClass(javaClass) }
                              ?: createTypeForMissingDependencies(fqName)
        annotationClass.defaultType
    }

    override val source = c.components.sourceElementFactory.source(javaAnnotation)

    override val allValueArguments by c.storageManager.createLazyValue {
        javaAnnotation.arguments.mapNotNull { arg ->
            val name = arg.name ?: DEFAULT_ANNOTATION_MEMBER_NAME
            resolveAnnotationArgument(arg)?.let { value -> name to value }
        }.toMap()
    }

    private fun resolveAnnotationArgument(argument: JavaAnnotationArgument?): ConstantValue<*>? {
        return when (argument) {
            is JavaLiteralAnnotationArgument -> ConstantValueFactory.createConstantValue(argument.value)
            is JavaEnumValueAnnotationArgument -> resolveFromEnumValue(argument.enumClassId, argument.entryName)
            is JavaArrayAnnotationArgument -> resolveFromArray(argument.name ?: DEFAULT_ANNOTATION_MEMBER_NAME, argument.getElements())
            is JavaAnnotationAsAnnotationArgument -> resolveFromAnnotation(argument.getAnnotation())
            is JavaClassObjectAnnotationArgument -> resolveFromJavaClassObjectType(argument.getReferencedType())
            else -> null
        }
    }

    private fun resolveFromAnnotation(javaAnnotation: JavaAnnotation): ConstantValue<*> {
        return ConstantValueFactory.createAnnotationValue(LazyJavaAnnotationDescriptor(c, javaAnnotation))
    }

    private fun resolveFromArray(argumentName: Name, elements: List<JavaAnnotationArgument>): ConstantValue<*>? {
        if (type.isError) return null

        val arrayType =
                DescriptorResolverUtils.getAnnotationParameterByName(argumentName, annotationClass!!)?.type
                 // Try to load annotation arguments even if the annotation class is not found
                 ?: c.components.module.builtIns.getArrayType(
                        Variance.INVARIANT,
                        ErrorUtils.createErrorType("Unknown array element type")
                    )

        val values = elements.map {
            argument -> resolveAnnotationArgument(argument) ?: ConstantValueFactory.createNullValue()
        }

        return ConstantValueFactory.createArrayValue(values, arrayType)
    }

    private fun resolveFromEnumValue(enumClassId: ClassId?, entryName: Name?): ConstantValue<*>? {
        if (entryName == null) return null

        if (enumClassId == null) {
            return ConstantValueFactory.createEnumValue(ErrorUtils.createErrorClassWithExactName(entryName))
        }

        val enumClass = c.module.findNonGenericClassAcrossDependencies(
                enumClassId,
                c.components.deserializedDescriptorResolver.components.notFoundClasses
        )

        val classifier = enumClass.unsubstitutedInnerClassesScope.getContributedClassifier(entryName, NoLookupLocation.FROM_JAVA_LOADER)
                                 as? ClassDescriptor ?: return null

        return ConstantValueFactory.createEnumValue(classifier)
    }

    private fun resolveFromJavaClassObjectType(javaType: JavaType): ConstantValue<*>? {
        // Class type is never nullable in 'Foo.class' in Java
        val type = TypeUtils.makeNotNullable(c.typeResolver.transformJavaType(
                javaType,
                TypeUsage.COMMON.toAttributes())
        )

        val jlClass = c.module.resolveTopLevelClass(FqName("java.lang.Class"), NoLookupLocation.FOR_NON_TRACKED_SCOPE) ?: return null

        val arguments = listOf(TypeProjectionImpl(type))

        val javaClassObjectType = KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, jlClass, arguments)

        return ConstantValueFactory.createKClassValue(javaClassObjectType)
    }

    override fun toString(): String {
        return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderAnnotation(this)
    }

    private fun createTypeForMissingDependencies(fqName: FqName) =
            c.module.findNonGenericClassAcrossDependencies(
                    ClassId.topLevel(fqName),
                    c.components.deserializedDescriptorResolver.components.notFoundClasses
            )
}
