/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

object JvmNames {
    val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")
    val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    val JVM_MULTIFILE_CLASS: FqName = FqName("kotlin.jvm.JvmMultifileClass")
    val JVM_MULTIFILE_CLASS_SHORT = JVM_MULTIFILE_CLASS.shortName().asString()

    val JVM_PACKAGE_NAME: FqName = FqName("kotlin.jvm.JvmPackageName")
    val JVM_PACKAGE_NAME_SHORT = JVM_PACKAGE_NAME.shortName().asString()

    val JVM_DEFAULT_FQ_NAME = FqName("kotlin.jvm.JvmDefault")
    val JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME = FqName("kotlin.jvm.JvmDefaultWithoutCompatibility")
    val JVM_OVERLOADS_FQ_NAME = FqName("kotlin.jvm.JvmOverloads")

    @JvmField
    val JVM_SYNTHETIC_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmSynthetic")
    val JVM_RECORD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmRecord")

    @JvmField
    val SYNCHRONIZED_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Synchronized")

    @JvmField
    val STRICTFP_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Strictfp")

    @JvmField
    val VOLATILE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Volatile")

    @JvmField
    val TRANSIENT_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Transient")

    const val MULTIFILE_PART_NAME_DELIMITER = "__"
}