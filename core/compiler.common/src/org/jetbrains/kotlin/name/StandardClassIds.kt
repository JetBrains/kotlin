/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.builtins.StandardNames

object StandardClassIds {

    val BASE_KOTLIN_PACKAGE = FqName("kotlin")
    val BASE_REFLECT_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("reflect"))
    val BASE_COLLECTIONS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("collections"))
    private fun String.baseId() = ClassId(BASE_KOTLIN_PACKAGE, Name.identifier(this))
    private fun ClassId.unsignedId() = ClassId(BASE_KOTLIN_PACKAGE, Name.identifier("U" + shortClassName.identifier))
    private fun String.reflectId() = ClassId(BASE_REFLECT_PACKAGE, Name.identifier(this))
    private fun Name.primitiveArrayId() = ClassId(Array.packageFqName, Name.identifier(identifier + Array.shortClassName.identifier))
    private fun String.collectionsId() = ClassId(BASE_COLLECTIONS_PACKAGE, Name.identifier(this))

    val Nothing = "Nothing".baseId()
    val Unit = "Unit".baseId()
    val Any = "Any".baseId()
    val Enum = "Enum".baseId()
    val Annotation = "Annotation".baseId()
    val Array = "Array".baseId()

    val Boolean = "Boolean".baseId()
    val Char = "Char".baseId()
    val Byte = "Byte".baseId()
    val Short = "Short".baseId()
    val Int = "Int".baseId()
    val Long = "Long".baseId()
    val Float = "Float".baseId()
    val Double = "Double".baseId()

    val UByte = Byte.unsignedId()
    val UShort = Short.unsignedId()
    val UInt = Int.unsignedId()
    val ULong = Long.unsignedId()

    val String = "String".baseId()
    val Throwable = "Throwable".baseId()

    val KProperty = "KProperty".reflectId()
    val KMutableProperty = "KMutableProperty".reflectId()
    val KProperty0 = "KProperty0".reflectId()
    val KMutableProperty0 = "KMutableProperty0".reflectId()
    val KProperty1 = "KProperty1".reflectId()
    val KMutableProperty1 = "KMutableProperty1".reflectId()
    val KProperty2 = "KProperty2".reflectId()
    val KMutableProperty2 = "KMutableProperty2".reflectId()
    val KFunction = "KFunction".reflectId()
    val KClass = "KClass".reflectId()
    val KCallable = "KCallable".reflectId()

    val Comparable = "Comparable".baseId()
    val Number = "Number".baseId()

    val Function = "Function".baseId()

    fun byName(name: String) = name.baseId()
    fun reflectByName(name: String) = name.reflectId()


    val primitiveTypes = listOf(
        Boolean, Char,
        Byte, Short, Int, Long,
        Float, Double
    )
    val primitiveTypesAndString = primitiveTypes + String

    val primitiveArrayTypeByElementType = primitiveTypes.associate { id -> id to id.shortClassName.primitiveArrayId() }
    val elementTypeByPrimitiveArrayType = primitiveArrayTypeByElementType.inverseMap()

    val unsignedTypes = listOf(UByte, UShort, UInt, ULong)
    val unsignedArrayTypeByElementType = unsignedTypes.associate { id -> id to id.shortClassName.primitiveArrayId() }
    val elementTypeByUnsignedArrayType = unsignedArrayTypeByElementType.inverseMap()

    val Continuation =
        ClassId(StandardNames.COROUTINES_PACKAGE_FQ_NAME_RELEASE, StandardNames.CONTINUATION_INTERFACE_FQ_NAME_RELEASE.shortName())

    @Suppress("FunctionName")
    fun FunctionN(n: Int): ClassId {
        return "Function$n".baseId()
    }

    val Iterator = "Iterator".collectionsId()
    val Iterable = "Iterable".collectionsId()
    val Collection = "Collection".collectionsId()
    val List = "List".collectionsId()
    val ListIterator = "ListIterator".collectionsId()
    val Set = "Set".collectionsId()
    val Map = "Map".collectionsId()
    val MutableIterator = "MutableIterator".collectionsId()

    val MutableIterable = "MutableIterable".collectionsId()
    val MutableCollection = "MutableCollection".collectionsId()
    val MutableList = "MutableList".collectionsId()
    val MutableListIterator = "MutableListIterator".collectionsId()
    val MutableSet = "MutableSet".collectionsId()
    val MutableMap = "MutableMap".collectionsId()

    val MapEntry = Map.createNestedClassId(Name.identifier("Entry"))
    val MutableMapEntry = MutableMap.createNestedClassId(Name.identifier("MutableEntry"))

    val extensionFunctionType = "ExtensionFunctionType".baseId()

    val Suppress = "Suppress".baseId()

    val FlexibleNullability = ClassId(FqName("kotlin.internal.ir"), Name.identifier("FlexibleNullability"))
    val EnhancedNullability = ClassId(FqName("kotlin.jvm.internal"), Name.identifier("EnhancedNullability"))

    val PublishedApi = "PublishedApi".baseId()
}

private fun <K, V> Map<K, V>.inverseMap() = entries.associate { (k, v) -> v to k }
