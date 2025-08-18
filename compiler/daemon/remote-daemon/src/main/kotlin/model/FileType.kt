/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.FileTypeGrpc

enum class FileType {
    SOURCE,
    DEPENDENCY,
    COMPILER_PLUGIN,
    RESULT,
}

fun FileType.toGrpc(): FileTypeGrpc {
    return when (this) {
        FileType.SOURCE -> FileTypeGrpc.SOURCE
        FileType.DEPENDENCY -> FileTypeGrpc.DEPENDENCY
        FileType.COMPILER_PLUGIN -> FileTypeGrpc.COMPILER_PLUGIN
        FileType.RESULT -> FileTypeGrpc.RESULT
    }
}

fun FileTypeGrpc.toDomain(): FileType {
    return when (this) {
        FileTypeGrpc.SOURCE -> FileType.SOURCE
        FileTypeGrpc.DEPENDENCY -> FileType.DEPENDENCY
        FileTypeGrpc.COMPILER_PLUGIN -> FileType.COMPILER_PLUGIN
        FileTypeGrpc.RESULT -> FileType.RESULT
        FileTypeGrpc.UNRECOGNIZED -> FileType.SOURCE // TODO double check
    }
}