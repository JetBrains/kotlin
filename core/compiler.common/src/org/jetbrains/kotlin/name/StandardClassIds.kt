/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

object StandardClassIds {
    val BASE_KOTLIN_PACKAGE = FqName("kotlin")
    val BASE_REFLECT_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("reflect"))
    val BASE_COLLECTIONS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("collections"))
    val BASE_RANGES_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("ranges"))
    val BASE_JVM_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("jvm"))
    val BASE_JVM_INTERNAL_PACKAGE = BASE_JVM_PACKAGE.child(Name.identifier("internal"))
    val BASE_ANNOTATION_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("annotation"))
    val BASE_INTERNAL_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("internal"))
    val BASE_INTERNAL_IR_PACKAGE = BASE_INTERNAL_PACKAGE.child(Name.identifier("ir"))
    val BASE_COROUTINES_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("coroutines"))
    val BASE_ENUMS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("enums"))
    val BASE_CONTRACTS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("contracts"))
    val BASE_CONCURRENT_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("concurrent"))

    val builtInsPackages = setOf(
        BASE_KOTLIN_PACKAGE,
        BASE_COLLECTIONS_PACKAGE,
        BASE_RANGES_PACKAGE,
        BASE_ANNOTATION_PACKAGE,
        BASE_REFLECT_PACKAGE,
        BASE_INTERNAL_PACKAGE,
        BASE_COROUTINES_PACKAGE
    )

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

    val CharSequence = "CharSequence".baseId()
    val String = "String".baseId()
    val Throwable = "Throwable".baseId()

    val Cloneable = "Cloneable".baseId()

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

    val primitiveTypes = setOf(Boolean, Char, Byte, Short, Int, Long, Float, Double)

    val primitiveArrayTypeByElementType = primitiveTypes.associateWith { id -> id.shortClassName.primitiveArrayId() }
    val elementTypeByPrimitiveArrayType = primitiveArrayTypeByElementType.inverseMap()

    val unsignedTypes = setOf(UByte, UShort, UInt, ULong)
    val unsignedArrayTypeByElementType = unsignedTypes.associateWith { id -> id.shortClassName.primitiveArrayId() }
    val elementTypeByUnsignedArrayType = unsignedArrayTypeByElementType.inverseMap()

    val constantAllowedTypes = primitiveTypes + unsignedTypes + String

    val Continuation = "Continuation".coroutinesId()

    @Suppress("FunctionName")
    fun FunctionN(n: Int): ClassId {
        return "Function$n".baseId()
    }

    @Suppress("FunctionName")
    fun SuspendFunctionN(n: Int): ClassId {
        return "SuspendFunction$n".coroutinesId()
    }

    @Suppress("FunctionName")
    fun KFunctionN(n: Int): ClassId {
        return "KFunction$n".reflectId()
    }

    @Suppress("FunctionName")
    fun KSuspendFunctionN(n: Int): ClassId {
        return "KSuspendFunction$n".reflectId()
    }

    val Iterator = "Iterator".collectionsId()
    val Iterable = "Iterable".collectionsId()
    val Collection = "Collection".collectionsId()
    val List = "List".collectionsId()
    val ListIterator = "ListIterator".collectionsId()
    val Set = "Set".collectionsId()
    val Map = "Map".collectionsId()
    val MutableIterator = "MutableIterator".collectionsId()
    val CharIterator = "CharIterator".collectionsId()

    val MutableIterable = "MutableIterable".collectionsId()
    val MutableCollection = "MutableCollection".collectionsId()
    val MutableList = "MutableList".collectionsId()
    val MutableListIterator = "MutableListIterator".collectionsId()
    val MutableSet = "MutableSet".collectionsId()
    val MutableMap = "MutableMap".collectionsId()

    val MapEntry = Map.createNestedClassId(Name.identifier("Entry"))
    val MutableMapEntry = MutableMap.createNestedClassId(Name.identifier("MutableEntry"))

    val Result = "Result".baseId()

    val IntRange = "IntRange".rangesId()
    val LongRange = "LongRange".rangesId()
    val CharRange = "CharRange".rangesId()

    val AnnotationRetention = "AnnotationRetention".annotationId()
    val AnnotationTarget = "AnnotationTarget".annotationId()
    val DeprecationLevel = "DeprecationLevel".baseId()

    val EnumEntries = "EnumEntries".enumsId()

    object Annotations {
        val Suppress = "Suppress".baseId()
        val PublishedApi = "PublishedApi".baseId()
        val SinceKotlin = "SinceKotlin".baseId()
        val ExtensionFunctionType = "ExtensionFunctionType".baseId()
        val ContextFunctionTypeParams = "ContextFunctionTypeParams".baseId()
        val Deprecated = "Deprecated".baseId()
        val DeprecatedSinceKotlin = "DeprecatedSinceKotlin".baseId()

        val HidesMembers = "HidesMembers".internalId()
        val DynamicExtension = "DynamicExtension".internalId()

        val Retention = "Retention".annotationId()
        val Target = "Target".annotationId()
        val Repeatable = "Repeatable".annotationId()
        val MustBeDocumented = "MustBeDocumented".annotationId()

        val JvmStatic = "JvmStatic".jvmId()
        val JvmName = "JvmName".jvmId()
        val JvmField = "JvmField".jvmId()
        val JvmDefault = "JvmDefault".jvmId()
        val JvmRepeatable = "JvmRepeatable".jvmId()
        val JvmRecord = "JvmRecord".jvmId()
        val JvmVolatile = "Volatile".jvmId()
        val Throws = "Throws".jvmId()

        val Volatile = "Volatile".concurrentId()

        val RawTypeAnnotation = "RawType".internalIrId()
        val FlexibleNullability = "FlexibleNullability".internalIrId()
        val EnhancedNullability = "EnhancedNullability".jvmInternalId()

        val InlineOnly = "InlineOnly".internalId()

        val OnlyInputTypes = "OnlyInputTypes".internalId()

        val RestrictsSuspension = "RestrictsSuspension".coroutinesId()

        val WasExperimental = "WasExperimental".baseId()

        val AccessibleLateinitPropertyLiteral = "AccessibleLateinitPropertyLiteral".internalId()

        object Java {
            val Deprecated = "Deprecated".javaLangId()
            val Repeatable = "Repeatable".javaAnnotationId()
            val Retention = "Retention".javaAnnotationId()
            val Documented = "Documented".javaAnnotationId()
            val Target = "Target".javaAnnotationId()
            val ElementType = "ElementType".javaAnnotationId()
            val RetentionPolicy = "RetentionPolicy".javaAnnotationId()
        }

        object ParameterNames {
            val value = Name.identifier("value")

            val retentionValue = value
            val targetAllowedTargets = Name.identifier("allowedTargets")
            val jvmNameName = Name.identifier("name")

            val sinceKotlinVersion = Name.identifier("version")

            val deprecatedMessage = Name.identifier("message")
            val deprecatedLevel = Name.identifier("level")

            val deprecatedSinceKotlinWarningSince = Name.identifier("warningSince")
            val deprecatedSinceKotlinErrorSince = Name.identifier("errorSince")
            val deprecatedSinceKotlinHiddenSince = Name.identifier("hiddenSince")

            val parameterNameName = jvmNameName
        }
    }

    object Callables {
        val suspend = "suspend".callableId(BASE_KOTLIN_PACKAGE)
        val coroutineContext = "coroutineContext".callableId(BASE_COROUTINES_PACKAGE)

        val clone = "clone".callableId(Cloneable)

        val not = "not".callableId(Boolean)

        val contract = "contract".callableId(BASE_CONTRACTS_PACKAGE)
    }

    object Java {
        val Record = "Record".javaLangId()
    }

    object Collections {
        val baseCollectionToMutableEquivalent: Map<ClassId, ClassId> = mapOf(
            StandardClassIds.Iterable to StandardClassIds.MutableIterable,
            StandardClassIds.Iterator to StandardClassIds.MutableIterator,
            StandardClassIds.ListIterator to StandardClassIds.MutableListIterator,
            StandardClassIds.List to StandardClassIds.MutableList,
            StandardClassIds.Collection to StandardClassIds.MutableCollection,
            StandardClassIds.Set to StandardClassIds.MutableSet,
            StandardClassIds.Map to StandardClassIds.MutableMap,
            StandardClassIds.MapEntry to StandardClassIds.MutableMapEntry
        )

        val mutableCollectionToBaseCollection: Map<ClassId, ClassId> =
            baseCollectionToMutableEquivalent.entries.associateBy({ it.value }) { it.key }
    }
}

