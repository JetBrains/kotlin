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
    val JVM_MULTIFILE_CLASS_ID: ClassId = ClassId(FqName("kotlin.jvm"), Name.identifier("JvmMultifileClass"))
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

    const val MULTIFILE_PART_NAME_DELIMITER = "__"

    object Annotations {
        val JvmStatic = "JvmStatic".jvmId()
        val JvmName = "JvmName".jvmId()
        val JvmField = "JvmField".jvmId()
        val JvmDefault = "JvmDefault".jvmId()
        val JvmRepeatable = "JvmRepeatable".jvmId()
        val JvmRecord = "JvmRecord".jvmId()
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
}

private fun String.jvmId() = ClassId(JvmStandardClassIds.BASE_JVM_PACKAGE, Name.identifier(this))

private val JAVA_LANG_PACKAGE = FqName("java.lang")
private val JAVA_LANG_ANNOTATION_PACKAGE = JAVA_LANG_PACKAGE.child(Name.identifier("annotation"))

private fun String.javaLangId() = ClassId(JAVA_LANG_PACKAGE, Name.identifier(this))
private fun String.javaAnnotationId() = ClassId(JAVA_LANG_ANNOTATION_PACKAGE, Name.identifier(this))
