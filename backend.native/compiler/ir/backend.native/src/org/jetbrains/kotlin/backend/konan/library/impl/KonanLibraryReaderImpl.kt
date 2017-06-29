/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.library.impl

import org.jetbrains.kotlin.backend.konan.library.KonanLibrary
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.backend.konan.library.MetadataReader
import org.jetbrains.kotlin.backend.konan.serialization.deserializeModule
import org.jetbrains.kotlin.backend.konan.util.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class FileBasedLibraryReader(
    val file: File, val currentAbiVersion: Int,
    val reader: MetadataReader): KonanLibraryReader {

    override val libraryName: String
        get() = file.path

    val moduleHeaderData: ByteArray by lazy {
        reader.loadSerializedModule()
    }

    fun packageMetadata(fqName: String): ByteArray =
        reader.loadSerializedPackageFragment(fqName)

    override fun moduleDescriptor(specifics: LanguageVersionSettings) 
        = deserializeModule(specifics, {packageMetadata(it)}, 
            moduleHeaderData)
}


class LibraryReaderImpl(override val libDir: File, currentAbiVersion: Int,
        override val target: KonanTarget?) : 
            FileBasedLibraryReader(libDir, currentAbiVersion, MetadataReaderImpl(libDir)), 
            KonanLibrary  {

    public constructor(path: String, currentAbiVersion: Int, target: KonanTarget?) : 
        this(File(path), currentAbiVersion, target) 

    init {
        unpackIfNeeded()
    }

    // TODO: Search path processing is also goes somewhere around here.
    fun unpackIfNeeded() {
        // TODO: Clarify the policy here.
        if (libDir.exists) {
            if (libDir.isDirectory) return
        }
        if (!klibFile.exists) {
            error("Could not find neither $libDir nor $klibFile.")
        }
        if (klibFile.isFile) {
            klibFile.unzipAs(libDir)

            if (!libDir.exists) error("Could not unpack $klibFile as $libDir.")
        } else {
            error("Expected $klibFile to be a regular file.")
        }
    }

    val manifestProperties: Properties by lazy {
        manifestFile.loadProperties()
    }

    val abiVersion: String
        get() {
            val manifestAbiVersion = manifestProperties.getProperty("abi_version")
            if ("$currentAbiVersion" != manifestAbiVersion) 
                error("ABI version mismatch. Compiler expects: $currentAbiVersion, the library is $manifestAbiVersion")
            return manifestAbiVersion
        }

    override val bitcodePaths: List<String>
        get() = (kotlinDir.listFiles + nativeDir.listFiles).map{it.absolutePath}

    override val linkerOpts: List<String>
        get() = manifestProperties.propertyList("linkerOpts", target!!.targetSuffix)
}

