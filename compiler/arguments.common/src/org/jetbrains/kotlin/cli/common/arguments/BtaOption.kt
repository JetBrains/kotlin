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

val StringTypeHint.converter: BtaConverter<*, *>?
    get() = when (this) {
        StringTypeHint.NONE -> null
        StringTypeHint.FILE -> PathStringConverter()
        StringTypeHint.DIRECTORY -> PathStringConverter()
        StringTypeHint.FILE_OR_DIRECTORY -> PathStringConverter()
        StringTypeHint.FILE_LIST -> null
        StringTypeHint.DIRECTORY_LIST -> null
        StringTypeHint.FILE_OR_DIRECTORY_LIST -> null
    }


interface BtaConverter<S, T> {
    fun convert(value: S): T
}

class NoopConverter : BtaConverter<String, String> {
    override fun convert(value: String): String = value
}

class PathStringConverter : BtaConverter<Path, String> {
    override fun convert(value: Path): String {
        return value.toString()
    }
}