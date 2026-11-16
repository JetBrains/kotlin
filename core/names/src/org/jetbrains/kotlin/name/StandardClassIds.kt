/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * Don't forget to run `./gradlew :core:names:updateKotlinAbi` after changes in this object.
 *
 * ## Placement
 *
 * - Classes, interfaces, enums and objects go directly into the object ([Any], [Map_Entry], [AnnotationTarget]).
 * - Annotation classes go into `Annotations` ([Annotations.Deprecated]).
 * - Callables go into `Callables` ([Callables.suspend]).
 * - Annotation parameter names go into `Annotations.ParameterNames` ([Annotations.ParameterNames.Deprecated_message]).
 * - Package constants go directly into the object.
 *
 * ## Naming rules
 *
 * 1. An id is named exactly as the declaration it refers to:
 *    PascalCase for classes (`Any`, `JvmName`), and the declared name for callables and parameters (`suspend`, `javaClass`).
 *    Classes with lowercase names keep them as well (`JsStandardClassIds.Annotations.nativeGetter`).
 * 2. `_` goes one level down, mirroring the structure of the id:
 *    - a nested class is `Outer_Nested`: [Map_Entry], [Annotations.JsExport_Ignore];
 *    - a member callable is `Owner_member`: [Callables.Cloneable_clone], [Callables.AtomicReference_compareAndSet];
 *    - an annotation parameter is `Annotation_parameter`: [Annotations.ParameterNames.Deprecated_message].
 *    Top-level callables have no owner prefix: [Callables.suspend].
 * 3. A trailing `_suffix` disambiguates an id whose name is also used by another declaration. Disambiguation always comes last,
 *    after all nesting segments, and members of a disambiguated class carry its suffix (`AtomicReference_java` ->
 *    `AtomicReference_compareAndSet_java`), e.g.
 *    - `_java`: a `java.*` class (`JvmStandardClassIds.Annotations.Deprecated_java` vs [Annotations.Deprecated]);
 *    - `_native`: a Native-only class (`NativeStandardClassIds.AtomicInt_native`, i.e. `kotlin.concurrent.AtomicInt`,
 *      vs [AtomicInt], i.e. `kotlin.concurrent.atomics.AtomicInt`);
 *    Ids without a collision get no suffix (`JvmStandardClassIds.Object`).
 * 4. A package constant is named after its path: a leading `kotlin` becomes `BASE`, every other segment is uppercased
 *    as is (camelCase segments are not split into words), segments are joined with `_`, and `_PACKAGE` is appended:
 *    [BASE_COROUTINES_INTRINSICS_PACKAGE] is `kotlin.coroutines.intrinsics`,
 *    `NativeStandardClassIds.BASE_NATIVE_INTERNAL_ESCAPEANALYSIS_PACKAGE` is `kotlin.native.internal.escapeAnalysis`,
 *    `JvmStandardClassIds.JAVA_LANG_ANNOTATION_PACKAGE` is `java.lang.annotation`.
 *    The only exception is `kotlin` itself, which is [BASE_KOTLIN_PACKAGE].
 * 5. Unlike the platform-specific `*StandardClassIds`, this one is public API: `:core:names` has ABI validation
 *    (`core/names/api/names.api`), so be careful when renaming or removing anything here.
 *
 * Prefer referring to ids qualified (`StandardClassIds.Annotations.Throws`): member imports hide which object (and thus which
 * declaration) a name comes from.
 */
