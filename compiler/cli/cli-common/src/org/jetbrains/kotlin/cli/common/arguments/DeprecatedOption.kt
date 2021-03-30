/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

@Retention(AnnotationRetention.RUNTIME)
annotation class DeprecatedOption(
    val message: String = "This option has no effect and will be removed in a future release.",
    val removeAfter: String,
    val level: DeprecationLevel
)