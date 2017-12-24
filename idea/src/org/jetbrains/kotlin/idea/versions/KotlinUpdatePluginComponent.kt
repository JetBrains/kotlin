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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import java.io.File

private val INSTALLED_KOTLIN_VERSION = "installed.kotlin.plugin.version"

/**
 * Component forces update for built-in libraries in plugin directory. They are ignored because of
 * com.intellij.util.indexing.FileBasedIndex.isUnderConfigOrSystem()
 */
class KotlinUpdatePluginComponent : ApplicationComponent {
    override fun initComponent() {
        if (ApplicationManager.getApplication()?.isUnitTestMode == true) {
            return
        }

        val installedKotlinVersion = PropertiesComponent.getInstance()?.getValue(INSTALLED_KOTLIN_VERSION)

        if (installedKotlinVersion == null || KotlinPluginUtil.getPluginVersion() != installedKotlinVersion) {
            // Force refresh jar handlers
            for (libraryJarDescriptor in LibraryJarDescriptor.values()) {
                requestFullJarUpdate(libraryJarDescriptor.getPathInPlugin())
            }

            PropertiesComponent.getInstance()?.setValue(INSTALLED_KOTLIN_VERSION, KotlinPluginUtil.getPluginVersion())
        }
    }

    override fun getComponentName(): String {
        return "ReindexBundledRuntimeComponent"
    }

    override fun disposeComponent() {
    }

    private fun requestFullJarUpdate(jarFilePath: File) {
        val localVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(jarFilePath) ?: return

        // Build and update JarHandler
        val jarFile = JarFileSystem.getInstance().getJarRootForLocalFile(localVirtualFile) ?: return
        VfsUtilCore.visitChildrenRecursively(jarFile, object : VirtualFileVisitor<Any?>() {})
        ((jarFile as NewVirtualFile)).markDirtyRecursively()
    }
}
