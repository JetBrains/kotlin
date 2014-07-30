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

import com.intellij.openapi.vfs.VirtualFileManager
import java.util.ArrayList
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.io.ByteArrayInputStream
import org.xml.sax.InputSource
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import com.intellij.psi.PsiElement

class CliAndroidUIXmlParser(val project: Project, override val searchPath: String?): AndroidUIXmlParser() {

    override var androidAppPackage: String = ""


    override fun lazySetup() {
        populateQueue(project)
        androidAppPackage = readManifest()._package
    }

    override fun parseSingleFileImpl(file: PsiFile): String {
        val ids: MutableCollection<AndroidWidget> = ArrayList()
        val handler = AndroidXmlHandler({ id, wClass -> ids.add(AndroidWidget(id, wClass)) })
        try {
            val source = InputSource(ByteArrayInputStream(file.getText()!!.getBytes("utf-8")))
            saxParser.parse(source, handler)
            return produceKotlinProperties(KotlinStringWriter(), ids).toString()
        } catch (e: Throwable) {
            LOG.error(e)
            return ""
        }
    }

    override fun renameId(oldName: String?, newName: String?, allRenames: MutableMap<PsiElement, String>) {
        return
    }
}

