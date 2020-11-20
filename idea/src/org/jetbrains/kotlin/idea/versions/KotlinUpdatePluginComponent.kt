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

package org.jetbrains.kotlin.idea.versions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import java.io.File

/**
 * Component forces update for built-in libraries in plugin directory. They are ignored because of
 * com.intellij.util.indexing.FileBasedIndex.isUnderConfigOrSystem()
 */
class KotlinUpdatePluginStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        if (isUnitTestMode()) return

        val propertiesComponent = PropertiesComponent.getInstance() ?: return

        val installedKotlinVersion = propertiesComponent.getValue(INSTALLED_KOTLIN_VERSION)

        if (KotlinPluginUtil.getPluginVersion() != installedKotlinVersion) {
            // Force refresh jar handlers
            for (libraryJarDescriptor in LibraryJarDescriptor.values()) {
                requestFullJarUpdate(libraryJarDescriptor.getPathInPlugin())
            }

            propertiesComponent.setValue(INSTALLED_KOTLIN_VERSION, KotlinPluginUtil.getPluginVersion())
        }
    }

    private fun requestFullJarUpdate(jarFilePath: File) {
        val localVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(jarFilePath) ?: return

        // Build and update JarHandler
        val jarFile = JarFileSystem.getInstance().getJarRootForLocalFile(localVirtualFile) ?: return
        VfsUtilCore.visitChildrenRecursively(jarFile, object : VirtualFileVisitor<Any?>() {})
        ((jarFile as NewVirtualFile)).markDirtyRecursively()
    }

    companion object {
        private const val INSTALLED_KOTLIN_VERSION = "installed.kotlin.plugin.version"
    }
}
