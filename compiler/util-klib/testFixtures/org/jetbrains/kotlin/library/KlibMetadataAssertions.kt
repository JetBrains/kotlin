/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes

fun assertNoEmptyPackageFragmentsInKlib(klibPath: Path, expectedPackageFqNames: Set<String>) {
    if (klibPath.isDirectory()) {
        assertNoEmptyPackageFragmentsInKlibRoot(klibPath, expectedPackageFqNames)
    } else {
        FileSystems.newFileSystem(klibPath, null as ClassLoader?).use { zipFileSystem ->
            assertNoEmptyPackageFragmentsInKlibRoot(zipFileSystem.rootDirectories.single(), expectedPackageFqNames)
        }
    }
}

private fun assertNoEmptyPackageFragmentsInKlibRoot(klibRoot: Path, expectedPackageFqNames: Set<String>) {
    val layout = KlibMetadataComponentLayout(klibRoot)

    val header = KlibMetadataProtoBuf.Header.parseFrom(layout.moduleHeaderFile.readBytes())
    assertTrue(header.emptyPackageList.isEmpty(), "The metadata header lists empty packages: ${header.emptyPackageList}")

    val packageFqNamesInHeader: List<String> = header.packageFragmentNameList
    assertEquals(expectedPackageFqNames.sorted(), packageFqNamesInHeader.sorted(), "Unexpected package FQ names in the metadata header")

    val packageFragmentDirs = layout.metadataDir.listDirectoryEntries().filter { it.isDirectory() }
    val packageFqNamesOnDisk = packageFragmentDirs.map { dir ->
        if (dir.name == KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME) "" else dir.name.removePrefix(KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX)
    }
    assertEquals(
        packageFqNamesInHeader.sorted(),
        packageFqNamesOnDisk.sorted(),
        "Package fragment directories do not match the package FQ names in the metadata header"
    )

    for (packageFragmentDir in packageFragmentDirs) {
        val packageFragmentFiles = packageFragmentDir.listDirectoryEntries()
            .filter { it.name.endsWith(KLIB_METADATA_FILE_EXTENSION_WITH_DOT) }
        assertTrue(packageFragmentFiles.isNotEmpty(), "No package fragments in $packageFragmentDir")

        for (packageFragmentFile in packageFragmentFiles) {
            val packageFragment = ProtoBuf.PackageFragment.parseFrom(packageFragmentFile.readBytes())
            assertFalse(packageFragment.hasNoDeclarations(), "Empty package fragment: $packageFragmentFile")
        }
    }
}

private fun ProtoBuf.PackageFragment.hasNoDeclarations(): Boolean =
    class_List.isEmpty() && (!hasPackage() || `package`.let { it.functionList.isEmpty() && it.propertyList.isEmpty() && it.typeAliasList.isEmpty() })
