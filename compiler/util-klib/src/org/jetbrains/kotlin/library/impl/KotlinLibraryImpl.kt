/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties

open class BaseKotlinLibraryImpl(
    val access: BaseLibraryAccess<KotlinLibraryLayout>,
    override val isDefault: Boolean
) : BaseKotlinLibrary {
    override val libraryFile get() = access.klib
    override val libraryName: String by lazy { access.inPlace { it.libraryName } }

    override val componentList: List<String> by lazy {
        access.inPlace {
            it.libDir.listFiles
                .filter { it.isDirectory }
                .filter { it.listFiles.map { it.name }.contains(KLIB_MANIFEST_FILE_NAME) }
                .map { it.name }
        }
    }

    override fun toString() = "$libraryName[default=$isDefault]"

    override val has_pre_1_4_manifest: Boolean by lazy {
        access.inPlace { it.pre_1_4_manifest.exists }
    }

    override val manifestProperties: Properties by lazy {
        access.inPlace { it.manifestFile.loadProperties() }
    }

    override val versions: KotlinLibraryVersioning by lazy {
        manifestProperties.readKonanLibraryVersioning()
    }
}

open class MetadataLibraryImpl(
    val access: MetadataLibraryAccess<MetadataKotlinLibraryLayout>
) : MetadataLibrary {

    override val moduleHeaderData: ByteArray by lazy {
        access.inPlace {
            it.moduleHeaderFile.readBytes()
        }
    }

    override fun packageMetadata(fqName: String, partName: String): ByteArray =
        access.inPlace {
            it.packageFragmentFile(fqName, partName).readBytes()
        }

    override fun packageMetadataParts(fqName: String): Set<String> =
        access.inPlace { inPlaceaccess ->
            val fileList =
                inPlaceaccess.packageFragmentsDir(fqName)
                    .listFiles
                    .mapNotNull {
                        it.name
                            .substringBeforeLast(KLIB_METADATA_FILE_EXTENSION_WITH_DOT, missingDelimiterValue = "")
                            .takeIf { it.isNotEmpty() }
                    }

            fileList.toSortedSet().also {
                require(it.size == fileList.size) { "Duplicated names: ${fileList.groupingBy { it }.eachCount().filter { (_, count) -> count > 1 }}" }
            }
        }
}

abstract class IrLibraryImpl(
    val access: IrLibraryAccess<IrKotlinLibraryLayout>
) : IrLibrary {
    override val dataFlowGraph by lazy {
        access.inPlace { it: IrKotlinLibraryLayout ->
            it.dataFlowGraphFile.let { if (it.exists) it.readBytes() else null }
        }
    }
}

class IrMonoliticLibraryImpl(_access: IrLibraryAccess<IrKotlinLibraryLayout>) : IrLibraryImpl(_access) {
    override fun fileCount(): Int = files.entryCount()

    override fun irDeclaration(index: Long, fileIndex: Int) = loadIrDeclaration(index, fileIndex)

    override fun symbol(index: Int, fileIndex: Int) = symbols.tableItemBytes(fileIndex, index)

    override fun type(index: Int, fileIndex: Int) = types.tableItemBytes(fileIndex, index)

    override fun string(index: Int, fileIndex: Int) = strings.tableItemBytes(fileIndex, index)

    override fun body(index: Int, fileIndex: Int) = bodies.tableItemBytes(fileIndex, index)

    override fun file(index: Int) = files.tableItemBytes(index)

    private fun loadIrDeclaration(index: Long, fileIndex: Int) =
        combinedDeclarations.tableItemBytes(fileIndex, DeclarationId(index))

    private val combinedDeclarations: DeclarationIrMultiTableReader by lazy {
        DeclarationIrMultiTableReader(access.realFiles {
            it.irDeclarations
        })
    }

    private val symbols: IrMultiArrayReader by lazy {
        IrMultiArrayReader(access.realFiles {
            it.irSymbols
        })
    }

