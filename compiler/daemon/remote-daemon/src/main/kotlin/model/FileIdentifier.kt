/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.FileIdentifierGrpc

data class FileIdentifier(
    val filePath: String,
    val fileFingerprint: String,
)

fun FileIdentifier.toGrpc(): FileIdentifierGrpc{
    return FileIdentifierGrpc
        .newBuilder()
        .setFilePath(filePath)
        .setFileFingerprint(fileFingerprint)
        .build()
}

fun FileIdentifierGrpc.toDomain(): FileIdentifier {
    return FileIdentifier(filePath, fileFingerprint)
}