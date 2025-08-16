/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

/**
 * True if the system property `kotlin.reflect.jvm.useK1Implementation` is set to true.
 *
 * This system property can be used to change kotlin-reflect implementation to the legacy one, based on parts of the K1 compiler,
 * in case of any problems with the new implementation, based on kotlin-metadata-jvm.
 *
 * Changing the value of the system property after it has been read (i.e., after any non-trivial operation in kotlin-reflect)
 * is not supported and might lead to unexpected or incorrect behavior.
 *
 * See KT-75463 and related issues for more information.
 */
internal val useK1Implementation = runCatching {
    System.getProperty("kotlin.reflect.jvm.useK1Implementation")
}.getOrNull()?.toBoolean() == true
