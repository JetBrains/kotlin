/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

class KotlinNativeMetaFileTypeFactory : FileTypeFactory() {

    override fun createFileTypes(consumer: FileTypeConsumer) = consumer.consume(KotlinNativeMetaFileType, KotlinNativeMetaFileType.defaultExtension)
}