    private val types: IrMultiArrayReader by lazy {
        IrMultiArrayReader(access.realFiles {
            it.irTypes
        })
    }

    private val strings: IrMultiArrayReader by lazy {
        IrMultiArrayReader(access.realFiles {
            it.irStrings
        })
    }

    private val bodies: IrMultiArrayReader by lazy {
        IrMultiArrayReader(access.realFiles {
            it.irBodies
        })
    }

    private val files: IrArrayReader by lazy {
        IrArrayReader(access.realFiles {
            it.irFiles
        })
    }
}

class IrPerFileLibraryImpl(_access: IrLibraryAccess<IrKotlinLibraryLayout>) : IrLibraryImpl(_access) {

    private val directories by lazy {
        access.realFiles {
            it.irDir.listFiles.filter { f -> f.isDirectory && f.name.endsWith(".file") }
        }
    }

    private val fileToDeclarationMap = mutableMapOf<Int, DeclarationIrTableReader>()
    override fun irDeclaration(index: Long, fileIndex: Int): ByteArray {
        val dataReader = fileToDeclarationMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            DeclarationIrTableReader(access.realFiles {
                it.irDeclarations(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(DeclarationId(index))
    }

    private val fileToSymbolMap = mutableMapOf<Int, IrArrayReader>()
    override fun symbol(index: Int, fileIndex: Int): ByteArray {
        val dataReader = fileToSymbolMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            IrArrayReader(access.realFiles {
                it.irSymbols(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(index)
    }

    private val fileToTypeMap = mutableMapOf<Int, IrArrayReader>()
    override fun type(index: Int, fileIndex: Int): ByteArray {
        val dataReader = fileToTypeMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            IrArrayReader(access.realFiles {
                it.irTypes(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(index)
    }

    private val fileToStringMap = mutableMapOf<Int, IrArrayReader>()
    override fun string(index: Int, fileIndex: Int): ByteArray {
        val dataReader = fileToStringMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            IrArrayReader(access.realFiles {
                it.irStrings(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(index)
    }

    private val fileToBodyMap = mutableMapOf<Int, IrArrayReader>()
    override fun body(index: Int, fileIndex: Int): ByteArray {
        val dataReader = fileToBodyMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            IrArrayReader(access.realFiles {
                it.irBodies(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(index)
    }

    override fun file(index: Int): ByteArray {
        return access.realFiles {
            it.irFile(directories[index]).readBytes()
        }
    }

    override fun fileCount(): Int {
        return directories.size
    }
}

open class KotlinLibraryImpl(
    val base: BaseKotlinLibraryImpl,
    val metadata: MetadataLibraryImpl,
    val ir: IrLibraryImpl
) : KotlinLibrary,
    BaseKotlinLibrary by base,
    MetadataLibrary by metadata,
    IrLibrary by ir

fun createKotlinLibrary(
    libraryFile: File,
    component: String,
    isDefault: Boolean = false
): KotlinLibrary {
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, component)
    val metadataAccess = MetadataLibraryAccess<MetadataKotlinLibraryLayout>(libraryFile, component)
    val irAccess = IrLibraryAccess<IrKotlinLibraryLayout>(libraryFile, component)

    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    val metadata = MetadataLibraryImpl(metadataAccess)
    val ir = IrMonoliticLibraryImpl(irAccess)
//    val ir = IrPerFileLibraryImpl(irAccess)

    return KotlinLibraryImpl(base, metadata, ir)
}

fun createKotlinLibraryComponents(
    libraryFile: File,
    isDefault: Boolean = true
) : List<KotlinLibrary> {
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, null)
    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    return base.componentList.map {
        createKotlinLibrary(libraryFile, it, isDefault)
    }
}

val File.isPre_1_4_Library: Boolean
    get() {
        val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(this, null)
        val base = BaseKotlinLibraryImpl(baseAccess, false)
        return base.has_pre_1_4_manifest
    }