/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.annotations

import org.jetbrains.kotlin.name.FqName

// This annotation is declared here in frontend (as opposed to frontend.java) because it's used in AllUnderImportScope
// If you wish to add another JVM-related annotation and has/find utility methods, please proceed to jvmAnnotationUtil.kt
@JvmField
val JVM_THROWS_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Throws")

val KOTLIN_THROWS_ANNOTATION_FQ_NAME = FqName("kotlin.Throws")

val KOTLIN_NATIVE_THROWS_ANNOTATION_FQ_NAME = FqName("kotlin.native.Throws")