private fun String.baseId() = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier(this))
private fun ClassId.unsignedId() = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("U" + shortClassName.identifier))
private fun String.reflectId() = ClassId(StandardClassIds.BASE_REFLECT_PACKAGE, Name.identifier(this))
private fun Name.primitiveArrayId() = ClassId(StandardClassIds.Array.packageFqName, Name.identifier(identifier + StandardClassIds.Array.shortClassName.identifier))
private fun String.collectionsId() = ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier(this))
private fun String.rangesId() = ClassId(StandardClassIds.BASE_RANGES_PACKAGE, Name.identifier(this))
private fun String.annotationId() = ClassId(StandardClassIds.BASE_ANNOTATION_PACKAGE, Name.identifier(this))
private fun String.jvmId() = ClassId(StandardClassIds.BASE_JVM_PACKAGE, Name.identifier(this))
private fun String.jvmInternalId() = ClassId(StandardClassIds.BASE_JVM_INTERNAL_PACKAGE, Name.identifier(this))
private fun String.internalId() = ClassId(StandardClassIds.BASE_INTERNAL_PACKAGE, Name.identifier(this))
private fun String.internalIrId() = ClassId(StandardClassIds.BASE_INTERNAL_IR_PACKAGE, Name.identifier(this))
private fun String.coroutinesId() = ClassId(StandardClassIds.BASE_COROUTINES_PACKAGE, Name.identifier(this))
private fun String.enumsId() = ClassId(StandardClassIds.BASE_ENUMS_PACKAGE, Name.identifier(this))
private fun String.concurrentId() = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
private fun String.callableId(classId: ClassId) = CallableId(classId, Name.identifier(this))

private val JAVA_LANG_PACKAGE = FqName("java.lang")
private val JAVA_LANG_ANNOTATION_PACKAGE = JAVA_LANG_PACKAGE.child(Name.identifier("annotation"))

private fun String.javaLangId() = ClassId(JAVA_LANG_PACKAGE, Name.identifier(this))
private fun String.javaAnnotationId() = ClassId(JAVA_LANG_ANNOTATION_PACKAGE, Name.identifier(this))

private fun <K, V> Map<K, V>.inverseMap(): Map<V, K> = entries.associate { (k, v) -> v to k }
