/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * Consult KDoc of [StandardClassIds] regarding naming rules when adding declarations here.
 * Also, check if the desired id is already present there.
 */
object JvmStandardClassIds {
    val JAVA_LANG_PACKAGE = FqName("java.lang")
    val JAVA_LANG_ANNOTATION_PACKAGE = JAVA_LANG_PACKAGE.child(Name.identifier("annotation"))
    val JAVA_UTIL_CONCURRENT_ATOMIC_PACKAGE = FqName("java.util.concurrent.atomic")

    val BoxingConstructorMarker = "BoxingConstructorMarker".jvmInternalId()

    val Object = "Object".javaLangId()
    val Record = "Record".javaLangId()
    val Annotation_java = "Annotation".javaLangAnnotationId()
    val ElementType = "ElementType".javaLangAnnotationId()
    val RetentionPolicy = "RetentionPolicy".javaLangAnnotationId()

    val AtomicBoolean_java = "AtomicBoolean".javaUtilConcurrentAtomicId()
    val AtomicInteger = "AtomicInteger".javaUtilConcurrentAtomicId()
    val AtomicLong_java = "AtomicLong".javaUtilConcurrentAtomicId()
    val AtomicReference_java = "AtomicReference".javaUtilConcurrentAtomicId()

    val atomicByPrimitive_java = mapOf(
        StandardClassIds.Boolean to AtomicBoolean_java,
        StandardClassIds.Int to AtomicInteger,
        StandardClassIds.Long to AtomicLong_java,
    )

    val AtomicReferenceArray = "AtomicReferenceArray".javaUtilConcurrentAtomicId()
    val AtomicIntegerArray = "AtomicIntegerArray".javaUtilConcurrentAtomicId()
    val AtomicLongArray_java = "AtomicLongArray".javaUtilConcurrentAtomicId()

    val atomicArrayByPrimitive_java = mapOf(
        StandardClassIds.Int to AtomicIntegerArray,
        StandardClassIds.Long to AtomicLongArray_java,
    )

    const val MULTIFILE_PART_NAME_DELIMITER = "__"

    object Annotations {
        val JvmStatic = "JvmStatic".jvmId()
        val JvmName = "JvmName".jvmId()
        val JvmField = "JvmField".jvmId()
        val JvmOverloads = "JvmOverloads".jvmId()
        val JvmSynthetic = "JvmSynthetic".jvmId()
        val JvmRecord = "JvmRecord".jvmId()
        val JvmRepeatable = "JvmRepeatable".jvmId()
        val JvmSuppressWildcards = "JvmSuppressWildcards".jvmId()
        val JvmWildcard = "JvmWildcard".jvmId()
        val JvmSerializableLambda = "JvmSerializableLambda".jvmId()
        val JvmExposeBoxed = "JvmExposeBoxed".jvmId()

        val JvmMultifileClass = "JvmMultifileClass".jvmId()
        val JvmPackageName = "JvmPackageName".jvmId()

        val JvmDefault = "JvmDefault".jvmId()
        val JvmDefaultWithCompatibility = "JvmDefaultWithCompatibility".jvmId()
        val JvmDefaultWithoutCompatibility = "JvmDefaultWithoutCompatibility".jvmId()

        val Synchronized = "Synchronized".jvmId()
        val Strictfp = "Strictfp".jvmId()
        val Volatile = "Volatile".jvmId()
        val Transient = "Transient".jvmId()

        /**
         * `kotlin.jvm.Throws`: the class that the common `kotlin.Throws` ([StandardClassIds.Annotations.Throws])
         * is a typealias to on JVM.
         */
        val Throws_aliased = "Throws".jvmId()

        val Deprecated_java = "Deprecated".javaLangId()
        val Repeatable_java = "Repeatable".javaLangAnnotationId()
        val Retention_java = "Retention".javaLangAnnotationId()
        val Documented = "Documented".javaLangAnnotationId()
        val Target_java = "Target".javaLangAnnotationId()

        object ParameterNames {
            val JvmExposeBoxed_jvmName = Name.identifier("jvmName")
        }
    }

    object Callables {
        val javaClass = "javaClass".callableId(StandardClassIds.BASE_JVM_PACKAGE)

        val AtomicReference_compareAndSet_java = "compareAndSet".callableId(AtomicReference_java)
        val AtomicReference_weakCompareAndSet_java = "weakCompareAndSet".callableId(AtomicReference_java)
        val AtomicReference_weakCompareAndSetAcquire_java = "weakCompareAndSetAcquire".callableId(AtomicReference_java)
        val AtomicReference_weakCompareAndSetRelease_java = "weakCompareAndSetRelease".callableId(AtomicReference_java)
        val AtomicReference_weakCompareAndSetPlain_java = "weakCompareAndSetPlain".callableId(AtomicReference_java)
        val AtomicReference_weakCompareAndSetVolatile_java = "weakCompareAndSetVolatile".callableId(AtomicReference_java)
        val AtomicReference_compareAndExchange_java = "compareAndExchange".callableId(AtomicReference_java)
        val AtomicReference_compareAndExchangeAcquire_java = "compareAndExchangeAcquire".callableId(AtomicReference_java)
        val AtomicReference_compareAndExchangeRelease_java = "compareAndExchangeRelease".callableId(AtomicReference_java)

        val AtomicReferenceArray_compareAndSet = "compareAndSet".callableId(AtomicReferenceArray)
        val AtomicReferenceArray_weakCompareAndSet = "weakCompareAndSet".callableId(AtomicReferenceArray)
        val AtomicReferenceArray_weakCompareAndSetAcquire = "weakCompareAndSetAcquire".callableId(AtomicReferenceArray)
        val AtomicReferenceArray_weakCompareAndSetRelease = "weakCompareAndSetRelease".callableId(AtomicReferenceArray)
        val AtomicReferenceArray_weakCompareAndSetPlain = "weakCompareAndSetPlain".callableId(AtomicReferenceArray)
        val AtomicReferenceArray_weakCompareAndSetVolatile = "weakCompareAndSetVolatile".callableId(AtomicReferenceArray)
        val AtomicReferenceArray_compareAndExchange = "compareAndExchange".callableId(AtomicReferenceArray)
        val AtomicReferenceArray_compareAndExchangeAcquire = "compareAndExchangeAcquire".callableId(AtomicReferenceArray)
        val AtomicReferenceArray_compareAndExchangeRelease = "compareAndExchangeRelease".callableId(AtomicReferenceArray)
    }
}

private fun String.jvmId() = ClassId(StandardClassIds.BASE_JVM_PACKAGE, Name.identifier(this))
private fun String.jvmInternalId() = ClassId(StandardClassIds.BASE_JVM_INTERNAL_PACKAGE, Name.identifier(this))
private fun String.javaLangId() = ClassId(JvmStandardClassIds.JAVA_LANG_PACKAGE, Name.identifier(this))
private fun String.javaLangAnnotationId() = ClassId(JvmStandardClassIds.JAVA_LANG_ANNOTATION_PACKAGE, Name.identifier(this))
private fun String.javaUtilConcurrentAtomicId() = ClassId(JvmStandardClassIds.JAVA_UTIL_CONCURRENT_ATOMIC_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
private fun String.callableId(classId: ClassId) = CallableId(classId, Name.identifier(this))
