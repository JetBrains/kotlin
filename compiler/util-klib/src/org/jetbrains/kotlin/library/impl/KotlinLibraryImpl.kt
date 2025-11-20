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
import org.jetbrains.kotlin.konan.file.ZipFileSystemInPlaceAccessor
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.library.*

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

class KotlinLibraryImpl(
    override val location: File,
    zipFileSystemAccessor: ZipFileSystemAccessor,
    val base: BaseKotlinLibraryImpl,
) : KotlinLibrary,
    BaseKotlinLibrary by base {

    private val components = KlibComponentsCache(
        layoutReaderFactory = KlibLayoutReaderFactory(
            klibFile = location,
            zipFileSystemAccessor = zipFileSystemAccessor
        )
    )

    override fun <KC : KlibComponent> getComponent(kind: KlibComponent.Kind<KC, *>) = components.getComponent(kind)

    override val attributes = KlibAttributes()

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
    val nonNullZipFileSystemAccessor = zipAccessor ?: ZipFileSystemInPlaceAccessor

    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, component, nonNullZipFileSystemAccessor)
    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)

    return KotlinLibraryImpl(libraryFile, nonNullZipFileSystemAccessor, base)
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
