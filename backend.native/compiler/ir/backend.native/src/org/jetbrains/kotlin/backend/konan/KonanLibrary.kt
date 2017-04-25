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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.backend.konan.llvm.MetadataReader
import org.jetbrains.kotlin.backend.konan.llvm.loadSerializedModule
import org.jetbrains.kotlin.backend.konan.llvm.loadSerializedPackageFragment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.backend.konan.serialization.Base64
import org.jetbrains.kotlin.backend.konan.serialization.deserializeModule
import java.io.File

interface KonanLibrary {
    val libraryName: String
    val moduleName: String
    val moduleDescriptor: ModuleDescriptorImpl
    val bitcodePaths: List<String>
}

class KtBcLibrary(val file: File, val configuration: CompilerConfiguration): KonanLibrary {
    constructor(path: String, configuration: CompilerConfiguration) : this(File(path), configuration) 
    init {
        if (!file.exists()) 
            error("Path '" + file.path + "' does not exist")
    }

    override val libraryName: String
        get() = file.path

    override val bitcodePaths: List<String>
        get() = listOf(libraryName)

    private val reader = MetadataReader(file)

    private val namedModuleData by lazy {
        val currentAbiVersion = configuration.get(KonanConfigKeys.ABI_VERSION)!!
        reader.loadSerializedModule(currentAbiVersion)
    }
    override val moduleName = namedModuleData.name

    private val tableOfContentsAsString = namedModuleData.base64

    private fun packageMetadata(fqName: String): Base64 =
        reader.loadSerializedPackageFragment(fqName)

    override val moduleDescriptor: ModuleDescriptorImpl by lazy {
        deserializeModule(configuration, 
            {it -> packageMetadata(it)}, 
            tableOfContentsAsString, moduleName)
    }
}

