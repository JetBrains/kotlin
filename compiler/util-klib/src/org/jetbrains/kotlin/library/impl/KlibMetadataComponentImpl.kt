/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readBytes

/**
 * The default implementation of [KlibMetadataComponent].
 */
internal class KlibMetadataComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibMetadataComponentLayout>,
) : KlibMetadataComponent {

    override val moduleHeaderData get() = layoutReader.readInPlace { it.moduleHeaderFile.readBytes() }

    override fun getPackageFragmentNames(packageFqName: String) = layoutReader.readInPlace { layout ->
        val fileList: List<String> = layout.getPackageFragmentsDir(packageFqName).listDirectoryEntries().mapNotNull { file ->
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

    override fun getPackageNames(): Set<String> {
        return layoutReader.readInPlace { layout ->
            layout.metadataDir.listDirectoryEntries()
                .mapNotNullTo(mutableSetOf()) {
                    when {
                        it.name.startsWith(KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME) -> ""
                        it.name.startsWith(KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX) -> {
                            Path(it.pathString).name.removePrefix(KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX)
                        }
                        else -> null
                    }
                }
        }
    }
}
