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
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
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

class IrLibraryImpl(val access: IrLibraryAccess<IrKotlinLibraryLayout>) : IrLibrary {
    override val hasMainIr by lazy {
        access.inPlace { it: IrKotlinLibraryLayout ->
            it.mainIr.dir.exists
        }
    }
    override val mainIr = IrDirectory { mainIr }

    override val hasInlinableFunsIr by lazy {
        access.inPlace { it: IrKotlinLibraryLayout ->
            it.inlineableFunsIr.dir.exists
        }
    }
    override val inlinableFunsIr = IrDirectory { inlineableFunsIr }

    inner class IrDirectory(private val selectIrDir: IrKotlinLibraryLayout.() -> IrKotlinLibraryLayout.IrDirectory) : IrLibrary.IrDirectory {
        override fun fileCount(): Int = files.entryCount()

        override fun irDeclaration(index: Int, fileIndex: Int) = loadIrDeclaration(index, fileIndex)

        override fun type(index: Int, fileIndex: Int) = types.tableItemBytes(fileIndex, index)

        override fun signature(index: Int, fileIndex: Int) = signatures.tableItemBytes(fileIndex, index)

        override fun string(index: Int, fileIndex: Int) = strings.tableItemBytes(fileIndex, index)

        override fun body(index: Int, fileIndex: Int) = bodies.tableItemBytes(fileIndex, index)

        override fun debugInfo(index: Int, fileIndex: Int) = debugInfos?.tableItemBytes(fileIndex, index)

        override fun fileEntry(index: Int, fileIndex: Int) = fileEntries?.tableItemBytes(fileIndex, index)

        override fun file(index: Int) = files.tableItemBytes(index)

        private fun loadIrDeclaration(index: Int, fileIndex: Int) =
            combinedDeclarations.tableItemBytes(fileIndex, DeclarationId(index))

        private val combinedDeclarations: DeclarationIdMultiTableReader by lazy {
            DeclarationIdMultiTableReader(access) { selectIrDir().irDeclarations}
        }

        private val types: IrMultiArrayReader by lazy {
            IrMultiArrayReader(access) { selectIrDir().irTypes }
        }

        private val signatures: IrMultiArrayReader by lazy {
            IrMultiArrayReader(access) { selectIrDir().irSignatures }
        }

        private val strings: IrMultiArrayReader by lazy {
            IrMultiArrayReader(access) { selectIrDir().irStrings }
        }

        private val bodies: IrMultiArrayReader by lazy {
            IrMultiArrayReader(access) { selectIrDir().irBodies }
        }

        private val debugInfos: IrMultiArrayReader? by lazy {
            if (access.inPlace { it.selectIrDir().irDebugInfo.exists })
                IrMultiArrayReader(access) { selectIrDir().irDebugInfo }
            else
                null
        }

        private val fileEntries: IrMultiArrayReader? by lazy {
            if (access.inPlace { it.selectIrDir().irFileEntries.exists })
                IrMultiArrayReader(access) { selectIrDir().irFileEntries }
            else
                null
        }

        private val files: IrArrayReader by lazy {
            IrArrayReader(access) { selectIrDir().irFiles }
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

        override fun fileEntries(fileIndex: Int): ByteArray? {
            return fileEntries?.tableItemBytes(fileIndex)
        }
    }
}

class KotlinLibraryImpl(
    override val location: File,
    val base: BaseKotlinLibraryImpl,
    val ir: IrLibraryImpl
) : KotlinLibrary,
    BaseKotlinLibrary by base,
    IrLibrary by ir {

    private val components: Map<KlibComponent.ID<*>, KlibComponent> = run {
        val layoutReaderFactory = KlibLayoutReaderFactory(location, ir.access.klibZipAccessor)
        mapOf(KlibMetadataComponent.ID to KlibMetadataComponentImpl(layoutReaderFactory))
    }

    override fun <KC : KlibComponent> getComponent(id: KlibComponent.ID<KC>): KC {
        @Suppress("UNCHECKED_CAST")
        val component = components[id] as KC?
        return component ?: error("Unknown component $id")
    }

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
    zipAccessor: ZipFileSystemAccessor? = null,
): KotlinLibrary {
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, component, zipAccessor)
    val irAccess = IrLibraryAccess<IrKotlinLibraryLayout>(libraryFile, component, zipAccessor)

    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    val ir = IrLibraryImpl(irAccess)

    return KotlinLibraryImpl(libraryFile, base, ir)
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
