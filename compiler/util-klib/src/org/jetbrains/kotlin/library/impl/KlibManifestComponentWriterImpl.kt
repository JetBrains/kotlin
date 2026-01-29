/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.KLIB_PROPERTY_COMPILER_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_SIGNATURE_VERSIONS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.library.KLIB_PROPERTY_WASM_TARGETS
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.writeKonanLibraryVersioning
import org.jetbrains.kotlin.library.writer.KlibManifestWriterSpec
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import java.util.Properties
import kotlin.collections.plusAssign
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * An implementation of [KlibComponentWriter] that writes manifest properties to the constructed Klib library.
 */
internal class KlibManifestComponentWriterImpl(
    val moduleName: String,
    val versions: KotlinLibraryVersioning,
    val builtInsPlatform: BuiltInsPlatform,
    val targetNames: List<String>,
    val customProperties: Properties,
) : KlibComponentWriter {
    override fun writeTo(root: KlibFile) {
        val properties = assembleProperties()

        val layout = KlibManifestComponentLayout(root)
        layout.manifestFile.parentFile.mkdirs()
        properties.saveToFile(layout.manifestFile)
    }

    private fun assembleProperties(): Properties = Properties().apply {
        this[KLIB_PROPERTY_UNIQUE_NAME] = moduleName
        writeKonanLibraryVersioning(versions)

        when (builtInsPlatform) {
            BuiltInsPlatform.COMMON -> Unit
            else -> {
                this[KLIB_PROPERTY_BUILTINS_PLATFORM] = builtInsPlatform.name
                getPropertyNameForListOfTargetNames(builtInsPlatform)?.let { this[it] = targetNames.sorted().joinToString(" ") }
            }
        }

        this += customProperties
    }

    companion object {
        /**
         * The set of property names that cannot be specified through [KlibManifestWriterSpec.customProperties].
         */
        internal val NON_CUSTOMIZED_PROPERTY_NAMES = setOf(
            // moduleName
            KLIB_PROPERTY_UNIQUE_NAME,

            // versions
            KLIB_PROPERTY_ABI_VERSION,
            KLIB_PROPERTY_METADATA_VERSION,
            KLIB_PROPERTY_COMPILER_VERSION,
            KLIB_PROPERTY_IR_SIGNATURE_VERSIONS,

            // builtInsPlatform
            KLIB_PROPERTY_BUILTINS_PLATFORM,

            // targetNames
            KLIB_PROPERTY_NATIVE_TARGETS,
            KLIB_PROPERTY_WASM_TARGETS,
        )

        /**
         * Get the name of the manifest property that contains the list of target names for the given platform.
         */
        internal fun getPropertyNameForListOfTargetNames(builtInsPlatform: BuiltInsPlatform): String? = when (builtInsPlatform) {
            BuiltInsPlatform.NATIVE -> KLIB_PROPERTY_NATIVE_TARGETS
            BuiltInsPlatform.WASM -> KLIB_PROPERTY_WASM_TARGETS
            else -> null // others don't have target names in manifest
        }
    }
}