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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.utils.valuesToMap
import org.jetbrains.kotlin.utils.keysToMapExceptNulls
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.load.kotlin.DeserializedResolverUtils.kotlinFqNameToJavaFqName
import org.jetbrains.kotlin.resolve.jvm.PLATFORM_TYPES

private object DEPRECATED_IN_JAVA : JavaLiteralAnnotationArgument {
    override val name: Name? = null
    override val value: Any? = "Deprecated in Java"
}

fun LazyJavaResolverContext.resolveAnnotation(annotation: JavaAnnotation): LazyJavaAnnotationDescriptor? {
    val classId = annotation.getClassId()
    if (classId == null || isSpecialAnnotation(classId, !PLATFORM_TYPES)) return null
    return LazyJavaAnnotationDescriptor(this, annotation)
}

class LazyJavaAnnotationDescriptor(
        private val c: LazyJavaResolverContext,
        val javaAnnotation : JavaAnnotation
) : AnnotationDescriptor {

    private val fqName = c.storageManager.createNullableLazyValue {
        val classId = javaAnnotation.getClassId()
        if (classId == null) null
        else kotlinFqNameToJavaFqName(classId.asSingleFqName())
    }

    private val type = c.storageManager.createLazyValue {(): JetType ->
        val fqName = fqName()
        if (fqName == null) return@createLazyValue ErrorUtils.createErrorType("No fqName: $javaAnnotation")
        val annotationClass = JavaToKotlinClassMap.INSTANCE.mapKotlinClass(fqName, TypeUsage.MEMBER_SIGNATURE_INVARIANT)
                ?: javaAnnotation.resolve()?.let { javaClass -> c.moduleClassResolver.resolveClass(javaClass) }
        annotationClass?.getDefaultType() ?: ErrorUtils.createErrorType(fqName.asString())
    }

    override fun getType(): JetType = type()

    private val allValueArguments = c.storageManager.createLazyValue {
        computeValueArguments()
    }

    override fun getAllValueArguments() = allValueArguments()

    private fun computeValueArguments(): Map<ValueParameterDescriptor, CompileTimeConstant<*>> {
        val constructors = getAnnotationClass().getConstructors()
        if (constructors.isEmpty()) return mapOf()

        val nameToArg = nameToArgument()

        return constructors.first().getValueParameters().keysToMapExceptNulls { valueParameter ->
            var javaAnnotationArgument = nameToArg[valueParameter.getName()]
            if (javaAnnotationArgument == null && valueParameter.getName() == DEFAULT_ANNOTATION_MEMBER_NAME) {
                javaAnnotationArgument = nameToArg[null]
            }

            resolveAnnotationArgument(javaAnnotationArgument)
        }
    }

    private fun nameToArgument(): Map<Name?, JavaAnnotationArgument> {
        var arguments = javaAnnotation.getArguments()
        if (arguments.isEmpty() && fqName()?.asString() == "java.lang.Deprecated") {
            arguments = listOf(DEPRECATED_IN_JAVA)
        }
        return arguments.valuesToMap { it.name }
    }

    private fun getAnnotationClass() = getType().getConstructor().getDeclarationDescriptor() as ClassDescriptor

    private fun resolveAnnotationArgument(argument: JavaAnnotationArgument?): CompileTimeConstant<*>? {
        return when (argument) {
            is JavaLiteralAnnotationArgument -> createCompileTimeConstant(argument.value, true, false, false, null)
            is JavaEnumValueAnnotationArgument -> resolveFromEnumValue(argument.resolve())
            is JavaArrayAnnotationArgument -> resolveFromArray(argument.name ?: DEFAULT_ANNOTATION_MEMBER_NAME, argument.getElements())
            is JavaAnnotationAsAnnotationArgument -> resolveFromAnnotation(argument.getAnnotation())
            is JavaClassObjectAnnotationArgument -> resolveFromJavaClassObjectType(argument.getReferencedType())
            else -> null
        }
    }

    private fun resolveFromAnnotation(javaAnnotation: JavaAnnotation): CompileTimeConstant<*>? {
        val descriptor = c.resolveAnnotation(javaAnnotation)
        if (descriptor == null) return null

        return AnnotationValue(descriptor)
    }

    private fun resolveFromArray(argumentName: Name, elements: List<JavaAnnotationArgument>): CompileTimeConstant<*>? {
        if (getType().isError()) return null

        val valueParameter = DescriptorResolverUtils.getAnnotationParameterByName(argumentName, getAnnotationClass())
        if (valueParameter == null) return null

        val values = elements.map {
            argument -> resolveAnnotationArgument(argument) ?: NullValue.NULL
        }
        return ArrayValue(values, valueParameter.getType(), true, values.any { it.usesVariableAsConstant() })
    }

    private fun resolveFromEnumValue(element: JavaField?): CompileTimeConstant<*>? {
        if (element == null || !element.isEnumEntry()) return null

        val containingJavaClass = element.getContainingClass()

        //TODO: (module refactoring) moduleClassResolver should be used here
        val enumClass = c.javaClassResolver.resolveClass(containingJavaClass)
        if (enumClass == null) return null

        val classifier = enumClass.getUnsubstitutedInnerClassesScope().getClassifier(element.getName())
        if (classifier !is ClassDescriptor) return null

        return EnumValue(classifier, false)
    }

    private fun resolveFromJavaClassObjectType(javaType: JavaType): CompileTimeConstant<*>? {
        // Class type is never nullable in 'Foo.class' in Java
        val type = TypeUtils.makeNotNullable(c.typeResolver.transformJavaType(
                javaType,
                TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes(allowFlexible = false))
        )

        val jlClass = c.packageFragmentProvider.module.resolveTopLevelClass(FqName("java.lang.Class"))
        if (jlClass == null) return null

        val arguments = listOf(TypeProjectionImpl(type))

        val javaClassObjectType = object : AbstractLazyType(c.storageManager) {
            override fun computeTypeConstructor() = jlClass.getTypeConstructor()
            override fun computeArguments() = arguments
            override fun computeMemberScope() = jlClass.getMemberScope(arguments)
        }

        return JavaClassValue(javaClassObjectType)
    }

    override fun toString(): String {
        return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderAnnotation(this)
    }
}
