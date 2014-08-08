/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.android

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor.NoAndroidManifestFound
import java.util.ArrayList
import java.io.File
import java.io.FileInputStream
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import com.intellij.openapi.vfs.VirtualFile

open class AndroidResourceManagerBase(project: Project, searchPath: String?) : AndroidResourceManager(project, searchPath) {
    override fun getLayoutXmlFiles(): Collection<PsiFile> {
        val fileManager = VirtualFileManager.getInstance()
        val watchDir = fileManager.findFileByUrl("file://" + searchPath)
        val psiManager = PsiManager.getInstance(project)
        return watchDir?.getChildren()?.toArrayList()?.map { psiManager.findFile(it) }?.mapNotNull { it } ?: ArrayList(0)
    }

    protected fun vritualFileToPsi(vf: VirtualFile): PsiFile? {
        val psiManager = PsiManager.getInstance(project)
        return psiManager.findFile(vf)

    }

    override fun idToXmlAttribute(id: String): PsiElement? {
        return null
    }

    override fun renameXmlAttr(elem: PsiElement, newName: String) {
    }

    override fun renameProperty(oldName: String, newName: String) {
    }

    val saxParser: SAXParser = initSAX()

    protected fun initSAX(): SAXParser {
        val saxFactory = SAXParserFactory.newInstance()
        saxFactory?.setNamespaceAware(true)
        return saxFactory!!.newSAXParser()
    }

    override fun readManifest(): AndroidManifest {
        try {
            val manifestXml = File(searchPath!!).getParentFile()!!.getParentFile()!!.listFiles { it.name == "AndroidManifest.xml" }!!.first()
            var _package: String = ""
            try {
                saxParser.parse(FileInputStream(manifestXml), object : DefaultHandler() {
                    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                        if (localName == "manifest")
                            _package = attributes.toMap()["package"] ?: ""
                    }
                })
            }
            catch (e: Exception) {
                throw e
            }
            return AndroidManifest(_package)
        }
        catch (e: Exception) {
            throw NoAndroidManifestFound()
        }
    }
}
