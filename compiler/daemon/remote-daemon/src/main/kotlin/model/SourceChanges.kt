/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.server.KnownProto
import org.jetbrains.kotlin.server.SourcesChangesProto
import org.jetbrains.kotlin.server.ToBeCalculatedProto
import org.jetbrains.kotlin.server.UnknownProto
import java.io.File

fun SourcesChanges.toProto(): SourcesChangesProto {
    return SourcesChangesProto.newBuilder()
        .apply {
            when (this@toProto) {
                is SourcesChanges.Unknown -> setUnknown(UnknownProto.getDefaultInstance())
                is SourcesChanges.ToBeCalculated -> setToBeCalculated(ToBeCalculatedProto.getDefaultInstance())
                is SourcesChanges.Known -> setKnown(
                    KnownProto.newBuilder()
                        .addAllModifiedFiles(this@toProto.modifiedFiles.map { it.absolutePath })
                        .addAllRemovedFiles(this@toProto.removedFiles.map { it.absolutePath })
                        .build()
                )
            }
        }
        .build()
}

fun SourcesChangesProto.toDomain(): SourcesChanges {
    return when (kindCase) {
        SourcesChangesProto.KindCase.UNKNOWN -> SourcesChanges.Unknown
        SourcesChangesProto.KindCase.TO_BE_CALCULATED -> SourcesChanges.ToBeCalculated
        SourcesChangesProto.KindCase.KNOWN -> SourcesChanges.Known(
            modifiedFiles = known.modifiedFilesList.map { File(it) },
            removedFiles = known.removedFilesList.map { File(it) }
        )
        SourcesChangesProto.KindCase.KIND_NOT_SET -> SourcesChanges.Unknown
        null -> SourcesChanges.Unknown
    }
}