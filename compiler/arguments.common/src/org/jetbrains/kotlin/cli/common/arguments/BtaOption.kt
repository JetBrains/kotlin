/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments;

import java.nio.file.Path
import kotlin.annotation.AnnotationRetention;
import kotlin.annotation.Retention;

@Retention(AnnotationRetention.RUNTIME)
annotation class BtaOption(
    val stringTypeHint: StringTypeHint,
)

enum class StringTypeHint {
    NONE,
    FILE,
    DIRECTORY,
    FILE_OR_DIRECTORY,
    FILE_LIST,
    DIRECTORY_LIST,
    FILE_OR_DIRECTORY_LIST;
}
