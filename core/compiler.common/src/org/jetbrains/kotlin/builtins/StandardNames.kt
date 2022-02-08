/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.builtins.StandardNames.FqNames.reflect
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize
import org.jetbrains.kotlin.utils.newHashSetWithExpectedSize

@Suppress("Reformat")
object StandardNames {
    @JvmField val BACKING_FIELD = Name.identifier("field")

    @JvmField val DEFAULT_VALUE_PARAMETER = Name.identifier("value")

    @JvmField val ENUM_VALUES = Name.identifier("values")

    @JvmField val ENUM_VALUE_OF = Name.identifier("valueOf")

    @JvmField val DATA_CLASS_COPY = Name.identifier("copy")

    @JvmField val HASHCODE_NAME = Name.identifier("hashCode")

    @JvmField val CHAR_CODE = Name.identifier("code")

    @JvmField val COROUTINES_PACKAGE_FQ_NAME = FqName("kotlin.coroutines")

    @JvmField val COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME = FqName("kotlin.coroutines.jvm.internal")

    @JvmField val COROUTINES_INTRINSICS_PACKAGE_FQ_NAME = FqName("kotlin.coroutines.intrinsics")

    @JvmField val CONTINUATION_INTERFACE_FQ_NAME = COROUTINES_PACKAGE_FQ_NAME.child(Name.identifier("Continuation"))

    @JvmField val RESULT_FQ_NAME = FqName("kotlin.Result")

    @JvmField val KOTLIN_REFLECT_FQ_NAME = FqName("kotlin.reflect")
    const val K_PROPERTY_PREFIX = "KProperty"
    const val K_MUTABLE_PROPERTY_PREFIX = "KMutableProperty"
    const val K_FUNCTION_PREFIX = "KFunction"
    const val K_SUSPEND_FUNCTION_PREFIX = "KSuspendFunction"

    @JvmField val PREFIXES = listOf(K_PROPERTY_PREFIX, K_MUTABLE_PROPERTY_PREFIX, K_FUNCTION_PREFIX, K_SUSPEND_FUNCTION_PREFIX)

    @JvmField
    val BUILT_INS_PACKAGE_NAME = Name.identifier("kotlin")

    @JvmField
    val BUILT_INS_PACKAGE_FQ_NAME = FqName.topLevel(BUILT_INS_PACKAGE_NAME)

