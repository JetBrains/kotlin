/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.library.*

const val KLIB_DEFAULT_COMPONENT_NAME = "default"

open class KotlinLibraryLayoutForWriter(
    override val libFile: File,
    val unzippedDir: File,
    override val component: String = KLIB_DEFAULT_COMPONENT_NAME
) : KotlinLibraryLayout {
    override val componentDir: File
        get() = File(unzippedDir, component)
    override val pre_1_4_manifest: File
        get() = File(unzippedDir, KLIB_MANIFEST_FILE_NAME)
}

class BaseWriterImpl(
    val libraryLayout: KotlinLibraryLayoutForWriter,
    moduleName: String,
    _versions: KotlinLibraryVersioning,
    builtInsPlatform: BuiltInsPlatform,
    nativeTargets: List<String> = emptyList(),
    val nopack: Boolean = false,
    val shortName: String? = null
) : BaseWriter {

    val klibFile = libraryLayout.libFile.canonicalFile
    val manifestProperties = Properties()
    override val versions: KotlinLibraryVersioning = _versions

    init {
        // TODO: figure out the proper policy here.
        klibFile.deleteRecursively()
        klibFile.parentFile.run { if (!exists) mkdirs() }
        libraryLayout.resourcesDir.mkdirs()
        initManifestProperties(moduleName, versions, builtInsPlatform, nativeTargets, shortName)
    }

    private fun initManifestProperties(
        moduleName: String,
        _versions: KotlinLibraryVersioning,
        builtInsPlatform: BuiltInsPlatform,
        nativeTargets: List<String>,
        shortName: String?
    ) {
        manifestProperties.setProperty(KLIB_PROPERTY_UNIQUE_NAME, moduleName)

        manifestProperties.writeKonanLibraryVersioning(_versions)

        if (builtInsPlatform != BuiltInsPlatform.COMMON) {
            manifestProperties.setProperty(KLIB_PROPERTY_BUILTINS_PLATFORM, builtInsPlatform.name)
        }

        if (builtInsPlatform == BuiltInsPlatform.NATIVE) {
            manifestProperties.setProperty(KLIB_PROPERTY_NATIVE_TARGETS, nativeTargets.joinToString(" "))
        }

        shortName?.let { manifestProperties.setProperty(KLIB_PROPERTY_SHORT_NAME, it) }
    }

    override fun addLinkDependencies(libraries: List<KotlinLibrary>) {
        if (libraries.isEmpty()) {
            manifestProperties.remove(KLIB_PROPERTY_DEPENDS)
            // make sure there are no leftovers from the .def file.
            return
        } else {
            val newValue = libraries.map { it.uniqueName }.toSpaceSeparatedString()
            manifestProperties.setProperty(KLIB_PROPERTY_DEPENDS, newValue)
        }
    }

    override fun addManifestAddend(properties: Properties) {
        manifestProperties.putAll(properties)
    }

    override fun commit() {
        manifestProperties.saveToFile(libraryLayout.manifestFile)
        if (!nopack) {
            libraryLayout.unzippedDir.zipDirAs(klibFile)
            libraryLayout.unzippedDir.deleteRecursively()
        }
    }
}

enum class BuiltInsPlatform {
    JVM, JS, NATIVE, WASM, COMMON;

    companion object {
        fun parseFromString(name: String): BuiltInsPlatform? = values().firstOrNull { it.name == name }
    }
}

fun List<String>.toSpaceSeparatedString(): String = joinToString(separator = " ") {
    if (it.contains(" ")) "\"$it\"" else it
}
