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

package org.jetbrains.kotlin.backend.konan.library

import org.jetbrains.kotlin.backend.konan.util.File
import java.util.Properties

class NamedModuleData(val name:String, val base64: String)

interface MetadataReader {
    fun loadSerializedModule(currentAbiVersion: Int): NamedModuleData
    fun loadSerializedPackageFragment(fqName: String): String
}

class SplitMetadataReader(override val libDir: File) : MetadataReader, SplitScheme {

    override fun loadSerializedModule(currentAbiVersion: Int): NamedModuleData {
        val header = Properties()
        manifestFile.bufferedReader().use { reader ->
            header.load(reader)
        }
        val headerAbiVersion = header.getProperty("abi_version")!!
        val moduleName = header.getProperty("module_name")!!
        val moduleData = moduleHeaderFile.readText()

        if ("$currentAbiVersion" != headerAbiVersion) 
            error("ABI version mismatch. Compiler expects: $currentAbiVersion, the library is $headerAbiVersion")

        return NamedModuleData(moduleName, moduleData)
    }

    override fun loadSerializedPackageFragment(fqName: String) 
        = packageFile(fqName).readText()
}

internal class SplitMetadataGenerator(override val libDir: File): SplitScheme {

    fun addLinkData(linkData: LinkData) {

        val linkdataDir = File(libDir, "linkdata")
        val header = Properties()
        header.putAll(hashMapOf(
            "abi_version" to "${linkData.abiVersion}",
            "module_name" to "${linkData.moduleName}"
        ))
        moduleHeaderFile.writeText(linkData.module)
        header.store(manifestFile.outputStream(), null)

        linkData.fragments.forEachIndexed { index, it ->
            val name = linkData.fragmentNames[index] 
            packageFile(name).writeText(it)
        }
    }
}

