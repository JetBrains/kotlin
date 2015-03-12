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
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.idea.JetPluginUtil
import org.jetbrains.kotlin.idea.vfilefinder.KotlinClassFileIndex
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

private val INSTALLED_KOTLIN_VERSION = "installed.kotlin.plugin.version"

/**
 * Component forces update for built-in libraries in plugin directory. They are ignored because of
 * com.intellij.util.indexing.FileBasedIndex.isUnderConfigOrSystem()
 */
class KotlinUpdatePluginComponent : ApplicationComponent {
    override fun initComponent() {
        if (ApplicationManager.getApplication()?.isUnitTestMode() == true) {
            return
        }

        val installedKotlinVersion = PropertiesComponent.getInstance()?.getValue(INSTALLED_KOTLIN_VERSION)

        if (installedKotlinVersion == null || JetPluginUtil.getPluginVersion() != installedKotlinVersion) {
            val ideaPluginPaths = PathUtil.getKotlinPathsForIdeaPlugin()

            // Force refresh jar handlers
            requestFullJarUpdate(ideaPluginPaths.getRuntimePath())
            requestFullJarUpdate(ideaPluginPaths.getReflectPath())
            requestFullJarUpdate(ideaPluginPaths.getRuntimeSourcesPath())

            requestFullJarUpdate(ideaPluginPaths.getJsStdLibJarPath())
            requestFullJarUpdate(ideaPluginPaths.getJsStdLibSrcJarPath())

            requestFullJarUpdate(ideaPluginPaths.getJdkAnnotationsPath())
            requestFullJarUpdate(ideaPluginPaths.getAndroidSdkAnnotationsPath())

            // Force update indices for files under config directory
            val fileBasedIndex = FileBasedIndex.getInstance()
            fileBasedIndex.requestRebuild(KotlinAbiVersionIndex.INSTANCE.getName())
            fileBasedIndex.requestRebuild(KotlinClassFileIndex.KEY)

            PropertiesComponent.getInstance()?.setValue(INSTALLED_KOTLIN_VERSION, JetPluginUtil.getPluginVersion())
        }
    }

    override fun getComponentName(): String {
        return "ReindexBundledRuntimeComponent"
    }

    override fun disposeComponent() {
    }

    private fun requestFullJarUpdate(jarFilePath: File) {
        val localVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(jarFilePath)

        // Build and update JarHandler
        val jarFile = JarFileSystem.getInstance()!!.getJarRootForLocalFile(localVirtualFile!!)
        VfsUtilCore.visitChildrenRecursively(jarFile!!, object : VirtualFileVisitor<Any?>() {})
        ((jarFile as NewVirtualFile)).markDirtyRecursively()

        // Synchronous refresh lead to deadlocks during components initialization KT-4584
        // jarFile.refresh(true, true)
        // VfsUtil.markDirtyAndRefresh(true, false, true, localVirtualFile)
    }
}
