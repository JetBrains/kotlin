/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.components

import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibConstants.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_MODULE_METADATA_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME
import org.jetbrains.kotlin.library.impl.KlibMetadataComponentImpl
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import java.nio.file.Path

/**
 * This component provides read access to Klib metadata.
 */
interface KlibMetadataComponent : KlibComponent {
    /** The metadata header in the raw form (bytes, yet to be deserialized to [KlibMetadataProtoBuf.Header]). */
    val moduleHeaderData: ByteArray

    /** Names of package fragments for the fully qualified package name [packageFqName]. */
    fun getPackageFragmentNames(packageFqName: String): Set<String>

    /** The concrete package fragment in the raw form (bytes, yet to be deserialized to [ProtoBuf.PackageFragment]). */
    fun getPackageFragment(packageFqName: String, fragmentName: String): ByteArray

    companion object Kind : KlibComponent.Kind<KlibMetadataComponent, KlibMetadataComponentLayout> {
        override fun createLayout(root: Path) = KlibMetadataComponentLayout(root)

        /**
         * Note: It is expected that every correct Klib has metadata files.
         * Therefore, no data availability check is performed in this method and the component is always created unconditionally.
         */
        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KlibMetadataComponentLayout>): KlibMetadataComponent =
            KlibMetadataComponentImpl(layoutReader)
    }
}

/**
 * A shortcut for accessing the [KlibMetadataComponent] in the [Klib] instance.
 *
 * It is expected that every correct Klib has metadata files. So, the [metadata] property always returns
 * a non-null component instance that can be used to read the Klib's metadata.
 */
inline val Klib.metadata: KlibMetadataComponent
    get() = getComponent(KlibMetadataComponent.Kind)!!

class KlibMetadataComponentLayout(root: Path) : KlibComponentLayout(root) {
    /** The metadata directory. */
    val metadataDir: Path
        get() = root.resolve(KLIB_DEFAULT_COMPONENT_NAME).resolve(KLIB_METADATA_FOLDER_NAME)

    /** The metadata header file. */
    val moduleHeaderFile: Path
        get() = metadataDir.resolve(KLIB_MODULE_METADATA_FILE_NAME)

    /** The directory where package fragments with the fully qualified package name [packageFqName] are located. */
    fun getPackageFragmentsDir(packageFqName: String): Path =
        metadataDir.resolve(if (packageFqName == "") KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME else "$KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX$packageFqName")

    /** The concrete package fragment file with the name [partName] for the fully qualified package name [packageFqName]. */
    fun getPackageFragmentFile(packageFqName: String, partName: String): Path =
        getPackageFragmentsDir(packageFqName).resolve("$partName.$KLIB_METADATA_FILE_EXTENSION")
}

object KlibMetadataConstants {
    const val KLIB_METADATA_FOLDER_NAME = "linkdata"
    const val KLIB_MODULE_METADATA_FILE_NAME = "module"
    const val KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME = "root_package"
    const val KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX = "package_"
    const val KLIB_METADATA_FILE_EXTENSION = "knm"
    const val KLIB_METADATA_FILE_EXTENSION_WITH_DOT = ".$KLIB_METADATA_FILE_EXTENSION"
}

