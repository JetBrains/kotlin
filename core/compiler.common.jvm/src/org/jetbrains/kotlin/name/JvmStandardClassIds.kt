/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

object JvmStandardClassIds {
    val BASE_JVM_PACKAGE = StandardClassIds.BASE_KOTLIN_PACKAGE.child(Name.identifier("jvm"))

    @JvmField
    val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")

    @JvmField
    val JVM_NAME_CLASS_ID = ClassId.topLevel(JVM_NAME)

    val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    val JVM_MULTIFILE_CLASS: FqName = FqName("kotlin.jvm.JvmMultifileClass")
    val JVM_MULTIFILE_CLASS_ID: ClassId = ClassId.topLevel(JVM_MULTIFILE_CLASS)
    val JVM_MULTIFILE_CLASS_SHORT = JVM_MULTIFILE_CLASS.shortName().asString()

    val JVM_PACKAGE_NAME: FqName = FqName("kotlin.jvm.JvmPackageName")
    val JVM_PACKAGE_NAME_SHORT = JVM_PACKAGE_NAME.shortName().asString()

    val JVM_DEFAULT_FQ_NAME = FqName("kotlin.jvm.JvmDefault")
    val JVM_DEFAULT_CLASS_ID = ClassId.topLevel(JVM_DEFAULT_FQ_NAME)
    val JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME = FqName("kotlin.jvm.JvmDefaultWithoutCompatibility")
    val JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME = FqName("kotlin.jvm.JvmDefaultWithCompatibility")
    val JVM_DEFAULT_NO_COMPATIBILITY_CLASS_ID = ClassId.topLevel(JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME)
    val JVM_DEFAULT_WITH_COMPATIBILITY_CLASS_ID = ClassId.topLevel(JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME)
    val JVM_OVERLOADS_FQ_NAME = FqName("kotlin.jvm.JvmOverloads")
    val JVM_OVERLOADS_CLASS_ID = ClassId.topLevel(JVM_OVERLOADS_FQ_NAME)
    val JVM_STATIC_FQ_NAME = FqName("kotlin.jvm.JvmStatic")

    val JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmSuppressWildcards")
    val JVM_WILDCARD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmWildcard")

    @JvmField
    val JVM_SYNTHETIC_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmSynthetic")

    @JvmField
    val JVM_SYNTHETIC_ANNOTATION_CLASS_ID = ClassId.topLevel(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)

    @JvmField
    val JVM_RECORD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmRecord")

    @JvmField
    val JVM_RECORD_ANNOTATION_CLASS_ID = ClassId.topLevel(JVM_RECORD_ANNOTATION_FQ_NAME)

    @JvmField
    val SYNCHRONIZED_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Synchronized")

    @JvmField
    val SYNCHRONIZED_ANNOTATION_CLASS_ID = ClassId.topLevel(SYNCHRONIZED_ANNOTATION_FQ_NAME)

    @JvmField
    val THROWS_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Throws")

    @JvmField
    val THROWS_ANNOTATION_CLASS_ID = ClassId.topLevel(THROWS_ANNOTATION_FQ_NAME)

    @JvmField
    val STRICTFP_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Strictfp")

    @JvmField
    val STRICTFP_ANNOTATION_CLASS_ID = ClassId.topLevel(STRICTFP_ANNOTATION_FQ_NAME)

    @JvmField
    val VOLATILE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Volatile")

    @JvmField
    val VOLATILE_ANNOTATION_CLASS_ID = ClassId.topLevel(VOLATILE_ANNOTATION_FQ_NAME)

    @JvmField
    val TRANSIENT_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Transient")

    @JvmField
    val TRANSIENT_ANNOTATION_CLASS_ID = ClassId.topLevel(TRANSIENT_ANNOTATION_FQ_NAME)

    @JvmField
    val ATOMIC_BOOLEAN_FQ_NAME = FqName("java.util.concurrent.atomic.AtomicBoolean")

    @JvmField
    val ATOMIC_BOOLEAN_CLASS_ID = ClassId.topLevel(ATOMIC_BOOLEAN_FQ_NAME)

    @JvmField
    val ATOMIC_INTEGER_FQ_NAME = FqName("java.util.concurrent.atomic.AtomicInteger")

    @JvmField
    val ATOMIC_INTEGER_CLASS_ID = ClassId.topLevel(ATOMIC_INTEGER_FQ_NAME)

    @JvmField
    val ATOMIC_LONG_FQ_NAME = FqName("java.util.concurrent.atomic.AtomicLong")

    @JvmField
    val ATOMIC_LONG_CLASS_ID = ClassId.topLevel(ATOMIC_LONG_FQ_NAME)

    @JvmField
    val ATOMIC_REFERENCE_FQ_NAME = FqName("java.util.concurrent.atomic.AtomicReference")

    @JvmField
    val ATOMIC_REFERENCE_CLASS_ID = ClassId.topLevel(ATOMIC_REFERENCE_FQ_NAME)

    @JvmField
    val atomicByPrimitive = mapOf(
        StandardClassIds.Boolean to ATOMIC_BOOLEAN_CLASS_ID,
        StandardClassIds.Int to ATOMIC_INTEGER_CLASS_ID,
        StandardClassIds.Long to ATOMIC_LONG_CLASS_ID,
    )

