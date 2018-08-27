/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.library.impl

import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout

internal class MetadataWriterImpl(libraryLayout: KonanLibraryLayout): KonanLibraryLayout by libraryLayout {

    fun addLinkData(linkData: LinkData) {
        moduleHeaderFile.writeBytes(linkData.module)
        linkData.fragments.forEachIndexed { index, it ->
            val name = linkData.fragmentNames[index] 
            packageFile(name).writeBytes(it)
        }
    }
}
