/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.multiplatform

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.load.java.components.JavaPropertyInitializerEvaluatorImpl
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns

class JavaActualAnnotationArgumentExtractor : ExpectedActualDeclarationChecker.ActualAnnotationArgumentExtractor {
    override fun extractDefaultValue(parameter: ValueParameterDescriptor, expectedType: KotlinType): ConstantValue<*>? {
        val element = (parameter.source as? JavaSourceElement)?.javaElement
        return (element as? JavaMethod)?.annotationParameterDefaultValue?.convert(expectedType)
    }

    // This code is similar to LazyJavaAnnotationDescriptor.resolveAnnotationArgument, but cannot be reused until
    // KClassValue/AnnotationValue are untied from descriptors/types, because here we do not have an instance of LazyJavaResolverContext.
    private fun JavaAnnotationArgument.convert(expectedType: KotlinType): ConstantValue<*>? {
        return when (this) {
            is JavaLiteralAnnotationArgument -> value?.let {
                JavaPropertyInitializerEvaluatorImpl.convertLiteralValue(it, expectedType)
            }
            is JavaEnumValueAnnotationArgument -> {
                enumClassId?.let { enumClassId ->
                    entryName?.let { entryName ->
                        EnumValue(enumClassId, entryName)
                    }
                }
            }
            is JavaArrayAnnotationArgument -> {
                val elementType = expectedType.builtIns.getArrayElementType(expectedType)
                ConstantValueFactory.createArrayValue(getElements().mapNotNull { it.convert(elementType) }, expectedType)
            }
            is JavaAnnotationAsAnnotationArgument -> {
                // TODO: support annotations as annotation arguments (KT-28077)
                null
            }
            is JavaClassObjectAnnotationArgument -> {
                convertTypeToKClassValue(getReferencedType())
            }
            else -> null
        }
    }

    // See FileBasedKotlinClass.resolveKotlinNameByType
    private fun convertTypeToKClassValue(javaType: JavaType): KClassValue? {
        var type = javaType
        var arrayDimensions = 0
        while (type is JavaArrayType) {
            type = type.componentType
            arrayDimensions++
        }
        return when (type) {
            is JavaPrimitiveType -> {
                val primitiveType = type.type
                // void.class is not representable in Kotlin, we approximate it by Unit::class
                    ?: return KClassValue(ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.unit.toSafe()), 0)
                if (arrayDimensions > 0) {
                    KClassValue(ClassId.topLevel(primitiveType.arrayTypeFqName), arrayDimensions - 1)
                } else {
                    KClassValue(ClassId.topLevel(primitiveType.typeFqName), arrayDimensions)
                }
            }
            is JavaClassifierType -> {
                val fqName = FqName(type.classifierQualifiedName)
                // TODO: support nested classes somehow
                val classId = JavaToKotlinClassMap.mapJavaToKotlin(fqName) ?: ClassId.topLevel(fqName)
                KClassValue(classId, arrayDimensions)
            }
            else -> null
        }
    }
}
