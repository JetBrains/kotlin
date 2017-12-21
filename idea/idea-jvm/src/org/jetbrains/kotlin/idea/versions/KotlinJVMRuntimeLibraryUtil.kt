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

package org.jetbrains.kotlin.idea.versions

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.createConfigureKotlinNotificationCollector
import org.jetbrains.kotlin.idea.configuration.getConfiguratorByName
import org.jetbrains.kotlin.idea.framework.JavaRuntimeDetectionUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.io.File

fun updateLibraries(project: Project, libraries: Collection<Library>) {
    if (project.allModules().any { module -> KotlinPluginUtil.isMavenModule(module) }) {
        Messages.showMessageDialog(project, "Automatic library version update for Maven projects is currently unsupported. Please update your pom.xml manually.",
                                   "Update Kotlin Runtime Library",
                                   Messages.getErrorIcon())
        return
    }

    if (project.allModules().any { module -> KotlinPluginUtil.isGradleModule(module) }) {
        Messages.showMessageDialog(project, "Automatic library version update for Gradle projects is currently unsupported. Please update your build.gradle manually.",
                                   "Update Kotlin Runtime Library",
                                   Messages.getErrorIcon())
        return
    }

    val kJvmConfigurator = getConfiguratorByName(KotlinJavaModuleConfigurator.NAME) as KotlinJavaModuleConfigurator? ?:
                           error("Configurator with given name doesn't exists: " + KotlinJavaModuleConfigurator.NAME)

    val kJsConfigurator = getConfiguratorByName(KotlinJsModuleConfigurator.NAME) as KotlinJsModuleConfigurator? ?:
                          error("Configurator with given name doesn't exists: " + KotlinJsModuleConfigurator.NAME)

    val collector = createConfigureKotlinNotificationCollector(project)
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    // TODO use module SDK

    for (library in libraries) {
        val libraryJarDescriptors = if (JavaRuntimeDetectionUtil.getJavaRuntimeVersion(library) != null)
            kJvmConfigurator.getLibraryJarDescriptors(sdk)
        else
            kJsConfigurator.getLibraryJarDescriptors(sdk)

        for (libraryJarDescriptor in libraryJarDescriptors) {
            updateJar(project, library, libraryJarDescriptor)
        }
    }

    collector.showNotification()
}

private fun updateJar(
        project: Project,
        library: Library,
        libraryJarDescriptor: LibraryJarDescriptor) {
    val fileToReplace = libraryJarDescriptor.findExistingJar(library)

    if (fileToReplace == null && !libraryJarDescriptor.shouldExist) {
        return
    }

    val oldUrl = fileToReplace?.url
    val jarPath: File = libraryJarDescriptor.getPathInPlugin()

    if (!jarPath.exists()) {
        showRuntimeJarNotFoundDialog(project, libraryJarDescriptor.jarName)
        return
    }

    val jarFileToReplace = getLocalJar(fileToReplace)!!
    val newVFile = replaceFile(jarPath, jarFileToReplace)
    if (newVFile != null) {
        val model = library.modifiableModel
        runWriteAction {
            try {
                if (oldUrl != null) {
                    model.removeRoot(oldUrl, libraryJarDescriptor.orderRootType)
                }
                val newRoot = JarFileSystem.getInstance().getJarRootForLocalFile(newVFile)!!
                model.addRoot(newRoot, libraryJarDescriptor.orderRootType)
            }
            finally {
                model.commit()
            }
        }
    }
}

internal fun replaceFile(updatedFile: File, jarFileToReplace: VirtualFile): VirtualFile? {
    val jarIoFileToReplace = File(jarFileToReplace.path)

    if (FileUtil.filesEqual(updatedFile, jarIoFileToReplace)) {
        return null
    }

    FileUtil.copy(updatedFile, jarIoFileToReplace)
    if (jarIoFileToReplace.name != updatedFile.name) {
        val newFile = File(jarIoFileToReplace.parent, updatedFile.name)
        if (!newFile.exists()) {
            if (!jarIoFileToReplace.renameTo(newFile)) {
                LOG.info("Failed to rename ${jarIoFileToReplace.path} to ${newFile.path}")
                return null
            }
            val newVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(newFile)
            if (newVFile == null) {
                LOG.info("Failed to find ${newFile.path} in VFS")
                return null
            }
            newVFile.refresh(false, true)
            return newVFile
        }
    }
    jarFileToReplace.refresh(false, true)
    return null
}