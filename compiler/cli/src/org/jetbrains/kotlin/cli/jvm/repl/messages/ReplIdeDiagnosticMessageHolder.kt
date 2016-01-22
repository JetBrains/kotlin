/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.repl.messages

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.w3c.dom.ls.DOMImplementationLS
import javax.xml.parsers.DocumentBuilderFactory

class ReplIdeDiagnosticMessageHolder : DiagnosticMessageHolder {
    private val diagnostics = arrayListOf<Pair<Diagnostic, String>>()

    override fun report(diagnostic: Diagnostic, file: PsiFile, render: String) {
        diagnostics.add(Pair(diagnostic, render))
    }

    override val renderedDiagnostics: String
        get() {
            val docFactory = DocumentBuilderFactory.newInstance()
            val docBuilder = docFactory.newDocumentBuilder()
            val errorReport = docBuilder.newDocument()

            val rootElement = errorReport.createElement("report")
            errorReport.appendChild(rootElement)

            for ((diagnostic, message) in diagnostics) {
                val errorRange = DiagnosticUtils.firstRange(diagnostic.textRanges)

                val reportEntry = errorReport.createElement("reportEntry")
                reportEntry.setAttribute("severity", diagnostic.severity.toString())
                reportEntry.setAttribute("rangeStart", errorRange.startOffset.toString())
                reportEntry.setAttribute("rangeEnd", errorRange.endOffset.toString())
                reportEntry.appendChild(errorReport.createTextNode(StringUtil.escapeXml(message)))

                rootElement.appendChild(reportEntry)
            }

            val domImplementation = errorReport.implementation as DOMImplementationLS
            val lsSerializer = domImplementation.createLSSerializer()
            return lsSerializer.writeToString(errorReport)
        }
}
