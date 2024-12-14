/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.builtins.StandardNames

object StandardClassIds {
    val BASE_KOTLIN_PACKAGE = FqName("kotlin")
    val BASE_REFLECT_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("reflect"))
    val BASE_COLLECTIONS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("collections"))
    val BASE_SEQUENCES_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("sequences"))
    val BASE_RANGES_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("ranges"))
    val BASE_JVM_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("jvm"))
    val BASE_ANNOTATIONS_JVM_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("annotations")).child(Name.identifier("jvm"))
    val BASE_JVM_INTERNAL_PACKAGE = BASE_JVM_PACKAGE.child(Name.identifier("internal"))
    val BASE_JVM_FUNCTIONS_PACKAGE = BASE_JVM_PACKAGE.child(Name.identifier("functions"))
    val BASE_ANNOTATION_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("annotation"))
    val BASE_INTERNAL_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("internal"))
    val BASE_INTERNAL_IR_PACKAGE = BASE_INTERNAL_PACKAGE.child(Name.identifier("ir"))
    val BASE_COROUTINES_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("coroutines"))
    val BASE_COROUTINES_INTRINSICS_PACKAGE = BASE_COROUTINES_PACKAGE.child(Name.identifier("intrinsics"))
    val BASE_ENUMS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("enums"))
    val BASE_CONTRACTS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("contracts"))
    val BASE_CONCURRENT_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("concurrent"))
    val BASE_CONCURRENT_ATOMICS_PACKAGE = BASE_CONCURRENT_PACKAGE.child(Name.identifier("atomics"))
    val BASE_TEST_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("test"))
    val BASE_TEXT_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("text"))

    val builtInsPackagesWithDefaultNamedImport = setOf(
        BASE_KOTLIN_PACKAGE,
        BASE_COLLECTIONS_PACKAGE,
        BASE_RANGES_PACKAGE,
        BASE_ANNOTATION_PACKAGE,
    )

    val builtInsPackages = setOf(
        BASE_KOTLIN_PACKAGE,
        BASE_COLLECTIONS_PACKAGE,
        BASE_RANGES_PACKAGE,
        BASE_ANNOTATION_PACKAGE,
        BASE_REFLECT_PACKAGE,
        BASE_INTERNAL_PACKAGE,
        BASE_COROUTINES_PACKAGE,
        // TODO: atomic builtins are moving from kotlin.concurrent to kotlin.concurrent.atomics package (see KT-73816),
        // builtins from kotlin.concurrent package are kept till Atomic API is completely moved to kotlin.concurrent.atomics
        // and built with the new bootstrap compiler which provides builtins from the new package.
        BASE_CONCURRENT_PACKAGE,
        BASE_CONCURRENT_ATOMICS_PACKAGE
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
    val KType = "KType".reflectId()

    val Comparable = "Comparable".baseId()
    val Number = "Number".baseId()

    val Function = "Function".baseId()

    fun byName(name: String) = name.baseId()
    fun reflectByName(name: String) = name.reflectId()

    val primitiveTypes = setOf(Boolean, Char, Byte, Short, Int, Long, Float, Double)
    val signedIntegerTypes = setOf(Byte, Short, Int, Long)

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
    val AbstractMap = "AbstractMap".collectionsId()
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
        val RequireKotlin = "RequireKotlin".internalId()

        val ConsistentCopyVisibility = "ConsistentCopyVisibility".baseId()
        val ExposedCopyVisibility = "ExposedCopyVisibility".baseId()

        val HidesMembers = "HidesMembers".internalId()
        val DynamicExtension = "DynamicExtension".internalId()
        val IntrinsicConstEvaluation = "IntrinsicConstEvaluation".internalId()

        val Retention = "Retention".annotationId()
        val Target = "Target".annotationId()
        val Repeatable = "Repeatable".annotationId()
        val MustBeDocumented = "MustBeDocumented".annotationId()

        val Volatile = "Volatile".concurrentId()

        val Test = "Test".testId()

        val RawTypeAnnotation = "RawType".internalIrId()
        val FlexibleNullability = "FlexibleNullability".internalIrId()
        val FlexibleMutability = "FlexibleMutability".internalIrId()
        val FlexibleArrayElementVariance = "FlexibleArrayElementVariance".internalIrId()
        val EnhancedNullability = "EnhancedNullability".jvmInternalId()
        val NoInfer = "NoInfer".internalId()

        val FunctionN = "FunctionN".jvmFunctionsId()

        val InlineOnly = "InlineOnly".internalId()

        val OnlyInputTypes = "OnlyInputTypes".internalId()

        val RestrictsSuspension = "RestrictsSuspension".coroutinesId()

        val WasExperimental = "WasExperimental".baseId()

        val AccessibleLateinitPropertyLiteral = "AccessibleLateinitPropertyLiteral".internalId()

        val OptionalExpectation = "OptionalExpectation".baseId()
        val ImplicitlyActualizedByJvmDeclaration = "ImplicitlyActualizedByJvmDeclaration".jvmId()
        val KotlinActual = "KotlinActual".annotationsJvmId()

        val jvmStatic = "JvmStatic".jvmId()

        val AssociatedObjectKey = "AssociatedObjectKey".reflectId()
        val ExperimentalAssociatedObjects = "ExperimentalAssociatedObjects".reflectId()

        val associatedObjectAnnotations = hashSetOf(AssociatedObjectKey, ExperimentalAssociatedObjects)

        val ActualizeByJvmBuiltinProvider = "ActualizeByJvmBuiltinProvider".internalId()

        val JvmBuiltin = "JvmBuiltin".internalId()
        val SuppressBytecodeGeneration = "SuppressBytecodeGeneration".internalId()

        object ParameterNames {
            val value = Name.identifier("value")

            val retentionValue = value
            val targetAllowedTargets = Name.identifier("allowedTargets")

            val sinceKotlinVersion = Name.identifier("version")

            val deprecatedMessage = Name.identifier("message")
            val deprecatedLevel = Name.identifier("level")

            val deprecatedSinceKotlinWarningSince = Name.identifier("warningSince")
            val deprecatedSinceKotlinErrorSince = Name.identifier("errorSince")
            val deprecatedSinceKotlinHiddenSince = Name.identifier("hiddenSince")

            val suppressNames = Name.identifier("names")

            val parameterNameName = StandardNames.NAME
        }
    }

    object Callables {
        val suspend = "suspend".callableId(BASE_KOTLIN_PACKAGE)
        val coroutineContext = "coroutineContext".callableId(BASE_COROUTINES_PACKAGE)

        val clone = "clone".callableId(Cloneable)

        val not = "not".callableId(Boolean)

        val contract = "contract".callableId(BASE_CONTRACTS_PACKAGE)
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

    val allBuiltinTypes = primitiveTypes + unsignedTypes + this.String + this.Unit + this.Any + this.Enum
}

