package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

internal interface CustomTypeMapper {
    val mappedClassDescriptor: ClassDescriptor
    fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType

    class Simple(
            override val mappedClassDescriptor: ClassDescriptor,
            private val objCClassName: String
    ) : CustomTypeMapper {

        override fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType =
                ObjCClassType(objCClassName)
    }

    class Collection(
            private val generator: ObjCExportHeaderGenerator,
            override val mappedClassDescriptor: ClassDescriptor,
            private val objCClassName: String
    ) : CustomTypeMapper {
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
        override val mappedClassDescriptor = generator.builtIns.getFunction(parameterCount)

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