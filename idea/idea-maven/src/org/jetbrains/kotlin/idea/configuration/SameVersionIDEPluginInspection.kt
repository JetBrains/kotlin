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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel

class SameVersionIDEPluginInspection : DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java) {
    private val idePluginVersion by lazy {
        SameVersionIDEPluginInspection::class.java.classLoader?.getResourceAsStream("META-INF/build.txt")?.bufferedReader()?.use { it.readText() }
    }

    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) {
            return
        }

        domFileElement.rootElement.build.plugins.plugins.filter { it.version.exists() && it.version.stringValue != idePluginVersion }.forEach { plugin ->
            createProblem(holder, plugin)
        }

        domFileElement.rootElement.build.pluginManagement.plugins.plugins.filter { it.version.exists() && it.version.stringValue != idePluginVersion }.forEach { plugin ->
            createProblem(holder, plugin)
        }
    }

    private fun createProblem(holder: DomElementAnnotationHolder, plugin: MavenDomPlugin) {
        holder.createProblem(plugin.version,
                             HighlightSeverity.WARNING,
                             "You use different IDE and Maven plugins' versions so you code's behaviour may be different")
    }
}