    @JvmField
    val ATOMIC_REFERENCE_ARRAY_FQ_NAME = FqName("java.util.concurrent.atomic.AtomicReferenceArray")

    @JvmField
    val ATOMIC_REFERENCE_ARRAY_CLASS_ID = ClassId.topLevel(ATOMIC_REFERENCE_ARRAY_FQ_NAME)

    @JvmField
    val ATOMIC_INTEGER_ARRAY_FQ_NAME = FqName("java.util.concurrent.atomic.AtomicIntegerArray")

    @JvmField
    val ATOMIC_INTEGER_ARRAY_CLASS_ID = ClassId.topLevel(ATOMIC_INTEGER_ARRAY_FQ_NAME)

    @JvmField
    val ATOMIC_LONG_ARRAY_FQ_NAME = FqName("java.util.concurrent.atomic.AtomicLongArray")

    @JvmField
    val ATOMIC_LONG_ARRAY_CLASS_ID = ClassId.topLevel(ATOMIC_LONG_ARRAY_FQ_NAME)

    @JvmField
    val atomicArrayByPrimitive = mapOf(
        StandardClassIds.Int to ATOMIC_INTEGER_ARRAY_CLASS_ID,
        StandardClassIds.Long to ATOMIC_LONG_ARRAY_CLASS_ID,
    )

    const val MULTIFILE_PART_NAME_DELIMITER = "__"

    object Annotations {
        val JvmStatic = ClassId.topLevel(JVM_STATIC_FQ_NAME)
        val JvmName = "JvmName".jvmId()
        val JvmField = "JvmField".jvmId()
        val JvmDefault = "JvmDefault".jvmId()
        val JvmRepeatable = "JvmRepeatable".jvmId()
        val JvmRecord = "JvmRecord".jvmId()
        val JvmSuppressWildcards = "JvmSuppressWildcards".jvmId()
        val JvmWildcard = "JvmWildcard".jvmId()
        val JvmVolatile = "Volatile".jvmId()
        val Throws = "Throws".jvmId()
        val ThrowsAlias = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("Throws"))

        object Java {
            val Deprecated = "Deprecated".javaLangId()
            val Repeatable = "Repeatable".javaAnnotationId()
            val Retention = "Retention".javaAnnotationId()
            val Documented = "Documented".javaAnnotationId()
            val Target = "Target".javaAnnotationId()
            val ElementType = "ElementType".javaAnnotationId()
            val RetentionPolicy = "RetentionPolicy".javaAnnotationId()
        }
    }

    object Java {
        val Record = "Record".javaLangId()
    }

    object Callables {
        val JavaClass = CallableId(BASE_JVM_PACKAGE, Name.identifier("javaClass"))

        val atomicReferenceCompareAndSet = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("compareAndSet"))
        val atomicReferenceWeakCompareAndSet = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("weakCompareAndSet"))
        val atomicReferenceWeakCompareAndSetAcquire = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("weakCompareAndSetAcquire"))
        val atomicReferenceWeakCompareAndSetRelease = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("weakCompareAndSetRelease"))
        val atomicReferenceWeakCompareAndSetPlain = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("weakCompareAndSetPlain"))
        val atomicReferenceWeakCompareAndSetVolatile = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("weakCompareAndSetVolatile"))
        val atomicReferenceCompareAndExchange = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("compareAndExchange"))
        val atomicReferenceCompareAndExchangeAcquire = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("compareAndExchangeAcquire"))
        val atomicReferenceCompareAndExchangeRelease = CallableId(ATOMIC_REFERENCE_CLASS_ID, Name.identifier("compareAndExchangeRelease"))

        val atomicReferenceArrayCompareAndSet = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("compareAndSet"))
        val atomicReferenceArrayWeakCompareAndSet = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("weakCompareAndSet"))
        val atomicReferenceArrayWeakCompareAndSetAcquire = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("weakCompareAndSetAcquire"))
        val atomicReferenceArrayWeakCompareAndSetRelease = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("weakCompareAndSetRelease"))
        val atomicReferenceArrayWeakCompareAndSetPlain = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("weakCompareAndSetPlain"))
        val atomicReferenceArrayWeakCompareAndSetVolatile = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("weakCompareAndSetVolatile"))
        val atomicReferenceArrayCompareAndExchange = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("compareAndExchange"))
        val atomicReferenceArrayCompareAndExchangeAcquire = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("compareAndExchangeAcquire"))
        val atomicReferenceArrayCompareAndExchangeRelease = CallableId(ATOMIC_REFERENCE_ARRAY_CLASS_ID, Name.identifier("compareAndExchangeRelease"))
    }
}

private fun String.jvmId() = ClassId(JvmStandardClassIds.BASE_JVM_PACKAGE, Name.identifier(this))

private val JAVA_LANG_PACKAGE = FqName("java.lang")
private val JAVA_LANG_ANNOTATION_PACKAGE = JAVA_LANG_PACKAGE.child(Name.identifier("annotation"))

private fun String.javaLangId() = ClassId(JAVA_LANG_PACKAGE, Name.identifier(this))
private fun String.javaAnnotationId() = ClassId(JAVA_LANG_ANNOTATION_PACKAGE, Name.identifier(this))
