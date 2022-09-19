/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

object JvmNames {
    @JvmField
    val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")

    @JvmField
    val JVM_NAME_CLASS_ID = ClassId.topLevel(JVM_NAME)

    val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    @JvmField
    val JVM_EXPOSE_BOXED: FqName = FqName("kotlin.jvm.JvmExposeBoxed")

    @JvmField
    val JVM_EXPOSE_BOXED_CLASS_ID = ClassId.topLevel(JVM_EXPOSE_BOXED)

    val JVM_EXPOSE_BOXED_SHORT: String = JVM_EXPOSE_BOXED.shortName().asString()

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
}
