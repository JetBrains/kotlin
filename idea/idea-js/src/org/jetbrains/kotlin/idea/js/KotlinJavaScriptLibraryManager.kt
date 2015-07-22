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
import com.intellij.util.PathUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.framework.KotlinJavaScriptLibraryDetectionUtil
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil.isJsKotlinModule
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.platform.platformStatic

public class KotlinJavaScriptLibraryManager private constructor(private var myProject: Project?) : ProjectComponent, ModuleRootListener {
    private val myMuted = AtomicBoolean(false)

    override fun getComponentName(): String = "KotlinJavascriptLibraryManager"

    override fun projectOpened() {
        val project = myProject!!
        project.getMessageBus().connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, this)
        DumbService.getInstance(project).smartInvokeLater() { updateProjectLibrary() }
    }

    override fun projectClosed() {
    }

    override fun initComponent() {
    }

    override fun disposeComponent() {
        myProject = null
    }

    override fun beforeRootsChange(event: ModuleRootEvent) {
    }

    override fun rootsChanged(event: ModuleRootEvent) {
        if (myMuted.get()) return

        ApplicationManager.getApplication().invokeLater({
                DumbService.getInstance(myProject!!).runWhenSmart() { updateProjectLibrary() }
        }, ModalityState.NON_MODAL, myProject!!.getDisposed())
    }

    TestOnly
    public fun syncUpdateProjectLibrary(): Unit = updateProjectLibrary(true)

    /**
     * @param synchronously may be true only in tests.
     */
    private fun updateProjectLibrary(synchronously: Boolean = false) {
        val project = myProject
        if (project == null || project.isDisposed()) return

        ApplicationManager.getApplication().assertReadAccessAllowed()

        for (module in ModuleManager.getInstance(project).getModules()) {
            if (!isModuleApplicable(module)) continue

            if (!isJsKotlinModule(module)) {
                findLibraryByName(module, LIBRARY_NAME)?.let { resetLibraries(module, ChangesToApply(), LIBRARY_NAME, synchronously) }
                continue
            }

            val clsRootUrls: MutableList<String> = arrayListOf()
            val srcRootUrls: MutableList<String> = arrayListOf()

            ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary() { library ->
                    if (KotlinJavaScriptLibraryDetectionUtil.isKotlinJavaScriptLibrary(library)) {
                        var addSources = false
                        for (clsRootFile in library.getFiles(OrderRootType.CLASSES)) {
                            val path = PathUtil.getLocalPath(clsRootFile)
                            assert(path != null, "expected not-null path for ${clsRootFile.getName()}")

                            val metadataList = KotlinJavascriptMetadataUtils.loadMetadata(path!!)
                            if (metadataList.filter { !it.isAbiVersionCompatible }.isNotEmpty()) continue

                            val classRoot = KotlinJavaScriptMetaFileSystem.getInstance().refreshAndFindFileByPath(path + "!/")
                            classRoot?.let {
                                clsRootUrls.add(it.getUrl())
                                addSources = true
                            }
                        }
                        if (addSources) {
                            srcRootUrls.addAll(library.getFiles(OrderRootType.SOURCES).map { it.getUrl() })
                        }
                    }
                    true
                }

            val changesToApply = ChangesToApply(clsRootUrls, srcRootUrls)

            resetLibraries(module, changesToApply, LIBRARY_NAME, synchronously)
        }
    }

    private fun resetLibraries(module: Module, changesToApply: ChangesToApply, libraryName: String, synchronously: Boolean) =
        applyChange(module, changesToApply, synchronously, libraryName)

    private fun applyChange(module: Module, changesToApply: ChangesToApply, synchronously: Boolean, libraryName: String) {
        if (synchronously) {
            //for test only
            val application = ApplicationManager.getApplication()
            if (!application.isUnitTestMode()) {
                throw IllegalStateException("Synchronous library update may be done only in test mode")
            }

            val token = application.acquireWriteActionLock(javaClass<KotlinJavaScriptLibraryManager>())
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
            ApplicationManager.getApplication().invokeLater(commitInWriteAction, myProject!!.getDisposed())
        }
    }

    synchronized private fun applyChangeImpl(module: Module, changesToApply: ChangesToApply, libraryName: String) {
        if (module.isDisposed()) return

        val model = ModuleRootManager.getInstance(module).getModifiableModel()
        val libraryTableModel = model.getModuleLibraryTable().getModifiableModel()

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

        val libraryModel = library.getModifiableModel()

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

    private fun isModuleApplicable(module: Module) = ModuleTypeId.JAVA_MODULE == ModuleType.get(module).getId()

    private fun findLibraryByName(libraryTableModel: LibraryTable.ModifiableModel, libraryName: String) =
            libraryTableModel.getLibraries().firstOrNull { libraryName == it.getName() }

    private fun findLibraryByName(module: Module, libraryName: String) =
            OrderEntryUtil.findLibraryOrderEntry(ModuleRootManager.getInstance(module), libraryName)?.getLibrary()

    private class ChangesToApply(val clsUrlsToAdd: List<String> = listOf(), val srcUrlsToAdd: List<String> = listOf())

    companion object {

        public val LIBRARY_NAME: String = "<Kotlin JavaScript library>"

        platformStatic
        public fun getInstance(project: Project): KotlinJavaScriptLibraryManager =
            project.getComponent(javaClass<KotlinJavaScriptLibraryManager>())!!
    }
}
