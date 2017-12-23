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

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.util.text.VersionComparatorUtil
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.idea.inspections.ReplaceStringInDocumentFix
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.versions.DEPRECATED_LIBRARIES_INFORMATION

class DeprecatedMavenDependencyInspection : DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java) {
    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) return

        val file = domFileElement.file
        val module = domFileElement.module ?: return
        val manager = MavenProjectsManager.getInstance(module.project) ?: return
        val project = manager.findProject(module) ?: return

        val pomFile = PomFile.forFileOrNull(file) ?: return

        for (libInfo in DEPRECATED_LIBRARIES_INFORMATION) {
            pomFile.findDependencies(MavenId(libInfo.old.groupId, libInfo.old.name, null))
                    .filter { it.version?.stringValue != null }
                    .filter {
                        val libVersion = project.findDependencies(libInfo.old.groupId, libInfo.old.name).map { it.version }.distinct().singleOrNull()
                        libVersion != null && VersionComparatorUtil.COMPARATOR.compare(libVersion, libInfo.outdatedAfterVersion) >= 0
                    }
                    .forEach { dependency ->
                        val xmlElement = dependency.artifactId.xmlElement
                        if (xmlElement != null) {
                            val fix = ReplaceStringInDocumentFix(xmlElement, libInfo.old.name, libInfo.new.name)

                            holder.createProblem(dependency.artifactId,
                                                 ProblemHighlightType.LIKE_DEPRECATED,
                                                 libInfo.message,
                                                 null,
                                                 fix)
                        }
                    }
        }
    }
}