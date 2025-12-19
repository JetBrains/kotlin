/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.writer

import org.jetbrains.kotlin.konan.file.createTempDir
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.library.KlibFormat
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KlibManifestComponentWriterImpl
import org.jetbrains.kotlin.library.impl.KlibManifestComponentWriterImpl.Companion.NON_CUSTOMIZED_PROPERTY_NAMES
import org.jetbrains.kotlin.library.impl.KlibManifestComponentWriterImpl.Companion.getPropertyNameForListOfTargetNames
import org.jetbrains.kotlin.library.impl.KlibResourcesComponentWriterImpl
import java.util.Properties
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * The [KlibWriter] component is the unified endpoint for writing [KotlinLibrary] artifacts to the file system.
 *
 * It can be configured using the [KlibWriterSpec] and [KlibManifestWriterSpec] DSLs.
 *
 * The component performs a limited set of basic checks, such as
 * - Checking that all the required manifest properties have been specified: module name, platform, list of targets, versions
 * - Checking validity of the supplied module name
 * - Checking if the list of targets can be written for the specified platform
 */
class KlibWriter(init: KlibWriterSpec.() -> Unit) {
    private var format: KlibFormat = KlibFormat.Directory
    private val componentWriters = mutableListOf<KlibComponentWriter>()

    private var moduleName: String? = null
    private var versions: KotlinLibraryVersioning? = null
    private var builtInsPlatform: BuiltInsPlatform? = null
    private var targetNames = emptyList<String>()
    private val customProperties = Properties()

    init {
        object : KlibWriterSpec, KlibManifestWriterSpec {

            /*** [KlibWriterSpec] ***/

            override fun format(format: KlibFormat) {
                this@KlibWriter.format = format
            }

            override fun include(vararg writers: KlibComponentWriter) {
                this@KlibWriter.componentWriters += writers
            }

            override fun include(writers: Collection<KlibComponentWriter>) {
                this@KlibWriter.componentWriters += writers
            }

            override fun manifest(init: KlibManifestWriterSpec.() -> Unit) = init()

            /*** [KlibManifestWriterSpec] ***/

            override fun moduleName(moduleName: String) {
                this@KlibWriter.moduleName = moduleName
            }

            override fun versions(versions: KotlinLibraryVersioning) {
                this@KlibWriter.versions = versions
            }

            override fun platformAndTargets(
                builtInsPlatform: BuiltInsPlatform,
                targetNames: List<String>,
            ) {
                this@KlibWriter.builtInsPlatform = builtInsPlatform
                this@KlibWriter.targetNames = targetNames.toList()
            }

            override fun customProperties(init: Properties.() -> Unit) {
                customProperties.init()
            }
        }.init()
    }

    fun writeTo(destinationPath: String) {
        val allComponentWriters: List<KlibComponentWriter> = buildList {
            this += componentWriters

            // Note: Calling this function may throw an exception if manifest is misconfigured.
            this += validateManifestPropertiesAndCreateComponentWriter()

            // This is the workaround for the 'resources' directory.
            this += KlibResourcesComponentWriterImpl
        }

        val destination = KlibFile(destinationPath)

        if (destination.exists) {
            destination.deleteRecursively()
        } else {
            destination.parentFile.mkdirs()
        }

        when (format) {
            KlibFormat.Directory -> writeComponents(allComponentWriters, root = destination)

            KlibFormat.ZipArchive -> {
                val temporaryDir = createTempDir("klib")
                writeComponents(allComponentWriters, root = temporaryDir)
                temporaryDir.zipDirAs(destination)
                temporaryDir.deleteRecursively()
            }
        }
    }

    private fun validateManifestPropertiesAndCreateComponentWriter(): KlibManifestComponentWriterImpl {
        val moduleName = checkNotNull(moduleName) {
            "Module name is not specified. Use `KlibWriter { manifest { moduleName(...) } }` to set it."
        }

        check(moduleName.isNotBlank()) { "Module name cannot be empty." }

        val versions = checkNotNull(versions) {
            "Klib versions are not specified. Use `KlibWriter { manifest { versions(...) } }` to set them."
        }

        val builtInsPlatform = checkNotNull(builtInsPlatform) {
            "Klib platform is not specified. Use `KlibWriter { manifest { platformAndTargets(...) } }` to set it along with targets."
        }

        check(targetNames.isEmpty() || getPropertyNameForListOfTargetNames(builtInsPlatform) != null) {
            "Non-empty list of target names is specified for a platform that doesn't support them: $builtInsPlatform"
        }

        val nonCustomizedPropertyNames = customProperties.stringPropertyNames() intersect NON_CUSTOMIZED_PROPERTY_NAMES
        check(nonCustomizedPropertyNames.isEmpty()) {
            "Custom properties $nonCustomizedPropertyNames are not allowed to be added through `KlibWriter { manifest { customProperties(...) } }`." +
                    " Use other functions to set them."
        }

        return KlibManifestComponentWriterImpl(
            moduleName = moduleName,
            versions = versions,
            builtInsPlatform = builtInsPlatform,
            targetNames = targetNames,
            customProperties = customProperties,
        )
    }

    companion object {
        private fun writeComponents(allComponentWriters: List<KlibComponentWriter>, root: KlibFile) {
            for (componentWriter in allComponentWriters) {
                componentWriter.writeTo(root)
            }
        }
    }
}

interface KlibWriterSpec {
    fun format(format: KlibFormat)
    fun include(vararg writers: KlibComponentWriter)
    fun include(writers: Collection<KlibComponentWriter>)
    fun manifest(init: KlibManifestWriterSpec.() -> Unit)
}

interface KlibManifestWriterSpec {
    fun moduleName(moduleName: String)
    fun versions(versions: KotlinLibraryVersioning)
    fun platformAndTargets(builtInsPlatform: BuiltInsPlatform, targetNames: List<String> = emptyList())
    fun customProperties(init: Properties.() -> Unit)
}