private fun String.baseId() = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier(this))
private fun ClassId.unsignedId() = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("U" + shortClassName.identifier))
private fun String.reflectId() = ClassId(StandardClassIds.BASE_REFLECT_PACKAGE, Name.identifier(this))
private fun Name.primitiveArrayId() = ClassId(StandardClassIds.Array.packageFqName, Name.identifier(identifier + StandardClassIds.Array.shortClassName.identifier))
private fun String.collectionsId() = ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier(this))
private fun String.rangesId() = ClassId(StandardClassIds.BASE_RANGES_PACKAGE, Name.identifier(this))
private fun String.annotationId() = ClassId(StandardClassIds.BASE_ANNOTATION_PACKAGE, Name.identifier(this))
private fun String.jvmId() = ClassId(StandardClassIds.BASE_JVM_PACKAGE, Name.identifier(this))
private fun String.annotationsJvmId() = ClassId(StandardClassIds.BASE_ANNOTATIONS_JVM_PACKAGE, Name.identifier(this))
private fun String.jvmInternalId() = ClassId(StandardClassIds.BASE_JVM_INTERNAL_PACKAGE, Name.identifier(this))
private fun String.jvmFunctionsId() = ClassId(StandardClassIds.BASE_JVM_FUNCTIONS_PACKAGE, Name.identifier(this))
private fun String.internalId() = ClassId(StandardClassIds.BASE_INTERNAL_PACKAGE, Name.identifier(this))
private fun String.internalIrId() = ClassId(StandardClassIds.BASE_INTERNAL_IR_PACKAGE, Name.identifier(this))
private fun String.coroutinesId() = ClassId(StandardClassIds.BASE_COROUTINES_PACKAGE, Name.identifier(this))
private fun String.enumsId() = ClassId(StandardClassIds.BASE_ENUMS_PACKAGE, Name.identifier(this))
private fun String.concurrentId() = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier(this))

private fun String.testId() = ClassId(StandardClassIds.BASE_TEST_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
private fun String.callableId(classId: ClassId) = CallableId(classId, Name.identifier(this))

private fun <K, V> Map<K, V>.inverseMap(): Map<V, K> = entries.associate { (k, v) -> v to k }
