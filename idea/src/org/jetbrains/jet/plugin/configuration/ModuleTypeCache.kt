package org.jetbrains.jet.plugin.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.openapi.util.DefaultModificationTracker
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.openapi.vfs.VirtualFileCopyEvent
import com.intellij.openapi.vfs.VirtualFilePropertyEvent

class ModuleTypeCacheManager private (project: Project) {
    class object {
        fun geInstance(project: Project) = ServiceManager.getService(project, javaClass<ModuleTypeCacheManager>())
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
        return cachedValue?.getValue(module)
    }

    private class VfsModificationTracker(project: Project): DefaultModificationTracker() {
        {
            val connection = project.getMessageBus().connect();
            connection.subscribe(VirtualFileManager.VFS_CHANGES, BulkVirtualFileListenerAdapter(
                    object : VirtualFileAdapter() {
                        override fun propertyChanged(event: VirtualFilePropertyEvent?) {
                            incModificationCount()
                        }

                        override fun fileCreated(event: VirtualFileEvent?) {
                            incModificationCount()
                        }

                        override fun fileDeleted(event: VirtualFileEvent?) {
                            incModificationCount()
                        }

                        override fun fileMoved(event: VirtualFileMoveEvent?) {
                            incModificationCount()
                        }

                        override fun fileCopied(event: VirtualFileCopyEvent?) {
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