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

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.annotations.TestOnly
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.idea.inspections.PluginVersionDependentInspection
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion

class DifferentKotlinMavenVersionInspection : DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java), PluginVersionDependentInspection {
    private val idePluginVersion by lazy { bundledRuntimeVersion() }

    override var testVersionMessage: String? = null
        @TestOnly set

    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) {
            return
        }

        val project = domFileElement.module?.project ?: return
        val mavenManager = MavenProjectsManager.getInstance(project) ?: return

        if (!mavenManager.isMavenizedProject || !mavenManager.isManagedFile(domFileElement.file.virtualFile)) {
            return
        }

        val pomFile = PomFile.forFileOrNull(domFileElement.file) ?: return
        pomFile.findKotlinPlugins().filter { it.version.exists() && it.version.stringValue != idePluginVersion }.forEach { plugin ->
            createProblem(holder, plugin)
        }
    }

    private fun createProblem(holder: DomElementAnnotationHolder, plugin: MavenDomPlugin) {
        holder.createProblem(plugin.version,
                             HighlightSeverity.WARNING,
                             "Kotlin version that is used for building with Maven (${plugin.version.stringValue}) differs from the one bundled into the IDE plugin (${testVersionMessage ?: idePluginVersion})")
    }
}