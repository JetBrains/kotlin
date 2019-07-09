/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.framework.JavaRuntimeDetectionUtil
import org.jetbrains.kotlin.idea.framework.isExternalLibrary
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.io.File
import java.io.IOException

fun updateLibraries(project: Project, libraries: Collection<Library>) {
    if (project.allModules().any { module -> module.getBuildSystemType() != BuildSystemType.JPS }) {
        Messages.showMessageDialog(
            project,
            "Automatic library version update for Maven and Gradle projects is currently unsupported. " +
                    "Please update your build scripts manually.",
            "Update Kotlin Runtime Library",
            Messages.getErrorIcon()
        )
        return
    }

    val kJvmConfigurator =
        getConfiguratorByName(KotlinJavaModuleConfigurator.NAME) as KotlinJavaModuleConfigurator?
                ?: error("Configurator with given name doesn't exists: " + KotlinJavaModuleConfigurator.NAME)

    val kJsConfigurator =
        getConfiguratorByName(KotlinJsModuleConfigurator.NAME) as KotlinJsModuleConfigurator?
                ?: error("Configurator with given name doesn't exists: " + KotlinJsModuleConfigurator.NAME)

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
    libraryJarDescriptor: LibraryJarDescriptor
) {
    val fileToReplace = libraryJarDescriptor.findExistingJar(library)
    if (fileToReplace == null) {
        if (libraryJarDescriptor.shouldExist) {
            error(
                "Update for library was requested, but file for replacement isn't present: \n" +
                        "name = ${library.name}\n" +
                        "isExternal = `${isExternalLibrary(library)}`\n" +
                        "entries = ${library.getUrls(libraryJarDescriptor.orderRootType)}\n" +
                        "buildSystems = ${project.allModules().map { module -> module.getBuildSystemType() }.distinct()}"
            )
        }

        return
    }

    val oldUrl = fileToReplace.url
    val jarPath: File = libraryJarDescriptor.getPathInPlugin()

    if (!jarPath.exists()) {
        showRuntimeJarNotFoundDialog(project, libraryJarDescriptor.jarName)
        return
    }

    val jarFileToReplace = getLocalJar(fileToReplace) ?: error("Couldn't find local jar for ${fileToReplace.canonicalPath}")
    val newVFile = try {
        replaceFile(jarPath, jarFileToReplace)
    } catch (e: IOException) {
        Messages.showErrorDialog(
            project,
            "Failed to update $jarPath: ${e.message}",
            "Library update failed"
        )
        return
    }

    if (newVFile != null) {
        val model = library.modifiableModel
        runWriteAction {
            try {
                model.removeRoot(oldUrl, libraryJarDescriptor.orderRootType)

                val newRoot = JarFileSystem.getInstance().getJarRootForLocalFile(newVFile) ?: run {
                    Messages.showErrorDialog(project, "Failed to find root for file: ${newVFile.canonicalPath}", "Library update failed")
                    return@runWriteAction
                }

                model.addRoot(newRoot, libraryJarDescriptor.orderRootType)
            } finally {
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