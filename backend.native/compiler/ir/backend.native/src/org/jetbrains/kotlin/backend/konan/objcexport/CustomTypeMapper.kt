/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

internal interface CustomTypeMapper {
    val mappedClassId: ClassId
    fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType

    class Simple(
            override val mappedClassId: ClassId,
            private val objCClassName: String
    ) : CustomTypeMapper {

        override fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType =
                ObjCClassType(objCClassName)
    }

    class Collection(
            private val generator: ObjCExportHeaderGenerator,
            mappedClassDescriptor: ClassDescriptor,
            private val objCClassName: String
    ) : CustomTypeMapper {

        override val mappedClassId = mappedClassDescriptor.classId!!

        override fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType {
            val typeArguments = mappedSuperType.arguments.map {
                val argument = it.type
                if (TypeUtils.isNullableType(argument)) {
                    // Kotlin `null` keys and values are represented as `NSNull` singleton.
                    ObjCIdType
                } else {
                    generator.mapReferenceTypeIgnoringNullability(argument)
                }
            }

            return ObjCClassType(objCClassName, typeArguments)
        }
    }

    class Function(
            private val generator: ObjCExportHeaderGenerator,
            parameterCount: Int
    ) : CustomTypeMapper {
        override val mappedClassId: ClassId = generator.builtIns.getFunction(parameterCount).classId!!

        override fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType {
            val functionType = mappedSuperType

            val returnType = functionType.getReturnTypeFromFunctionType()
            val parameterTypes = listOfNotNull(functionType.getReceiverTypeFromFunctionType()) +
                    functionType.getValueParameterTypesFromFunctionType().map { it.type }

            return ObjCBlockPointerType(
                    generator.mapReferenceType(returnType),
                    parameterTypes.map { generator.mapReferenceType(it) }
            )
        }
    }
}