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
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger

class BaseKotlinLibraryImpl(
    val access: BaseLibraryAccess<KotlinLibraryLayout>,
    override val isDefault: Boolean
) : BaseKotlinLibrary {
    override val libraryFile get() = access.klib
    override val libraryName: String by lazy { access.inPlace { it.libraryName } }

    override val componentList: List<String> by lazy {
        access.inPlace { layout ->
            val listFiles = layout.libFile.listFiles
            listFiles
                .filter { it.isDirectory }
                .filter { it.listFiles.map { it.name }.contains(KLIB_MANIFEST_FILE_NAME) }
                .map { it.name }
        }
    }

    override fun toString() = "$libraryName[default=$isDefault]"

    override val manifestProperties: Properties by lazy {
        access.inPlace { it.manifestFile.loadProperties() }
    }

    override val versions: KotlinLibraryVersioning by lazy {
        manifestProperties.readKonanLibraryVersioning()
    }
}

class MetadataLibraryImpl(
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
    override val hasIr by lazy {
        access.inPlace { it: IrKotlinLibraryLayout ->
            it.irDir.exists
        }
    }
}

class IrMonoliticLibraryImpl(_access: IrLibraryAccess<IrKotlinLibraryLayout>) : IrLibraryImpl(_access) {
    override fun fileCount(): Int = files.entryCount()

    override fun irDeclaration(index: Int, fileIndex: Int) = loadIrDeclaration(index, fileIndex)

    override fun type(index: Int, fileIndex: Int) = types.tableItemBytes(fileIndex, index)

    override fun signature(index: Int, fileIndex: Int) = signatures.tableItemBytes(fileIndex, index)

    override fun string(index: Int, fileIndex: Int) = strings.tableItemBytes(fileIndex, index)

    override fun body(index: Int, fileIndex: Int) = bodies.tableItemBytes(fileIndex, index)

    override fun debugInfo(index: Int, fileIndex: Int) = debugInfos?.tableItemBytes(fileIndex, index)

    override fun file(index: Int) = files.tableItemBytes(index)

    private fun loadIrDeclaration(index: Int, fileIndex: Int) =
        combinedDeclarations.tableItemBytes(fileIndex, DeclarationId(index))

    private val combinedDeclarations: DeclarationIrMultiTableFileReader by lazy {
        DeclarationIrMultiTableFileReader(access.realFiles {
            it.irDeclarations
        })
    }

    private val types: IrMultiArrayFileReader by lazy {
        IrMultiArrayFileReader(access.realFiles {
            it.irTypes
        })
    }

    private val signatures: IrMultiArrayFileReader by lazy {
        IrMultiArrayFileReader(access.realFiles {
            it.irSignatures
        })
    }

    private val strings: IrMultiArrayFileReader by lazy {
        IrMultiArrayFileReader(access.realFiles {
            it.irStrings
        })
    }

    private val bodies: IrMultiArrayFileReader by lazy {
        IrMultiArrayFileReader(access.realFiles {
            it.irBodies
        })
    }

    private val debugInfos: IrMultiArrayFileReader? by lazy {
        access.realFiles {
            it.irDebugInfo.let { diFile -> if (diFile.exists) IrMultiArrayFileReader(diFile) else null }
        }
    }

    private val files: IrArrayFileReader by lazy {
        IrArrayFileReader(access.realFiles {
            it.irFiles
        })
    }

    override fun types(fileIndex: Int): ByteArray {
        return types.tableItemBytes(fileIndex)
    }

    override fun signatures(fileIndex: Int): ByteArray {
        return signatures.tableItemBytes(fileIndex)
    }

    override fun strings(fileIndex: Int): ByteArray {
        return strings.tableItemBytes(fileIndex)
    }

    override fun declarations(fileIndex: Int): ByteArray {
        return combinedDeclarations.tableItemBytes(fileIndex)
    }

    override fun bodies(fileIndex: Int): ByteArray {
        return bodies.tableItemBytes(fileIndex)
    }
}

class IrPerFileLibraryImpl(_access: IrLibraryAccess<IrKotlinLibraryLayout>) : IrLibraryImpl(_access) {

    private val directories by lazy {
        access.realFiles {
            it.irDir.listFiles.filter { f -> f.isDirectory && f.name.endsWith(".file") }
        }
    }

