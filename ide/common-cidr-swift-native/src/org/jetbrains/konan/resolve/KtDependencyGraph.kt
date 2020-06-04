package org.jetbrains.konan.resolve

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesPackListener
import gnu.trove.THashSet
import gnu.trove.TObjectHash
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.konan.resolve.symbols.KtDependencyMarker
import org.jetbrains.kotlin.idea.KotlinFileType

@Service
class KtDependencyGraph(project: Project) {
    private val dependent: MultiMap<VirtualFile, VirtualFile> = MultiMap.createSet()

    init {
        VirtualFileManager.getInstance().addAsyncFileListener(AsyncFileListener { events ->
            val fileTypeManager = FileTypeManager.getInstance()
            val changedFiles = THashSet<VirtualFile>()
            val affectedModules = THashSet<Module>()
            val dirtyFiles = THashSet<VirtualFile>()

            fun didChange(file: VirtualFile) {
                if (fileTypeManager.isFileOfType(file, KotlinFileType.INSTANCE) && changedFiles.add(file)) {
                    dirtyFiles += getDependentFiles(file)
                }
            }

            fun willCreate(parent: VirtualFile, name: String) {
                if (fileTypeManager.getFileTypeByFileName(name) === KotlinFileType.INSTANCE && !fileTypeManager.isFileIgnored(name)) {
                    val module = ModuleUtil.findModuleForFile(parent, project)?.takeIf { affectedModules.add(it) } ?: return
                    //TODO: Is there a better way with LookupTracker?
                    dirtyFiles += FileTypeIndex.getFiles(KotlinFileType.INSTANCE, module.moduleWithDependentsScope)
                }
            }

            for (event in events) {
                ProgressManager.checkCanceled()
                when (event) {
                    is VFileMoveEvent -> {
                        didChange(event.file)
                        willCreate(event.newParent, event.file.name)
                    }
                    is VFileCreateEvent -> willCreate(event.parent, event.childName)
                    is VFilePropertyChangeEvent -> when {
                        isFileNameChange(event) -> {
                            didChange(event.file)
                            willCreate(event.file.parent, event.newValue.toString())
                        }
                        isSymlinkChange(event) -> didChange(event.file)
                    }
                    is VFileDeleteEvent -> didChange(event.file)
                    is VFileContentChangeEvent -> didChange(event.file)
                    is VFileCopyEvent -> willCreate(event.newParent, event.newChildName)
                }
            }

            if (dirtyFiles.isEmpty) return@AsyncFileListener null
            object : AsyncFileListener.ChangeApplier {
                override fun afterVfsChange() {
                    FileSymbolTablesCache.getInstance(project).invalidateDirtyIncludeFiles(dirtyFiles)
                }
            }
        }, project)
    }

    @Synchronized
    private fun addDependencies(file: VirtualFile, dependencies: TObjectHash<VirtualFile>) {
        dependencies.forEach {
            dependent.putValue(it, file)
            true
        }
    }

    @Synchronized
    private fun removeDependencies(file: VirtualFile, dependencies: TObjectHash<VirtualFile>) {
        dependencies.forEach {
            dependent.remove(it, file)
            true
        }
    }

    @Synchronized
    private fun getDependentFiles(file: VirtualFile): ImmutableList<VirtualFile> = dependent.get(file).toImmutableList()

    class Listener(val project: Project) : FileSymbolTablesPackListener {
        override fun afterTableAddedToPack(table: FileSymbolTable) {
            val marker = table.contents.lastOrNull() as? KtDependencyMarker ?: return
            getInstance(project)
                .addDependencies(table.containingFile, marker.dependencies)
        }

        override fun beforeTableRemovedFromPack(table: FileSymbolTable) {
            val marker = table.contents.lastOrNull() as? KtDependencyMarker ?: return
            getInstance(project)
                .removeDependencies(table.containingFile, marker.dependencies)
        }
    }

    companion object {
        fun getInstance(project: Project): KtDependencyGraph = project.service()

        private fun isFileNameChange(propertyChangeEvent: VFilePropertyChangeEvent): Boolean =
            propertyChangeEvent.propertyName == VirtualFile.PROP_NAME && propertyChangeEvent.oldValue != propertyChangeEvent.newValue

        private fun isSymlinkChange(propertyChangeEvent: VFilePropertyChangeEvent): Boolean =
            propertyChangeEvent.propertyName == VirtualFile.PROP_SYMLINK_TARGET
    }
}