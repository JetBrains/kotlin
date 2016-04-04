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

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.util.*

/**
 * Analyze maven modules graph and exclude all children from the [selectedModules] so only
 * topmost modules of [selectedModules] will remain.
 *
 * Example:
 *
 *
 * - root
 *   - module1
 *   - module2
 *     - module2.1
 *     - module2.2
 *   - module3
 *
 *
 * so `excludeMavenChildrenModules(project, listOf(module2, module2.2, module1)` -> `listOf(module1, module2)`
 *
 */
fun excludeMavenChildrenModules(project: Project, selectedModules: List<Module>): List<Module> {
    val mavenManager = MavenProjectsManager.getInstance(project)

    val projectsById = mavenManager.projects.associateBy { it.mavenId }
    val selectedProjects = selectedModules.mapNotNull { mavenManager.findProject(it) }
    val selectedIds = selectedProjects.mapTo(HashSet()) { it.mavenId }

    val excluded = HashSet<MavenId>(selectedProjects.size)
    for (m in selectedProjects) {
        if (m.mavenId !in excluded) {
            var current: MavenProject? = m
            while (current != null) {
                if (current.mavenId in excluded || (current != m && current.mavenId in selectedIds)) {
                    excluded.add(m.mavenId)
                    break
                }
                current = current.parentId?.let { projectsById[it] }
            }
        }
    }

    return selectedProjects.filter { it.mavenId !in excluded }.mapNotNull { mavenManager.findModule(it) }
}
