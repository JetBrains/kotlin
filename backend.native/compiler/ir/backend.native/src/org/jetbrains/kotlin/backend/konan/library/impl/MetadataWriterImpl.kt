/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.library.impl

import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout

internal class MetadataWriterImpl(libraryLayout: KonanLibraryLayout): KonanLibraryLayout by libraryLayout {

    fun addLinkData(linkData: LinkData) {
        moduleHeaderFile.writeBytes(linkData.module)
        linkData.ir?.let { irHeader.writeBytes(it.module) }
        linkData.fragments.forEachIndexed { index, it ->
            val packageFqName = linkData.fragmentNames[index]
            val shortName = packageFqName.substringAfterLast(".")
            val dir = packageFragmentsDir(packageFqName)
            dir.deleteRecursively()
            dir.mkdirs()
            val numCount = it.size.toString().length
            fun withLeadingZeros(i: Int) = String.format("%0${numCount}d", i)
            for ((i, fragment) in it.withIndex()) {
                packageFragmentFile(packageFqName, "${withLeadingZeros(i)}_$shortName").writeBytes(fragment)
            }
        }
        linkData.ir?.combinedDeclarationFilePath?.let {
            // TODO: use Files.move.
            File(it).copyTo(irFile)
        }
    }
}