object StandardClassIds {
    val BASE_KOTLIN_PACKAGE = FqName("kotlin")
    val BASE_REFLECT_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("reflect"))
    val BASE_EXPERIMENTAL_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("experimental"))
    val BASE_COLLECTIONS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("collections"))
    val BASE_SEQUENCES_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("sequences"))
    val BASE_RANGES_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("ranges"))
    val BASE_JVM_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("jvm"))
    val BASE_JS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("js"))
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
        BASE_CONCURRENT_ATOMICS_PACKAGE
    )

    val Nothing = "Nothing".baseId()
    val Unit = "Unit".baseId()
    val Any = "Any".baseId()
    val NonError = "NonError".baseId()
    val RichError = "RichError".baseId()
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

    val Sequence = "Sequence".sequencesId()

    val Comparable = "Comparable".baseId()
    val Number = "Number".baseId()

    val Function = "Function".baseId()
    val SuspendFunction = "SuspendFunction".coroutinesId()
    val FunctionN = "FunctionN".jvmFunctionsId()

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
    val allIntegerTypes = signedIntegerTypes + unsignedTypes

    val Continuation = "Continuation".coroutinesId()
    val CoroutineContext = "CoroutineContext".coroutinesId()

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

    val Map_Entry = Map.createNestedClassId(Name.identifier("Entry"))
    val MutableMap_MutableEntry = MutableMap.createNestedClassId(Name.identifier("MutableEntry"))

    val Result = "Result".baseId()

    val IntRange = "IntRange".rangesId()
    val LongRange = "LongRange".rangesId()
    val CharRange = "CharRange".rangesId()

    val AnnotationRetention = "AnnotationRetention".annotationId()
    val AnnotationTarget = "AnnotationTarget".annotationId()
    val DeprecationLevel = "DeprecationLevel".baseId()

    val EnumEntries = "EnumEntries".enumsId()

    val AtomicBoolean = "AtomicBoolean".concurrentAtomicsId()
    val AtomicInt = "AtomicInt".concurrentAtomicsId()
    val AtomicLong = "AtomicLong".concurrentAtomicsId()
    val AtomicReference = "AtomicReference".concurrentAtomicsId()

    val atomicByPrimitive = mapOf(
        Boolean to AtomicBoolean,
        Int to AtomicInt,
        Long to AtomicLong,
    )

    val AtomicArray = "AtomicArray".concurrentAtomicsId()
    val AtomicIntArray = "AtomicIntArray".concurrentAtomicsId()
    val AtomicLongArray = "AtomicLongArray".concurrentAtomicsId()

    val atomicArrayByPrimitive = mapOf(
        Int to AtomicIntArray,
        Long to AtomicLongArray,
    )

    val ClassCastException = "ClassCastException".baseId()

    @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Map_Entry"), level = ERROR)
    val MapEntry get() = Map_Entry

    @Deprecated("Deprecated in 2.5.20.", ReplaceWith("MutableMap_MutableEntry"), level = ERROR)
    val MutableMapEntry get() = MutableMap_MutableEntry

    object Annotations {
        val Suppress = "Suppress".baseId()
        val PublishedApi = "PublishedApi".baseId()
        val SinceKotlin = "SinceKotlin".baseId()
        val ExtensionFunctionType = "ExtensionFunctionType".baseId()
        val ContextFunctionTypeParams = "ContextFunctionTypeParams".baseId()
        val Deprecated = "Deprecated".baseId()
        val DeprecatedSinceKotlin = "DeprecatedSinceKotlin".baseId()
        val RequireKotlin = "RequireKotlin".internalId()
        val DslMarker = "DslMarker".baseId()
        val IntroducedAt = "IntroducedAt".baseId()

        val NumericClass = "NumericClass".baseId()

        val LowPriorityInOverloadResolution = "LowPriorityInOverloadResolution".internalId()

        val ConsistentCopyVisibility = "ConsistentCopyVisibility".baseId()
        val ExposedCopyVisibility = "ExposedCopyVisibility".baseId()

        val WillBecomeValue = "WillBecomeValue".baseId()

        val HidesMembers = "HidesMembers".internalId()
        val DynamicExtension = "DynamicExtension".internalId()
        val IntrinsicConstEvaluation = "IntrinsicConstEvaluation".internalId()

        val Retention = "Retention".annotationId()
        val Target = "Target".annotationId()
        val Repeatable = "Repeatable".annotationId()
        val MustBeDocumented = "MustBeDocumented".annotationId()
        val ExpectRefinement = "ExpectRefinement".experimentalId()

        val Volatile = "Volatile".concurrentId()

        /**
         * `kotlin.Throws`: an `expect` class.
         * On JVM, it is a typealias to `kotlin.jvm.Throws` (`JvmStandardClassIds.Annotations.Throws_aliased`).
         * On Native, it is a class aliased by `kotlin.native.Throws` (`NativeStandardClassIds.Annotations.Throws_alias`).
         */
        val Throws = "Throws".baseId()

        val Test = "Test".testId()

        val RawType = "RawType".internalIrId()
        val FlexibleNullability = "FlexibleNullability".internalIrId()
        val FlexibleMutability = "FlexibleMutability".internalIrId()
        val FlexibleArrayElementVariance = "FlexibleArrayElementVariance".internalIrId()
        val EnhancedNullability = "EnhancedNullability".jvmInternalId()
        val NoInfer = "NoInfer".internalId()

        val InlineOnly = "InlineOnly".internalId()

        val OnlyInputTypes = "OnlyInputTypes".internalId()

        val RestrictsSuspension = "RestrictsSuspension".coroutinesId()

        val WasExperimental = "WasExperimental".baseId()

        val MustUseReturnValues = "MustUseReturnValues".baseId()
        val IgnorableReturnValue = "IgnorableReturnValue".baseId()

        val EqualityBound = "EqualityBound".baseId()

        val AccessibleLateinitPropertyLiteral = "AccessibleLateinitPropertyLiteral".internalId()

        val OptionalExpectation = "OptionalExpectation".baseId()
        val ImplicitlyActualizedByJvmDeclaration = "ImplicitlyActualizedByJvmDeclaration".jvmId()
        val KotlinActual = "KotlinActual".annotationsJvmId()

        val JvmName = "JvmName".jvmId()
        val JvmStatic = "JvmStatic".jvmId()

        val Transient = "Transient".jvmId()
        val JvmInline = "JvmInline".jvmId()

        val JsExport = "JsExport".jsId()
        val JsImplicitExport = "JsImplicitExport".jsId()
        val JsExport_Ignore = JsExport.createNestedClassId(Name.identifier("Ignore"))
        val JsExport_Default = JsExport.createNestedClassId(Name.identifier("Default"))
        val JsNoDispatchReceiver = "JsNoDispatchReceiver".jsId()
        val JsNoRuntime = "JsNoRuntime".jsId()

        val AssociatedObjectKey = "AssociatedObjectKey".reflectId()
        val ExperimentalAssociatedObjects = "ExperimentalAssociatedObjects".reflectId()

        val associatedObjectAnnotations = hashSetOf(AssociatedObjectKey, ExperimentalAssociatedObjects)

        val JvmBuiltin = "JvmBuiltin".internalId()
        val SuppressBytecodeGeneration = "SuppressBytecodeGeneration".internalId()

        val UsedFromCompilerGeneratedCode = "UsedFromCompilerGeneratedCode".internalId()

        /**
         * `kotlin.internal.ReflectionPackageName`: a file annotation making the backend store the given package name
         * in the reflective information of the classes declared in the file, instead of the real one.
         *
         * Used by the test infrastructure, which renames packages when compiling several tests into one batch.
         */
        val ReflectionPackageName = "ReflectionPackageName".internalId()

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("JsExport_Ignore"), level = ERROR)
        val jsExportIgnore get() = JsExport_Ignore

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("JsExport_Default"), level = ERROR)
        val jsExportDefault get() = JsExport_Default

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("StandardClassIds.FunctionN"), level = ERROR)
        val FunctionN get() = StandardClassIds.FunctionN

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("RawType"), level = ERROR)
        val RawTypeAnnotation get() = RawType

        object ParameterNames {
            val value = Name.identifier("value")

            val Retention_value = value
            val Target_allowedTargets = Name.identifier("allowedTargets")

            val EqualityBound_bound = Name.identifier("bound")

            val SinceKotlin_version = Name.identifier("version")

            val NumericClass_actualizations = Name.identifier("actualizations")

            val Deprecated_message = Name.identifier("message")
            val Deprecated_level = Name.identifier("level")
            val Deprecated_replaceWith = Name.identifier("replaceWith")

            val ReplaceWith_expression = Name.identifier("expression")
            val ReplaceWith_imports = Name.identifier("imports")

            val DeprecatedSinceKotlin_warningSince = Name.identifier("warningSince")
            val DeprecatedSinceKotlin_errorSince = Name.identifier("errorSince")
            val DeprecatedSinceKotlin_hiddenSince = Name.identifier("hiddenSince")

            val Suppress_names = Name.identifier("names")

            val ParameterName_name = Name.identifier("name")

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Retention_value"), level = ERROR)
            val retentionValue get() = Retention_value

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Target_allowedTargets"), level = ERROR)
            val targetAllowedTargets get() = Target_allowedTargets

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("EqualityBound_bound"), level = ERROR)
            val equalityBound get() = EqualityBound_bound

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("SinceKotlin_version"), level = ERROR)
            val sinceKotlinVersion get() = SinceKotlin_version

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("NumericClass_actualizations"), level = ERROR)
            val actualizations get() = NumericClass_actualizations

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Deprecated_message"), level = ERROR)
            val deprecatedMessage get() = Deprecated_message

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Deprecated_level"), level = ERROR)
            val deprecatedLevel get() = Deprecated_level

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Deprecated_replaceWith"), level = ERROR)
            val deprecatedReplaceWith get() = Deprecated_replaceWith

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("ReplaceWith_expression"), level = ERROR)
            val replaceWithExpression get() = ReplaceWith_expression

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("ReplaceWith_imports"), level = ERROR)
            val replaceWithImports get() = ReplaceWith_imports

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("DeprecatedSinceKotlin_warningSince"), level = ERROR)
            val deprecatedSinceKotlinWarningSince get() = DeprecatedSinceKotlin_warningSince

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("DeprecatedSinceKotlin_errorSince"), level = ERROR)
            val deprecatedSinceKotlinErrorSince get() = DeprecatedSinceKotlin_errorSince

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("DeprecatedSinceKotlin_hiddenSince"), level = ERROR)
            val deprecatedSinceKotlinHiddenSince get() = DeprecatedSinceKotlin_hiddenSince

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Suppress_names"), level = ERROR)
            val suppressNames get() = Suppress_names

            @Deprecated("Deprecated in 2.5.20.", ReplaceWith("ParameterName_name"), level = ERROR)
            val parameterNameName get() = ParameterName_name
        }
    }

    object Callables {
        val suspend = "suspend".callableId(BASE_KOTLIN_PACKAGE)
        val coroutineContext = "coroutineContext".callableId(BASE_COROUTINES_PACKAGE)

        val Cloneable_clone = "clone".callableId(Cloneable)

        val Boolean_not = "not".callableId(Boolean)

        val contract = "contract".callableId(BASE_CONTRACTS_PACKAGE)

        val AtomicReference_compareAndSet = "compareAndSet".callableId(AtomicReference)
        val AtomicReference_compareAndExchange = "compareAndExchange".callableId(AtomicReference)
        val AtomicArray_compareAndSetAt = "compareAndSetAt".callableId(AtomicArray)
        val AtomicArray_compareAndExchangeAt = "compareAndExchangeAt".callableId(AtomicArray)

        val throwNoWhenBranchMatchedException = "throwNoWhenBranchMatchedException".callableId(BASE_INTERNAL_PACKAGE)

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Cloneable_clone"), level = ERROR)
        val clone get() = Cloneable_clone

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("Boolean_not"), level = ERROR)
        val not get() = Boolean_not

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("AtomicReference_compareAndSet"), level = ERROR)
        val atomicReferenceCompareAndSet get() = AtomicReference_compareAndSet

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("AtomicReference_compareAndExchange"), level = ERROR)
        val atomicReferenceCompareAndExchange get() = AtomicReference_compareAndExchange

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("AtomicArray_compareAndSetAt"), level = ERROR)
        val atomicArrayCompareAndSetAt get() = AtomicArray_compareAndSetAt

        @Deprecated("Deprecated in 2.5.20.", ReplaceWith("AtomicArray_compareAndExchangeAt"), level = ERROR)
        val atomicArrayCompareAndExchangeAt get() = AtomicArray_compareAndExchangeAt
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
            StandardClassIds.Map_Entry to StandardClassIds.MutableMap_MutableEntry
        )

        val mutableCollectionToBaseCollection: Map<ClassId, ClassId> =
            baseCollectionToMutableEquivalent.entries.associateBy({ it.value }) { it.key }
    }

    val allBuiltinTypes = primitiveTypes + unsignedTypes + this.String + this.Unit + this.Any + this.Enum
}

private fun String.baseId() = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier(this))
private fun String.experimentalId() = ClassId(StandardClassIds.BASE_EXPERIMENTAL_PACKAGE, Name.identifier(this))
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
private fun String.concurrentAtomicsId() = ClassId(StandardClassIds.BASE_CONCURRENT_ATOMICS_PACKAGE, Name.identifier(this))
private fun String.sequencesId() = ClassId(StandardClassIds.BASE_SEQUENCES_PACKAGE, Name.identifier(this))

private fun String.testId() = ClassId(StandardClassIds.BASE_TEST_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
private fun String.callableId(classId: ClassId) = CallableId(classId, Name.identifier(this))

private fun String.jsId() = ClassId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))

private fun <K, V> Map<K, V>.inverseMap(): Map<V, K> = entries.associate { [k, v] -> v to k }
