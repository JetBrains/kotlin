/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.script

import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.Tag
import java.io.File
import java.io.InputStream
import java.util.*

@Tag("import")
class KotlinScriptExternalDependenciesConfig : KotlinScriptExternalDependencies {
    @Tag("classpath")
    @AbstractCollection(surroundWithTag = false, elementTag = "path", elementValueAttribute = "")
    override var classpath: MutableList<String> = ArrayList()

    @Tag("imports")
    @AbstractCollection(surroundWithTag = false, elementTag = "name", elementValueAttribute = "")
    override var imports: MutableList<String> = ArrayList()

    @Tag("sources")
    @AbstractCollection(surroundWithTag = false, elementTag = "path", elementValueAttribute = "")
    override var sources: MutableList<String> = ArrayList()
}

fun loadScriptExternalImportConfigs(configFile: File): List<KotlinScriptExternalDependenciesConfig> =
        JDOMUtil.loadDocument(configFile).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScriptExternalDependenciesConfig::class.java)
        }

fun loadScriptExternalImportConfigs(configStream: InputStream): List<KotlinScriptExternalDependenciesConfig> =
        JDOMUtil.loadDocument(configStream).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScriptExternalDependenciesConfig::class.java)
        }

fun <TF> getScriptDependenciesFromConfig(file: TF): KotlinScriptExternalDependencies? {
    val IMPORTS_FILE_EXTENSION = ".ktsimports.xml"
    fun streamFromSibling(file: VirtualFile): InputStream? =
            file.parent.findFileByRelativePath(file.name + IMPORTS_FILE_EXTENSION)?.let { it.inputStream }
    fun streamFromSibling(file: File): InputStream? {
        val sibling = File(file.parentFile, file.name + IMPORTS_FILE_EXTENSION)
        return if (sibling.exists()) sibling.inputStream()
               else null
    }
    return when (file) {
        is VirtualFile -> streamFromSibling(file)
        is PsiFile -> streamFromSibling(file.originalFile.virtualFile)
        is File -> streamFromSibling(file)
        else -> throw IllegalArgumentException("Unsupported file type $file")
    }?.let { KotlinScriptExternalDependenciesUnion(loadScriptExternalImportConfigs(it)) }
}
