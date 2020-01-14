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
    override val libDir: File,
    override val component: String = KLIB_DEFAULT_COMPONENT_NAME
) : KotlinLibraryLayout, MetadataKotlinLibraryLayout, IrKotlinLibraryLayout

open class BaseWriterImpl(
    val libraryLayout: KotlinLibraryLayoutForWriter,
    moduleName: String,
    override val versions: KotlinLibraryVersioning,
    val nopack: Boolean = false
) : BaseWriter {

    val klibFile = File("${libraryLayout.libDir.path}.$KLIB_FILE_EXTENSION")
    val manifestProperties = Properties()

    init {
        // TODO: figure out the proper policy here.
        libraryLayout.libDir.deleteRecursively()
        klibFile.delete()
        libraryLayout.libDir.mkdirs()
        libraryLayout.resourcesDir.mkdirs()
        // TODO: <name>:<hash> will go somewhere around here.
        manifestProperties.setProperty(KLIB_PROPERTY_UNIQUE_NAME, moduleName)
        manifestProperties.writeKonanLibraryVersioning(versions)
    }

    override fun addLinkDependencies(libraries: List<KotlinLibrary>) {
        if (libraries.isEmpty()) {
            manifestProperties.remove(KLIB_PROPERTY_DEPENDS)
            // make sure there are no leftovers from the .def file.
            return
        } else {
            val newValue = libraries.joinToString(" ") { it.uniqueName }
            manifestProperties.setProperty(KLIB_PROPERTY_DEPENDS, newValue)
            libraries.forEach { it ->
                if (it.versions.libraryVersion != null) {
                    manifestProperties.setProperty(
                        "${KLIB_PROPERTY_DEPENDENCY_VERSION}_${it.uniqueName}",
                        it.versions.libraryVersion
                    )
                }
            }
        }
    }

    override fun addManifestAddend(properties: Properties) {
        manifestProperties.putAll(properties)
    }

    override fun commit() {
        manifestProperties.saveToFile(libraryLayout.manifestFile)
        if (!nopack) {
            libraryLayout.libDir.zipDirAs(klibFile)
            libraryLayout.libDir.deleteRecursively()
        }
    }
}

/**
 * Requires non-null [target].
 */
class KoltinLibraryWriterImpl(
    libDir: File,
    moduleName: String,
    versions: KotlinLibraryVersioning,
    nopack: Boolean = false,

    val layout: KotlinLibraryLayoutForWriter = KotlinLibraryLayoutForWriter(libDir),

    val base: BaseWriter = BaseWriterImpl(layout, moduleName, versions, nopack),
    metadata: MetadataWriter = MetadataWriterImpl(layout),
    ir: IrWriter = IrMonoliticWriterImpl(layout)
//    ir: IrWriter = IrPerFileWriterImpl(layout)

) : BaseWriter by base, MetadataWriter by metadata, IrWriter by ir, KotlinLibraryWriter

fun buildKoltinLibrary(
    linkDependencies: List<KotlinLibrary>,
    metadata: SerializedMetadata,
    ir: SerializedIrModule?,
    versions: KotlinLibraryVersioning,
    output: String,
    moduleName: String,
    nopack: Boolean,
    manifestProperties: Properties?,
    dataFlowGraph: ByteArray?
): KotlinLibraryLayout {

    val library = KoltinLibraryWriterImpl(File(output), moduleName, versions, nopack)

    library.addMetadata(metadata)

    if (ir != null) {
        library.addIr(ir)
    }

    manifestProperties?.let { library.addManifestAddend(it) }
    library.addLinkDependencies(linkDependencies)
    dataFlowGraph?.let { library.addDataFlowGraph(it) }

    library.commit()
    return library.layout
}