    @JvmField
    val ANNOTATION_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("annotation"))

    @JvmField
    val COLLECTIONS_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("collections"))

    @JvmField
    val RANGES_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("ranges"))

    @JvmField
    val TEXT_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("text"))

    val KOTLIN_INTERNAL_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("internal"))

    @JvmField
    val BUILT_INS_PACKAGE_FQ_NAMES = setOf(
        BUILT_INS_PACKAGE_FQ_NAME,
        COLLECTIONS_PACKAGE_FQ_NAME,
        RANGES_PACKAGE_FQ_NAME,
        ANNOTATION_PACKAGE_FQ_NAME,
        KOTLIN_REFLECT_FQ_NAME,
        KOTLIN_INTERNAL_FQ_NAME,
        COROUTINES_PACKAGE_FQ_NAME
    )

    object FqNames {
        @JvmField val any: FqNameUnsafe = fqNameUnsafe("Any")
        @JvmField val nothing: FqNameUnsafe = fqNameUnsafe("Nothing")
        @JvmField val cloneable: FqNameUnsafe = fqNameUnsafe("Cloneable")
        @JvmField val suppress: FqName = fqName("Suppress")
        @JvmField val unit: FqNameUnsafe = fqNameUnsafe("Unit")
        @JvmField val charSequence: FqNameUnsafe = fqNameUnsafe("CharSequence")
        @JvmField val string: FqNameUnsafe = fqNameUnsafe("String")
        @JvmField val array: FqNameUnsafe = fqNameUnsafe("Array")

        @JvmField val _boolean: FqNameUnsafe = fqNameUnsafe("Boolean")
        @JvmField val _char: FqNameUnsafe = fqNameUnsafe("Char")
        @JvmField val _byte: FqNameUnsafe = fqNameUnsafe("Byte")
        @JvmField val _short: FqNameUnsafe = fqNameUnsafe("Short")
        @JvmField val _int: FqNameUnsafe = fqNameUnsafe("Int")
        @JvmField val _long: FqNameUnsafe = fqNameUnsafe("Long")
        @JvmField val _float: FqNameUnsafe = fqNameUnsafe("Float")
        @JvmField val _double: FqNameUnsafe = fqNameUnsafe("Double")
        @JvmField val number: FqNameUnsafe = fqNameUnsafe("Number")

        @JvmField val _enum: FqNameUnsafe = fqNameUnsafe("Enum")

        @JvmField val functionSupertype: FqNameUnsafe = fqNameUnsafe("Function")

        @JvmField val throwable: FqName = fqName("Throwable")
        @JvmField val comparable: FqName = fqName("Comparable")

        @JvmField val intRange: FqNameUnsafe = rangesFqName("IntRange")
        @JvmField val longRange: FqNameUnsafe = rangesFqName("LongRange")

        @JvmField val deprecated: FqName = fqName("Deprecated")
        @JvmField val deprecatedSinceKotlin: FqName = fqName("DeprecatedSinceKotlin")
        @JvmField val deprecationLevel: FqName = fqName("DeprecationLevel")
        @JvmField val replaceWith: FqName = fqName("ReplaceWith")
        @JvmField val extensionFunctionType: FqName = fqName("ExtensionFunctionType")
        @JvmField val contextFunctionTypeParams: FqName = fqName("ContextFunctionTypeParams")
        @JvmField val parameterName: FqName = fqName("ParameterName")
        @JvmField val parameterNameClassId: ClassId = ClassId.topLevel(parameterName)
        @JvmField val annotation: FqName = fqName("Annotation")
        @JvmField val target: FqName = annotationName("Target")
        @JvmField val targetClassId: ClassId = ClassId.topLevel(target)
        @JvmField val annotationTarget: FqName = annotationName("AnnotationTarget")
        @JvmField val annotationRetention: FqName = annotationName("AnnotationRetention")
        @JvmField val retention: FqName = annotationName("Retention")
        @JvmField val retentionClassId: ClassId = ClassId.topLevel(retention)
        @JvmField val repeatable: FqName = annotationName("Repeatable")
        @JvmField val repeatableClassId: ClassId = ClassId.topLevel(repeatable)
        @JvmField val mustBeDocumented: FqName = annotationName("MustBeDocumented")
        @JvmField val unsafeVariance: FqName = fqName("UnsafeVariance")
        @JvmField val publishedApi: FqName = fqName("PublishedApi")

        @JvmField val iterator: FqName = collectionsFqName("Iterator")
        @JvmField val iterable: FqName = collectionsFqName("Iterable")
        @JvmField val collection: FqName = collectionsFqName("Collection")
        @JvmField val list: FqName = collectionsFqName("List")
        @JvmField val listIterator: FqName = collectionsFqName("ListIterator")
        @JvmField val set: FqName = collectionsFqName("Set")
        @JvmField val map: FqName = collectionsFqName("Map")
        @JvmField val mapEntry: FqName = map.child(Name.identifier("Entry"))
        @JvmField val mutableIterator: FqName = collectionsFqName("MutableIterator")
        @JvmField val mutableIterable: FqName = collectionsFqName("MutableIterable")
        @JvmField val mutableCollection: FqName = collectionsFqName("MutableCollection")
        @JvmField val mutableList: FqName = collectionsFqName("MutableList")
        @JvmField val mutableListIterator: FqName = collectionsFqName("MutableListIterator")
        @JvmField val mutableSet: FqName = collectionsFqName("MutableSet")
        @JvmField val mutableMap: FqName = collectionsFqName("MutableMap")
        @JvmField val mutableMapEntry: FqName = mutableMap.child(Name.identifier("MutableEntry"))

        @JvmField val kClass: FqNameUnsafe = reflect("KClass")
        @JvmField val kCallable: FqNameUnsafe = reflect("KCallable")
        @JvmField val kProperty0: FqNameUnsafe = reflect("KProperty0")
        @JvmField val kProperty1: FqNameUnsafe = reflect("KProperty1")
        @JvmField val kProperty2: FqNameUnsafe = reflect("KProperty2")
        @JvmField val kMutableProperty0: FqNameUnsafe = reflect("KMutableProperty0")
        @JvmField val kMutableProperty1: FqNameUnsafe = reflect("KMutableProperty1")
        @JvmField val kMutableProperty2: FqNameUnsafe = reflect("KMutableProperty2")
        @JvmField val kPropertyFqName: FqNameUnsafe = reflect("KProperty")
        @JvmField val kMutablePropertyFqName: FqNameUnsafe = reflect("KMutableProperty")
        @JvmField val kProperty: ClassId = ClassId.topLevel(kPropertyFqName.toSafe())
        @JvmField val kDeclarationContainer: FqNameUnsafe = reflect("KDeclarationContainer")

        @JvmField val uByteFqName: FqName = fqName("UByte")
        @JvmField val uShortFqName: FqName = fqName("UShort")
        @JvmField val uIntFqName: FqName = fqName("UInt")
        @JvmField val uLongFqName: FqName = fqName("ULong")
        @JvmField val uByte: ClassId = ClassId.topLevel(uByteFqName)
        @JvmField val uShort: ClassId = ClassId.topLevel(uShortFqName)
        @JvmField val uInt: ClassId = ClassId.topLevel(uIntFqName)
        @JvmField val uLong: ClassId = ClassId.topLevel(uLongFqName)
        @JvmField val uByteArrayFqName: FqName = fqName("UByteArray")
        @JvmField val uShortArrayFqName: FqName = fqName("UShortArray")
        @JvmField val uIntArrayFqName: FqName = fqName("UIntArray")
        @JvmField val uLongArrayFqName: FqName = fqName("ULongArray")

        @JvmField val primitiveTypeShortNames: Set<Name> = newHashSetWithExpectedSize<Name>(PrimitiveType.values().size).apply {
            PrimitiveType.values().mapTo(this) { it.typeName }
        }

        @JvmField val primitiveArrayTypeShortNames: Set<Name> = newHashSetWithExpectedSize<Name>(PrimitiveType.values().size).apply {
            PrimitiveType.values().mapTo(this) { it.arrayTypeName }
        }

        @JvmField val fqNameToPrimitiveType: Map<FqNameUnsafe, PrimitiveType> =
            newHashMapWithExpectedSize<FqNameUnsafe, PrimitiveType>(PrimitiveType.values().size).apply {
                for (primitiveType in PrimitiveType.values()) {
                    this[fqNameUnsafe(primitiveType.typeName.asString())] = primitiveType
                }
            }

        @JvmField val arrayClassFqNameToPrimitiveType: MutableMap<FqNameUnsafe, PrimitiveType> =
            newHashMapWithExpectedSize<FqNameUnsafe, PrimitiveType>(PrimitiveType.values().size).apply {
                for (primitiveType in PrimitiveType.values()) {
                    this[fqNameUnsafe(primitiveType.arrayTypeName.asString())] = primitiveType
                }
            }


        private fun fqNameUnsafe(simpleName: String): FqNameUnsafe {
            return fqName(simpleName).toUnsafe()
        }

        private fun fqName(simpleName: String): FqName {
            return BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName))
        }

        private fun collectionsFqName(simpleName: String): FqName {
            return COLLECTIONS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName))
        }

        private fun rangesFqName(simpleName: String): FqNameUnsafe {
            return RANGES_PACKAGE_FQ_NAME.child(Name.identifier(simpleName)).toUnsafe()
        }

        @JvmStatic
        fun reflect(simpleName: String): FqNameUnsafe {
            return KOTLIN_REFLECT_FQ_NAME.child(Name.identifier(simpleName)).toUnsafe()
        }

        private fun annotationName(simpleName: String): FqName {
            return ANNOTATION_PACKAGE_FQ_NAME.child(Name.identifier(simpleName))
        }
    }

    @JvmStatic
    fun getFunctionName(parameterCount: Int): String {
        return "Function$parameterCount"
    }

    @JvmStatic
    fun getFunctionClassId(parameterCount: Int): ClassId {
        return ClassId(BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(getFunctionName(parameterCount)))
    }

    @JvmStatic
    fun getKFunctionFqName(parameterCount: Int): FqNameUnsafe {
        return reflect(FunctionClassKind.KFunction.classNamePrefix + parameterCount)
    }

    @JvmStatic
    fun getKFunctionClassId(parameterCount: Int): ClassId {
        val fqName = getKFunctionFqName(parameterCount)
        return ClassId(fqName.parent().toSafe(), fqName.shortName())
    }

    @JvmStatic
    fun getSuspendFunctionName(parameterCount: Int): String {
        return FunctionClassKind.SuspendFunction.classNamePrefix + parameterCount
    }

    @JvmStatic
    fun getSuspendFunctionClassId(parameterCount: Int): ClassId {
        return ClassId(COROUTINES_PACKAGE_FQ_NAME, Name.identifier(getSuspendFunctionName(parameterCount)))
    }

    @JvmStatic
    fun getKSuspendFunctionName(parameterCount: Int): FqNameUnsafe {
        return reflect(FunctionClassKind.KSuspendFunction.classNamePrefix + parameterCount)
    }

    @JvmStatic
    fun getKSuspendFunctionClassId(parameterCount: Int): ClassId {
        val fqName = getKSuspendFunctionName(parameterCount)
        return ClassId(fqName.parent().toSafe(), fqName.shortName())
    }

    @JvmStatic
    fun isPrimitiveArray(arrayFqName: FqNameUnsafe): Boolean {
        return FqNames.arrayClassFqNameToPrimitiveType.get(arrayFqName) != null
    }

    @JvmStatic
    fun getPrimitiveFqName(primitiveType: PrimitiveType): FqName {
        return BUILT_INS_PACKAGE_FQ_NAME.child(primitiveType.typeName)
    }
}
