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

package org.jetbrains.kotlin.idea.js

import com.intellij.ProjectTopics
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.OrderEntryUtil
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.FileContentUtilCore
import com.intellij.util.PathUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.framework.KotlinJavaScriptLibraryDetectionUtil
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil.isJsKotlinModule
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

public class KotlinJavaScriptLibraryManager private constructor(private var myProject: Project?) : ProjectComponent, ModuleRootListener, BulkFileListener {
    private val myMuted = AtomicBoolean(false)

    private val libraryFileStampMap: MutableMap<String, Long> = Collections.synchronizedMap(hashMapOf())

    override fun getComponentName(): String = "KotlinJavascriptLibraryManager"

    override fun projectOpened() {
        val project = myProject!!
        project.messageBus.connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, this)
        project.messageBus.connect(project).subscribe(VirtualFileManager.VFS_CHANGES, this)
        DumbService.getInstance(project).smartInvokeLater() { updateProjectLibrary() }
    }

    override fun projectClosed() {
    }

    override fun initComponent() {
    }

    override fun disposeComponent() {
        myProject = null
    }

    override fun after(events: List<VFileEvent>) {
        if (myMuted.get()) return

        val changedFiles = events
                .filter { it !is MyVFileContentChangeEvent && it is VFileContentChangeEvent }
                .map { it.file }
                .filterNotNull()

        val files = update(changedFiles, addToMapIfAbsent = false)
        val application = ApplicationManager.getApplication()
        refreshFiles(files, application.isUnitTestMode)
    }

    override fun before(events: List<VFileEvent>) {
    }

    override fun beforeRootsChange(event: ModuleRootEvent) {
    }

    override fun rootsChanged(event: ModuleRootEvent) {
        if (myMuted.get()) return

        ApplicationManager.getApplication().invokeLater(Runnable {
            DumbService.getInstance(myProject!!).runWhenSmart() { updateProjectLibrary() }
        }, ModalityState.NON_MODAL, myProject!!.disposed)
    }

    @TestOnly
    public fun syncUpdateProjectLibrary(): Unit = updateProjectLibrary(true)

    /**
     * @param synchronously may be true only in tests.
     */
    private fun updateProjectLibrary(synchronously: Boolean = false) {
        val project = myProject
        if (project == null || project.isDisposed) return

        ApplicationManager.getApplication().assertReadAccessAllowed()

        for (module in ModuleManager.getInstance(project).modules) {
            if (!isModuleApplicable(module)) continue

            if (!isJsKotlinModule(module)) {
                findLibraryByName(module, LIBRARY_NAME)?.let { resetLibraries(module, ChangesToApply(), LIBRARY_NAME, synchronously) }
                continue
            }

            val clsRootUrls: MutableList<String> = arrayListOf()
            val srcRootUrls: MutableList<String> = arrayListOf()
            val rootFiles: MutableList<VirtualFile> = arrayListOf()

            ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary() { library ->
                if (KotlinJavaScriptLibraryDetectionUtil.isKotlinJavaScriptLibrary(library)) {
                    var addSources = false
                    for (clsRootFile in library.getFiles(OrderRootType.CLASSES)) {
                        val path = PathUtil.getLocalPath(clsRootFile)
                        assert(path != null) { "expected not-null path for ${clsRootFile.name}" }

                        val metadataList = KotlinJavascriptMetadataUtils.loadMetadata(path!!)
                        if (metadataList.filter { !it.TEMP_isAbiVersionCompatible }.isNotEmpty()) continue

                        VfsUtil.findFileByIoFile(File(path), true)?.let { rootFiles.add(it) }
                        val classRoot = KotlinJavaScriptMetaFileSystem.getInstance().refreshAndFindFileByPath("$path!/")
                        classRoot?.let {
                            clsRootUrls.add(it.url)
                            addSources = true
                        }
                    }
                    if (addSources) {
                        srcRootUrls.addAll(library.getFiles(OrderRootType.SOURCES).map { it.url })
                    }
                }
                true
            }

            val filesToRefresh = update(rootFiles, addToMapIfAbsent = true)

            val changesToApply = ChangesToApply(clsRootUrls, srcRootUrls, filesToRefresh)

            resetLibraries(module, changesToApply, LIBRARY_NAME, synchronously)
        }
    }

    private fun resetLibraries(module: Module, changesToApply: ChangesToApply, libraryName: String, synchronously: Boolean) =
            applyChange(module, changesToApply, synchronously, libraryName)

    private fun applyChange(module: Module, changesToApply: ChangesToApply, synchronously: Boolean, libraryName: String) {
        if (synchronously) {
            //for test only
            val application = ApplicationManager.getApplication()
            if (!application.isUnitTestMode) {
                throw IllegalStateException("Synchronous library update may be done only in test mode")
            }

            val token = application.acquireWriteActionLock(KotlinJavaScriptLibraryManager::class.java)
            try {
                applyChangeImpl(module, changesToApply, libraryName)
            }
            finally {
                token.finish()
            }
        }
        else {
            val commit = Runnable { applyChangeImpl(module, changesToApply, libraryName) }
            val commitInWriteAction = Runnable { ApplicationManager.getApplication().runWriteAction(commit) }
            ApplicationManager.getApplication().invokeLater(commitInWriteAction, myProject!!.disposed)
        }
    }

    private fun update(changedFiles: List<VirtualFile>, addToMapIfAbsent: Boolean): List<VirtualFile> {
        if (changedFiles.isEmpty()) return emptyList()

        val files = arrayListOf<VirtualFile>()
        synchronized(libraryFileStampMap) {
            changedFiles.forEach { file ->
                PathUtil.getLocalPath(file)?.let { path ->
                    val timeStamp = libraryFileStampMap.get(path)
                    if (addToMapIfAbsent && timeStamp != file.timeStamp ||
                        !addToMapIfAbsent && timeStamp != null) {
                        libraryFileStampMap[path] = file.timeStamp
                        files.add(file)
                    }
                }
            }
        }

        return files
    }

    private fun refreshFiles(files: List<VirtualFile>, synchronously: Boolean) {
        val events = files.filter { !it.isDirectory && it.isValid }.map { MyVFileContentChangeEvent(it) }
        if (events.isEmpty()) return

        val commitInWriteAction = Runnable {
            ApplicationManager.getApplication().runWriteAction() {
                val publisher = ApplicationManager.getApplication().messageBus.syncPublisher(VirtualFileManager.VFS_CHANGES)
                publisher.before(events)
                publisher.after(events)
            }
        }

        if (synchronously) {
            commitInWriteAction.run()
        }
        else {
            ApplicationManager.getApplication().invokeLater(commitInWriteAction, myProject!!.disposed)
        }
    }

    @Synchronized
    private fun applyChangeImpl(module: Module, changesToApply: ChangesToApply, libraryName: String) {
        if (module.isDisposed) return

        refreshFiles(changesToApply.filesToRefresh, synchronously = true)

        val model = ModuleRootManager.getInstance(module).modifiableModel
        val libraryTableModel = model.moduleLibraryTable.modifiableModel

        var library = findLibraryByName(libraryTableModel, libraryName)

        if (library == null) {
            if (changesToApply.clsUrlsToAdd.isEmpty()) {
                model.dispose()
                return
            }

            library = libraryTableModel.createLibrary(libraryName)!!
        }

        if (changesToApply.clsUrlsToAdd.isEmpty()) {
            libraryTableModel.removeLibrary(library)
            commitLibraries(null, libraryTableModel, model)
            return
        }

        val libraryModel = library.modifiableModel

        val existingClsUrls = library.getUrls(OrderRootType.CLASSES).toSet()
        val existingSrcUrls = library.getUrls(OrderRootType.SOURCES).toSet()

        if (existingClsUrls == changesToApply.clsUrlsToAdd.toSet() && existingSrcUrls == changesToApply.srcUrlsToAdd.toSet()) {
            model.dispose()
            Disposer.dispose(libraryModel)
            return
        }

        existingClsUrls.forEach { libraryModel.removeRoot(it, OrderRootType.CLASSES) }
        existingSrcUrls.forEach { libraryModel.removeRoot(it, OrderRootType.SOURCES) }

        changesToApply.clsUrlsToAdd.forEach { libraryModel.addRoot(it, OrderRootType.CLASSES) }
        changesToApply.srcUrlsToAdd.forEach { libraryModel.addRoot(it, OrderRootType.SOURCES) }

        commitLibraries(libraryModel, libraryTableModel, model)
    }

    private fun commitLibraries(libraryModel: Library.ModifiableModel?, tableModel: LibraryTable.ModifiableModel, model: ModifiableRootModel) {
        try {
            myMuted.set(true)
            libraryModel?.commit()
            tableModel.commit()
            model.commit()
        }
        finally {
            myMuted.set(false)
        }
    }

    private fun isModuleApplicable(module: Module) = ModuleTypeId.JAVA_MODULE == ModuleType.get(module).id

    private fun findLibraryByName(libraryTableModel: LibraryTable.ModifiableModel, libraryName: String) =
            libraryTableModel.libraries.firstOrNull { libraryName == it.name }

    private fun findLibraryByName(module: Module, libraryName: String) =
            OrderEntryUtil.findLibraryOrderEntry(ModuleRootManager.getInstance(module), libraryName)?.library

    private class ChangesToApply(val clsUrlsToAdd: List<String> = listOf(), val srcUrlsToAdd: List<String> = listOf(), val filesToRefresh: List<VirtualFile> = listOf())

    private class MyVFileContentChangeEvent(
            file: VirtualFile
    ) : VFileContentChangeEvent(
            FileContentUtilCore.FORCE_RELOAD_REQUESTOR,
            file,
            file.modificationStamp,
            -1, /* oldModificationStamp */
            false /* isFromRefresh */
    ) {
        override fun getPath(): String = PathUtil.getLocalPath(file.path + KotlinJavaScriptMetaFileSystem.ARCHIVE_SUFFIX)
    }

    companion object {

        public val LIBRARY_NAME: String = "<Kotlin JavaScript library>"

        @JvmStatic
        public fun getInstance(project: Project): KotlinJavaScriptLibraryManager =
                project.getComponent(KotlinJavaScriptLibraryManager::class.java)!!
    }
}
