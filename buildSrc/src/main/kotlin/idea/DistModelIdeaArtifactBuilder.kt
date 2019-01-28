/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildUtils.idea

import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.ArtifactType
import org.jetbrains.gradle.ext.RecursiveArtifact

class DistModelIdeaArtifactBuilder(val rootProject: Project) {
    fun RecursiveArtifact.addFiles(vFile: DistVFile, inJar: Boolean = false) {
        val files = mutableSetOf<String>()

        vFile.contents.forEach {
            when (it) {
                is DistCopy -> {
                    val file = it.src.file
                    when {
                        inJar && file.name.endsWith(".jar") -> extractedDirectory(file.path)
                        file.isDirectory -> {
                            files.add(file.name)
                            directoryContent(file.path)
                        }
                        else -> {
                            files.add(file.name)
                            file(file.path)
                        }
                    }
                }
                is DistModuleOutput -> {
                    val name = it.ideaModuleName

                    if (name.result != null) moduleOutput(name.result + "_main")
                    else logger.warn("Cannot find idea module name for project `${it.projectId}`: ${name.error}")
                }
            }
        }

        vFile.child.values.forEach {
            if (it.name !in files) {
                when {
                    it.name.endsWith(".jar") -> archive(it.name).addFiles(it, true)
                    else -> directory(it.name).addFiles(it, inJar)
                }
            }
        }
    }

    class Result<T: Any>(val result: T? = null, val error: String? = null)

    val DistModuleOutput.ideaModuleName: Result<String>
        get() {
            val findProject = rootProject.findProject(projectId) ?: return Result(error = "cannot find gradle project $projectId")
            val idea = findProject.extensions?.findByName("idea") as? IdeaModel ?: return Result(error = "cannot find idea model for gradle project $projectId")
            val name = idea.module?.name ?: return Result(error = "idea model for project $projectId has no module name")
            return Result(name)
        }
}