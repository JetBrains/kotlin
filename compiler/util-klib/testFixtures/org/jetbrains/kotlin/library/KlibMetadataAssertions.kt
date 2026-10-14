/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.io.ZipFileSystemInPlaceAccessor
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION_WITH_DOT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

fun assertPackageFragmentsInKlib(klibPath: Path, expectedPackageFqNames: Set<String>) {
    KlibLayoutReaderFactory(klibPath, ZipFileSystemInPlaceAccessor)
        .createLayoutReader(::KlibMetadataComponentLayout)
        .readInPlace { layout -> assertPackageFragmentsInKlib(layout, expectedPackageFqNames) }
}

private fun assertPackageFragmentsInKlib(layout: KlibMetadataComponentLayout, expectedPackageFqNames: Set<String>) {
    val packageFragmentDirs = layout.metadataDir.listDirectoryEntries().filter { it.isDirectory() }
    assertEquals(
        expectedPackageFqNames.map(layout::getPackageFragmentsDir).sorted(),
        packageFragmentDirs.sorted(),
        "Unexpected package fragment directories"
    )

    for (packageFragmentDir in packageFragmentDirs) {
        val packageFragmentFiles = packageFragmentDir.listDirectoryEntries()
            .filter { it.name.endsWith(KLIB_METADATA_FILE_EXTENSION_WITH_DOT) }
        assertTrue(packageFragmentFiles.isNotEmpty(), "No package fragments in $packageFragmentDir")
    }
}
