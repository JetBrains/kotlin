/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibConstants.KLIB_RESOURCES_FOLDER_NAME
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * This is the workaround to create an empty 'resources' directory that is expected inside a KLIB.
 */
internal object KlibResourcesComponentWriterImpl : KlibComponentWriter {
    override fun writeTo(root: KlibFile) {
        KlibResourcesComponentLayout(root).resourcesDir.mkdirs()
    }
}

private class KlibResourcesComponentLayout(root: KlibFile) : KlibComponentLayout(root) {
    val resourcesDir: KlibFile
        get() = root.child(KLIB_DEFAULT_COMPONENT_NAME).child(KLIB_RESOURCES_FOLDER_NAME)
}
