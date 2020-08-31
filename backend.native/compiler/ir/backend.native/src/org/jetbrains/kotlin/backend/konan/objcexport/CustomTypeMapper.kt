/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

internal fun ClassDescriptor.isMappedFunctionClass() =
        this.getFunctionalClassKind() == FunctionClassKind.Function &&
                // Type parameters include return type.
                declaredTypeParameters.size - 1 < CustomTypeMappers.functionTypeMappersArityLimit

internal interface CustomTypeMapper {
    val mappedClassId: ClassId
    fun mapType(mappedSuperType: KotlinType, translator: ObjCExportTranslatorImpl, objCExportScope: ObjCExportScope): ObjCNonNullReferenceType
}

internal object CustomTypeMappers {
    /**
     * Custom type mappers.
     *
     * Don't forget to update [hiddenTypes] after adding new one.
     */
    private val predefined: Map<ClassId, CustomTypeMapper> = with(KotlinBuiltIns.FQ_NAMES) {
        val result = mutableListOf<CustomTypeMapper>()

        result += Collection(list, "NSArray")
        result += Collection(mutableList, "NSMutableArray")
        result += Collection(set, "NSSet")
        result += Collection(mutableSet, { namer.mutableSetName.objCName })
        result += Collection(map, "NSDictionary")
        result += Collection(mutableMap, { namer.mutableMapName.objCName })

        NSNumberKind.values().forEach {
            // TODO: NSNumber seem to have different equality semantics.
            val classId = it.mappedKotlinClassId
            if (classId != null) {
                result += Simple(classId, { namer.numberBoxName(classId).objCName })
            }

        }

        result += Simple(ClassId.topLevel(string.toSafe()), "NSString")

        result.associateBy { it.mappedClassId }
    }

    internal val functionTypeMappersArityLimit = 33 // not including, i.e. [0..33)

    fun hasMapper(descriptor: ClassDescriptor): Boolean {
        // Should be equivalent to `getMapper(descriptor) != null`.
        if (descriptor.classId in predefined) return true
        if (descriptor.isMappedFunctionClass()) return true
        return false
    }

    fun getMapper(descriptor: ClassDescriptor): CustomTypeMapper? {
        val classId = descriptor.classId

        predefined[classId]?.let { return it }

        if (descriptor.isMappedFunctionClass()) {
            // TODO: somewhat hacky, consider using FunctionClassDescriptor.arity later.
            val arity = descriptor.declaredTypeParameters.size - 1 // Type parameters include return type.
            assert(classId == KotlinBuiltIns.getFunctionClassId(arity))
            return Function(arity)
        }

        return null
    }

    /**
     * Types to be "hidden" during mapping, i.e. represented as `id`.
     *
     * Currently contains super types of classes handled by custom type mappers.
     * Note: can be generated programmatically, but requires stdlib in this case.
     */
    val hiddenTypes: Set<ClassId> = listOf(
            "kotlin.Any",
            "kotlin.CharSequence",
            "kotlin.Comparable",
            "kotlin.Function",
            "kotlin.Number",
            "kotlin.collections.Collection",
            "kotlin.collections.Iterable",
            "kotlin.collections.MutableCollection",
            "kotlin.collections.MutableIterable"
    ).map { ClassId.topLevel(FqName(it)) }.toSet()

    private class Simple(
            override val mappedClassId: ClassId,
            private val getObjCClassName: ObjCExportTranslatorImpl.() -> String
    ) : CustomTypeMapper {

        constructor(
                mappedClassId: ClassId,
                objCClassName: String
        ) : this(mappedClassId, { objCClassName })

        override fun mapType(mappedSuperType: KotlinType, translator: ObjCExportTranslatorImpl, objCExportScope: ObjCExportScope): ObjCNonNullReferenceType =
                ObjCClassType(translator.getObjCClassName())
    }

    private class Collection(
            mappedClassFqName: FqName,
            private val getObjCClassName: ObjCExportTranslatorImpl.() -> String
    ) : CustomTypeMapper {

        constructor(
                mappedClassFqName: FqName,
                objCClassName: String
        ) : this(mappedClassFqName, { objCClassName })

        override val mappedClassId = ClassId.topLevel(mappedClassFqName)

        override fun mapType(mappedSuperType: KotlinType, translator: ObjCExportTranslatorImpl, objCExportScope: ObjCExportScope): ObjCNonNullReferenceType {
            val typeArguments = mappedSuperType.arguments.map {
                val argument = it.type
                if (TypeUtils.isNullableType(argument)) {
                    // Kotlin `null` keys and values are represented as `NSNull` singleton.
                    ObjCIdType
                } else {
                    translator.mapReferenceTypeIgnoringNullability(argument, objCExportScope)
                }
            }

            return ObjCClassType(translator.getObjCClassName(), typeArguments)
        }
    }

    private class Function(private val parameterCount: Int) : CustomTypeMapper {
        override val mappedClassId: ClassId
            get() = KotlinBuiltIns.getFunctionClassId(parameterCount)

        override fun mapType(mappedSuperType: KotlinType, translator: ObjCExportTranslatorImpl, objCExportScope: ObjCExportScope): ObjCNonNullReferenceType {
            return translator.mapFunctionTypeIgnoringNullability(mappedSuperType, objCExportScope, returnsVoid = false)
        }
    }
}
