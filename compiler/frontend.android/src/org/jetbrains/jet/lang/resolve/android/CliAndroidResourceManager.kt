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

import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileInputStream
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor.NoAndroidManifestFound
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

public class CliAndroidResourceManager(project: Project, searchPath: String?, val manifestPath: String?) : AndroidResourceManagerBase(project, searchPath) {

    val saxParser: SAXParser = initSAX()

    protected fun initSAX(): SAXParser {
        val saxFactory = SAXParserFactory.newInstance()
        saxFactory?.setNamespaceAware(true)
        return saxFactory!!.newSAXParser()
    }
    override fun readManifest(): AndroidManifest {
        try {
            val manifestXml = File(manifestPath!!)
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
