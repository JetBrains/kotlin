/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.components

import org.jetbrains.kotlin.konan.file.File as KlibFile
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibMandatoryComponent
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_MODULE_METADATA_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf

/**
 * This component provides read access to Klib metadata.
 */
interface KlibMetadataComponent : KlibMandatoryComponent {
    /** The metadata header in the raw form (bytes, yet to be deserialized to [KlibMetadataProtoBuf.Header]). */
    val moduleHeaderData: ByteArray

    /** Names of package fragments for the fully qualified package name [packageFqName]. */
    fun getPackageFragmentNames(packageFqName: String): Set<String>

    /** The concrete package fragment in the raw form (bytes, yet to be deserialized to [ProtoBuf.PackageFragment]). */
    fun getPackageFragment(packageFqName: String, fragmentName: String): ByteArray

    companion object Kind : KlibMandatoryComponent.Kind<KlibMetadataComponent>
}

/**
 * A shortcut for accessing the [KlibMetadataComponent] in the [Klib] instance.
 *
 * It is expected that every correct Klib has metadata files. So, the [metadata] property always returns
 * a non-null component instance that can be used to read the Klib's metadata.
 */
inline val Klib.metadata: KlibMetadataComponent
    get() = getComponent(KlibMetadataComponent.Kind)

class KlibMetadataComponentLayout(root: KlibFile) : KlibComponentLayout(root) {
    constructor(root: String) : this(KlibFile(root))

    /** The metadata directory. */
    val metadataDir: KlibFile
        get() = root.child(KLIB_DEFAULT_COMPONENT_NAME).child(KLIB_METADATA_FOLDER_NAME)

    /** The metadata header file. */
    val moduleHeaderFile: KlibFile
        get() = metadataDir.child(KLIB_MODULE_METADATA_FILE_NAME)

    /** The directory where package fragments with the fully qualified package name [packageFqName] are located. */
    fun getPackageFragmentsDir(packageFqName: String): KlibFile =
        metadataDir.child(if (packageFqName == "") KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME else "$KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX$packageFqName")

    /** The concrete package fragment file with the name [partName] for the fully qualified package name [packageFqName]. */
    fun getPackageFragmentFile(packageFqName: String, partName: String): KlibFile =
        getPackageFragmentsDir(packageFqName).child("$partName.$KLIB_METADATA_FILE_EXTENSION")
}

object KlibMetadataConstants {
    const val KLIB_METADATA_FOLDER_NAME = "linkdata"
    const val KLIB_MODULE_METADATA_FILE_NAME = "module"
    const val KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME = "root_package"
    const val KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX = "package_"
    const val KLIB_METADATA_FILE_EXTENSION = "knm"
    const val KLIB_METADATA_FILE_EXTENSION_WITH_DOT = ".$KLIB_METADATA_FILE_EXTENSION"
}