    private val fileToDeclarationMap = mutableMapOf<Int, DeclarationIrTableFileReader>()
    override fun irDeclaration(index: Int, fileIndex: Int): ByteArray {
        val dataReader = fileToDeclarationMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            DeclarationIrTableFileReader(access.realFiles {
                it.irDeclarations(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(DeclarationId(index))
    }

    private val fileToTypeMap = mutableMapOf<Int, IrArrayFileReader>()
    override fun type(index: Int, fileIndex: Int): ByteArray {
        val dataReader = fileToTypeMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            IrArrayFileReader(access.realFiles {
                it.irTypes(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(index)
    }

    private fun signatureDataReader(fileIndex: Int): IrArrayFileReader {
        return fileToTypeMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            IrArrayFileReader(access.realFiles {
                it.irSignatures(fileDirectory)
            })
        }
    }

    override fun signature(index: Int, fileIndex: Int): ByteArray {
        val dataReader = signatureDataReader(fileIndex)
        return dataReader.tableItemBytes(index)
    }

    private val fileToStringMap = mutableMapOf<Int, IrArrayFileReader>()
    override fun string(index: Int, fileIndex: Int): ByteArray {
        val dataReader = fileToStringMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            IrArrayFileReader(access.realFiles {
                it.irStrings(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(index)
    }

    private val fileToBodyMap = mutableMapOf<Int, IrArrayFileReader>()
    override fun body(index: Int, fileIndex: Int): ByteArray {
        val dataReader = fileToBodyMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            IrArrayFileReader(access.realFiles {
                it.irBodies(fileDirectory)
            })
        }
        return dataReader.tableItemBytes(index)
    }


    private val fileToDebugInfoMap = mutableMapOf<Int, IrArrayFileReader?>()
    override fun debugInfo(index: Int, fileIndex: Int): ByteArray? {
        val dataReader = fileToDebugInfoMap.getOrPut(fileIndex) {
            val fileDirectory = directories[fileIndex]
            access.realFiles {
                it.irDebugInfo(fileDirectory).let { diFile ->
                    if (diFile.exists) {
                        IrArrayFileReader(diFile)
                    } else null
                }
            }

        }
        return dataReader?.tableItemBytes(index)
    }

    override fun file(index: Int): ByteArray {
        return access.realFiles {
            it.irFile(directories[index]).readBytes()
        }
    }

    override fun fileCount(): Int {
        return directories.size
    }

    override fun types(fileIndex: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun signatures(fileIndex: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun strings(fileIndex: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun declarations(fileIndex: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun bodies(fileIndex: Int): ByteArray {
        TODO("Not yet implemented")
    }
}

class KotlinLibraryImpl(
    val base: BaseKotlinLibraryImpl,
    val metadata: MetadataLibraryImpl,
    val ir: IrLibraryImpl
) : KotlinLibrary,
    BaseKotlinLibrary by base,
    MetadataLibrary by metadata,
    IrLibrary by ir {
    override fun toString(): String = buildString {
        append("name ")
        append(base.libraryName)
        append(", ")
        append("file: ")
        append(base.libraryFile.path)
        append(", ")
        append("version: ")
        append(base.versions)
        interopFlag?.let { append(", interop: $it") }
        irProviderName?.let { append(", IR provider: $it") }
        nativeTargets.takeIf { it.isNotEmpty() }?.joinTo(this, ", ", ", native targets: {", "}")
        append(')')
    }
}

fun createKotlinLibrary(
    libraryFile: File,
    component: String,
    isDefault: Boolean = false,
    perFile: Boolean = false,
    zipAccessor: ZipFileSystemAccessor? = null,
): KotlinLibrary {
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, component, zipAccessor)
    val metadataAccess = MetadataLibraryAccess<MetadataKotlinLibraryLayout>(libraryFile, component, zipAccessor)
    val irAccess = IrLibraryAccess<IrKotlinLibraryLayout>(libraryFile, component, zipAccessor)

    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    val metadata = MetadataLibraryImpl(metadataAccess)
    val ir = if (perFile) IrPerFileLibraryImpl(irAccess) else IrMonoliticLibraryImpl(irAccess)

    return KotlinLibraryImpl(base, metadata, ir)
}

fun createKotlinLibraryComponents(
    libraryFile: File,
    isDefault: Boolean = true,
    zipAccessor: ZipFileSystemAccessor? = null,
): List<KotlinLibrary> {
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, null, zipAccessor)
    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    return base.componentList.map {
        createKotlinLibrary(libraryFile, it, isDefault, zipAccessor = zipAccessor)
    }
}

fun isKotlinLibrary(libraryFile: File): Boolean = try {
    val libraryPath = libraryFile.absolutePath

    /**
     * Important: Try to resolve it as a "lenient" library. This will allow to probe a library
     * without logging any errors to [DummyLogger] and without any side effects such as throwing an
     * exception from [SingleKlibComponentResolver.resolve] if the library is not found.
     */
    SingleKlibComponentResolver(
        klibFile = libraryPath,
        logger = object : Logger {
            override fun log(message: String) = Unit // don't log
            override fun error(message: String) = Unit // don't log
            override fun warning(message: String) = Unit // don't log

            @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
            override fun fatal(message: String): Nothing = kotlin.error("This function should not be called")
        },
        knownIrProviders = emptyList()
    ).resolve(
        LenientUnresolvedLibrary(libraryPath)
    ) != null
} catch (e: Throwable) {
    false
}

fun isKotlinLibrary(libraryFile: java.io.File): Boolean =
    isKotlinLibrary(File(libraryFile.absolutePath))
