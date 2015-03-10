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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.openapi.vfs.VirtualFileCopyEvent
import com.intellij.openapi.vfs.VirtualFilePropertyEvent
import com.intellij.openapi.util.SimpleModificationTracker
import kotlin.platform.platformStatic

class ModuleTypeCacheManager private (project: Project) {
    default object {
        platformStatic fun getInstance(project: Project) = ServiceManager.getService(project, javaClass<ModuleTypeCacheManager>())
    }

    private val vfsModificationTracker = VfsModificationTracker(project)

    private val cachedValue = CachedValuesManager.getManager(project).createParameterizedCachedValue(
            {
                (module: Module?) ->
                val moduleType = if (module != null) computeType(module) else null
                CachedValueProvider.Result.create<ModuleType>(moduleType, vfsModificationTracker)
            }, false)

    fun isGradleModule(module: Module) = getModuleType(module) == ModuleType.GRADLE

    private fun getModuleType(module: Module): ModuleType? {
        return cachedValue.getValue(module)
    }

    private class VfsModificationTracker(project: Project): SimpleModificationTracker() {
        {
            val connection = project.getMessageBus().connect();
            connection.subscribe(VirtualFileManager.VFS_CHANGES, BulkVirtualFileListenerAdapter(
                    object : VirtualFileAdapter() {
                        override fun propertyChanged(event: VirtualFilePropertyEvent) {
                            incModificationCount()
                        }

                        override fun fileCreated(event: VirtualFileEvent) {
                            incModificationCount()
                        }

                        override fun fileDeleted(event: VirtualFileEvent) {
                            incModificationCount()
                        }

                        override fun fileMoved(event: VirtualFileMoveEvent) {
                            incModificationCount()
                        }

                        override fun fileCopied(event: VirtualFileCopyEvent) {
                            incModificationCount()
                        }
                    }
            ))
        }
    }
}

enum class ModuleType {
    GRADLE
    OTHER
}

private fun computeType(module: Module) =
        when {
            isGradleModule(module) -> ModuleType.GRADLE
            else -> ModuleType.OTHER
        }

private val DEFAULT_SCRIPT_NAME = "build.gradle"

private fun isGradleModule(module: Module): Boolean {
    val moduleFile = module.getModuleFile()
    if (moduleFile == null){
        return false
    }

    val buildFile = moduleFile.getParent()?.findChild(DEFAULT_SCRIPT_NAME)
    return buildFile != null && buildFile.exists()
}
