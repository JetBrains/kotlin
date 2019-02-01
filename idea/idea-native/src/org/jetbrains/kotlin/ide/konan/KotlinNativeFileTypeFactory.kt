/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.konan.library.KDEFINITIONS_FILE_EXTENSION

class KotlinNativeFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(ArchiveFileType.INSTANCE, KLIB_FILE_EXTENSION)
        consumer.consume(NativeDefinitionsFileType, KDEFINITIONS_FILE_EXTENSION)
    }
}