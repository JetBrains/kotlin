/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION_WITH_DOT

/**
 * The default implementation of [KlibMetadataComponent].
 *
 * TODO (KT-81411): This class is an implementation detail. It should be made internal after dropping `KonanLibraryImpl`.
 */
class KlibMetadataComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibMetadataComponentLayout>,
) : KlibMetadataComponent {

    override val moduleHeaderData get() = layoutReader.readInPlace { it.moduleHeaderFile.readBytes() }

    override fun getPackageFragmentNames(packageFqName: String) = layoutReader.readInPlace { layout ->
        val fileList: List<String> = layout.getPackageFragmentsDir(packageFqName).listFiles.mapNotNull { file ->
            file.name
                .substringBeforeLast(KLIB_METADATA_FILE_EXTENSION_WITH_DOT, missingDelimiterValue = "")
                .takeIf { it.isNotEmpty() }
        }

        fileList.toSortedSet().also { fileSet ->
            check(fileSet.size == fileList.size) {
                "Duplicated names: ${fileList.groupingBy { it }.eachCount().filter { (_, count) -> count > 1 }}"
            }
        }
    }

    override fun getPackageFragment(packageFqName: String, fragmentName: String) = layoutReader.readInPlace {
        it.getPackageFragmentFile(packageFqName, fragmentName).readBytes()
    }
}
