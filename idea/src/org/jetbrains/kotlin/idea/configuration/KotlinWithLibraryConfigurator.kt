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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.Processor
import org.jetbrains.annotations.Contract
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.framework.ui.CreateLibraryDialogWithModules
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils
import java.io.File
import java.util.*

abstract class KotlinWithLibraryConfigurator internal constructor() : KotlinProjectConfigurator {

    protected abstract val libraryName: String

    protected abstract val messageForOverrideDialog: String

    protected abstract val dialogTitle: String

    protected abstract val libraryCaption: String

    abstract val existingJarFiles: RuntimeLibraryFiles

    protected abstract fun getOldSourceRootUrl(library: Library): String?

    override fun getStatus(module: Module): ConfigureKotlinStatus {
        if (!isApplicable(module)) {
            return ConfigureKotlinStatus.NON_APPLICABLE
        }
        if (isConfigured(module)) {
            return ConfigureKotlinStatus.CONFIGURED
        }
        return ConfigureKotlinStatus.CAN_BE_CONFIGURED
    }

    abstract fun isConfigured(module: Module): Boolean

    @JvmSuppressWildcards
    override fun configure(project: Project, excludeModules: Collection<Module>) {
        val defaultPathToJar = getDefaultPathToJarFile(project)
        val showPathToJarPanel = needToChooseJarPath(project)

        var nonConfiguredModules = if (!ApplicationManager.getApplication().isUnitTestMode)
            getNonConfiguredModules(project, this)
        else
            Arrays.asList(*ModuleManager.getInstance(project).modules)
        nonConfiguredModules -= excludeModules

        var modulesToConfigure = nonConfiguredModules
        var copyLibraryIntoPath: String? = null

        if (nonConfiguredModules.size > 1 || showPathToJarPanel) {
            val dialog = CreateLibraryDialogWithModules(
                    project, this, defaultPathToJar, showPathToJarPanel,
                    dialogTitle,
                    libraryCaption,
                    excludeModules)

            if (!ApplicationManager.getApplication().isUnitTestMode) {
                dialog.show()
                if (!dialog.isOK) return
            }
            else {
                dialog.close(0)
            }

            modulesToConfigure = dialog.modulesToConfigure
            copyLibraryIntoPath = dialog.copyIntoPath
        }

        val finalModulesToConfigure = modulesToConfigure
        val finalCopyLibraryIntoPath = copyLibraryIntoPath

        val collector = createConfigureKotlinNotificationCollector(project)
        for (module in finalModulesToConfigure) {
            configureModuleWithLibrary(module, defaultPathToJar, finalCopyLibraryIntoPath, collector)
        }

        collector.showNotification()
    }

    fun configureSilently(project: Project) {
        val defaultPathToJar = getDefaultPathToJarFile(project)
        val collector = createConfigureKotlinNotificationCollector(project)
        for (module in ModuleManager.getInstance(project).modules) {
            configureModuleWithLibrary(module, defaultPathToJar, null, collector)
        }
    }

    protected fun configureModuleWithLibrary(
            module: Module,
            defaultPath: String,
            pathFromDialog: String?,
            collector: NotificationMessageCollector
    ) {
        val project = module.project

        val files = existingJarFiles
        val libraryState = getLibraryState(project)
        val dirToCopyJar = getPathToCopyFileTo(project, OrderRootType.CLASSES, defaultPath, pathFromDialog)
        val runtimeState = getJarState(project, files.getRuntimeDestination(dirToCopyJar), OrderRootType.CLASSES, pathFromDialog == null)

        configureModuleWithLibraryClasses(module, libraryState, runtimeState, dirToCopyJar, collector)

        val library = getKotlinLibrary(project) ?: return

        val dirToCopySourcesJar = getPathToCopyFileTo(project, OrderRootType.SOURCES, defaultPath, pathFromDialog)
        val sourcesState = getJarState(project, files.getRuntimeSourcesDestination(dirToCopySourcesJar), OrderRootType.SOURCES,
                                       pathFromDialog == null)

        configureModuleWithLibrarySources(library, sourcesState, dirToCopySourcesJar, collector)
    }

    fun configureModuleWithLibraryClasses(
            module: Module,
            libraryState: LibraryState,
            jarState: FileState,
            dirToCopyJarTo: String,
            collector: NotificationMessageCollector
    ) {
        val project = module.project
        val files = existingJarFiles
        val runtimeJar = files.runtimeJar
        val reflectJar = files.reflectJar

        when (libraryState) {
            KotlinWithLibraryConfigurator.LibraryState.LIBRARY -> when (jarState) {
                KotlinWithLibraryConfigurator.FileState.EXISTS -> {
                }
                KotlinWithLibraryConfigurator.FileState.COPY -> {
                    copyFileToDir(runtimeJar, dirToCopyJarTo, collector)
                    if (reflectJar != null) {
                        copyFileToDir(reflectJar, dirToCopyJarTo, collector)
                    }
                }
                KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY -> {
                    throw IllegalStateException(
                            "Kotlin library exists, so path to copy should be hidden in configuration dialog and jar should be copied using path in library table")
                }
            }
            KotlinWithLibraryConfigurator.LibraryState.NON_CONFIGURED_LIBRARY -> when (jarState) {
                KotlinWithLibraryConfigurator.FileState.EXISTS -> {
                    addJarsToExistingLibrary(
                            project, files.getRuntimeDestination(dirToCopyJarTo), files.getReflectDestination(dirToCopyJarTo), collector
                    )
                }
                KotlinWithLibraryConfigurator.FileState.COPY -> {
                    val copiedRuntimeJar = copyFileToDir(runtimeJar, dirToCopyJarTo, collector)
                    val copiedReflectJar = copyFileToDir(reflectJar, dirToCopyJarTo, collector)
                    addJarsToExistingLibrary(project, copiedRuntimeJar!!, copiedReflectJar, collector)
                }
                KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY -> {
                    addJarsToExistingLibrary(project, runtimeJar, reflectJar, collector)
                }
            }
            KotlinWithLibraryConfigurator.LibraryState.NEW_LIBRARY -> when (jarState) {
                KotlinWithLibraryConfigurator.FileState.EXISTS -> {
                    addJarsToNewLibrary(
                            project, files.getRuntimeDestination(dirToCopyJarTo), files.getReflectDestination(dirToCopyJarTo), collector
                    )
                }
                KotlinWithLibraryConfigurator.FileState.COPY -> {
                    val copiedRuntimeJar = copyFileToDir(runtimeJar, dirToCopyJarTo, collector)
                    val copiedReflectJar = copyFileToDir(reflectJar, dirToCopyJarTo, collector)
                    addJarsToNewLibrary(project, copiedRuntimeJar!!, copiedReflectJar, collector)
                }
                KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY -> {
                    addJarsToNewLibrary(project, runtimeJar, reflectJar, collector)
                }
            }
        }

        addLibraryToModuleIfNeeded(module, collector)
    }

    protected fun configureModuleWithLibrarySources(
            library: Library,
            jarState: FileState,
            dirToCopyJarTo: String?,
            collector: NotificationMessageCollector
    ) {
        val files = existingJarFiles
        val runtimeSourcesJar = files.runtimeSourcesJar
        when (jarState) {
            KotlinWithLibraryConfigurator.FileState.EXISTS -> {
                if (dirToCopyJarTo != null) {
                    addSourcesToLibraryIfNeeded(library, files.getRuntimeSourcesDestination(dirToCopyJarTo), collector)
                }
            }
            KotlinWithLibraryConfigurator.FileState.COPY -> {
                assert(dirToCopyJarTo != null) { "Path to copy should be non-null" }
                val file = copyFileToDir(runtimeSourcesJar, dirToCopyJarTo!!, collector)
                addSourcesToLibraryIfNeeded(library, file!!, collector)
            }
            KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY -> {
                addSourcesToLibraryIfNeeded(library, runtimeSourcesJar, collector)
            }
        }
    }

    fun getKotlinLibrary(project: Project): Library? {
        val librariesContainer = LibrariesContainerFactory.createContainer(project)
        for (library in librariesContainer.getLibraries(LibrariesContainer.LibraryLevel.PROJECT)) {
            if (isKotlinLibrary(project, library)) {
                return library
            }
        }
        for (library in librariesContainer.getLibraries(LibrariesContainer.LibraryLevel.GLOBAL)) {
            if (isKotlinLibrary(project, library)) {
                return library
            }
        }
        return null
    }

    @Contract("!null, _, _ -> !null")
    fun copyFileToDir(file: File?, toDir: String, collector: NotificationMessageCollector): File? {
        if (file == null) return null

        val copy = FileUIUtils.copyWithOverwriteDialog(messageForOverrideDialog, toDir, file)
        if (copy != null) {
            collector.addMessage(file.name + " was copied to " + toDir)
        }
        return copy
    }

    protected fun getPathFromLibrary(project: Project, type: OrderRootType): String? {
        return getPathFromLibrary(getKotlinLibrary(project), type)
    }

    private fun addLibraryToModuleIfNeeded(module: Module, collector: NotificationMessageCollector) {
        val expectedDependencyScope = getDependencyScope(module)
        val kotlinLibrary = getKotlinLibrary(module)
        if (kotlinLibrary == null) {
            val library = getKotlinLibrary(module.project) ?: error("Kotlin project library should exists")

            ModuleRootModificationUtil.addDependency(module, library, expectedDependencyScope, false)
            collector.addMessage(library.name + " library was added to module " + module.name)
        }
        else {
            val libraryEntry = findLibraryOrderEntry(ModuleRootManager.getInstance(module).orderEntries, kotlinLibrary)
            if (libraryEntry != null) {
                val libraryDependencyScope = libraryEntry.scope
                if (expectedDependencyScope != libraryDependencyScope) {
                    libraryEntry.scope = expectedDependencyScope

                    collector.addMessage(
                            kotlinLibrary.name + " library scope has changed from " + libraryDependencyScope +
                            " to " + expectedDependencyScope + " for module " + module.name)
                }
            }
        }
    }

    private fun addJarsToExistingLibrary(project: Project, runtimeJar: File, reflectJar: File?, collector: NotificationMessageCollector) {
        val library = getKotlinLibrary(project) ?: error("Kotlin library should present, instead createNewLibrary should be invoked")

        val model = library.modifiableModel
        model.addRoot(VfsUtil.getUrlForLibraryRoot(runtimeJar), OrderRootType.CLASSES)
        if (reflectJar != null) {
            model.addRoot(VfsUtil.getUrlForLibraryRoot(reflectJar), OrderRootType.CLASSES)
        }

        ApplicationManager.getApplication().runWriteAction { model.commit() }

        collector.addMessage(library.name!! + " library was configured")
    }

    private fun addJarsToNewLibrary(
            project: Project,
            runtimeJar: File,
            reflectJar: File?,
            collector: NotificationMessageCollector
    ) {
        val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        val library = Ref<Library>()
        ApplicationManager.getApplication().runWriteAction {
            library.set(table.createLibrary(libraryName))
            val model = library.get().modifiableModel
            model.addRoot(VfsUtil.getUrlForLibraryRoot(runtimeJar), OrderRootType.CLASSES)
            if (reflectJar != null) {
                model.addRoot(VfsUtil.getUrlForLibraryRoot(reflectJar), OrderRootType.CLASSES)
            }
            model.commit()
        }

        collector.addMessage(library.get().name!! + " library was created")
    }

    private fun isProjectLibraryWithoutPathsPresent(project: Project): Boolean {
        val library = getKotlinLibrary(project)
        return library != null && library.getUrls(OrderRootType.CLASSES).size == 0
    }

    private fun isProjectLibraryPresent(project: Project): Boolean {
        val library = getKotlinLibrary(project)
        return library != null && library.getUrls(OrderRootType.CLASSES).size > 0
    }

    private fun getKotlinLibrary(module: Module): Library? {
        val result = Ref.create<Library>(null)
        OrderEnumerator.orderEntries(module).forEachLibrary(Processor<Library> { library ->
            if (isKotlinLibrary(module.project, library)) {
                result.set(library)
                return@Processor false
            }
            true
        })
        return result.get()
    }

    protected open fun isKotlinLibrary(project: Project, library: Library): Boolean {
        if (libraryName == library.name) {
            return true
        }

        val fileName = existingJarFiles.runtimeJar.name

        for (root in library.getFiles(OrderRootType.CLASSES)) {
            if (root.name == fileName) {
                return true
            }
        }

        return false
    }

    protected fun needToChooseJarPath(project: Project): Boolean {
        val defaultPath = getDefaultPathToJarFile(project)
        return !isProjectLibraryPresent(project) && !existingJarFiles.getRuntimeDestination(defaultPath).exists()
    }

    protected open fun getDefaultPathToJarFile(project: Project): String {
        return FileUIUtils.createRelativePath(project, project.baseDir, DEFAULT_LIBRARY_DIR)
    }

    protected fun showError(message: String) {
        Messages.showErrorDialog(message, messageForOverrideDialog)
    }

    enum class FileState {
        EXISTS,
        COPY,
        DO_NOT_COPY
    }

    enum class LibraryState {
        LIBRARY,
        NON_CONFIGURED_LIBRARY,
        NEW_LIBRARY
    }

    fun getLibraryState(project: Project): LibraryState {
        if (isProjectLibraryPresent(project)) {
            return LibraryState.LIBRARY
        }
        else if (isProjectLibraryWithoutPathsPresent(project)) {
            return LibraryState.NON_CONFIGURED_LIBRARY
        }
        return LibraryState.NEW_LIBRARY
    }

    protected fun getJarState(
            project: Project,
            targetFile: File,
            jarType: OrderRootType,
            useBundled: Boolean
    ): FileState {
        if (targetFile.exists()) {
            return FileState.EXISTS
        }
        else if (getPathFromLibrary(project, jarType) != null) {
            return FileState.COPY
        }
        else if (useBundled) {
            return FileState.DO_NOT_COPY
        }
        else {
            return FileState.COPY
        }
    }

    private fun getPathToCopyFileTo(
            project: Project,
            jarType: OrderRootType,
            defaultDir: String,
            pathFromDialog: String?
    ): String {
        if (pathFromDialog != null) {
            return pathFromDialog
        }
        val pathFromLibrary = getPathFromLibrary(project, jarType)
        if (pathFromLibrary != null) {
            return pathFromLibrary
        }
        return defaultDir
    }

    protected fun assertFileExists(file: File): File {
        if (!file.exists()) {
            showError("Couldn't find file: " + file.path)
        }
        return file
    }

    fun copySourcesToPathFromLibrary(library: Library, collector: NotificationMessageCollector) {
        val dirToJarFromLibrary = getPathFromLibrary(library, OrderRootType.SOURCES) ?: error("Directory to file from library should be non null")

        copyFileToDir(existingJarFiles.runtimeSourcesJar, dirToJarFromLibrary, collector)
    }

    fun changeOldSourcesPathIfNeeded(library: Library, collector: NotificationMessageCollector): Boolean {
        if (!removeOldSourcesRootIfNeeded(library, collector)) {
            return false
        }

        val parentDir = getPathFromLibrary(library, OrderRootType.CLASSES) ?: error("Parent dir for classes jar should exists for Kotlin library")

        return addSourcesToLibraryIfNeeded(library, existingJarFiles.getRuntimeSourcesDestination(parentDir), collector)
    }

    protected fun removeOldSourcesRootIfNeeded(library: Library, collector: NotificationMessageCollector): Boolean {
        val oldLibrarySourceRoot = getOldSourceRootUrl(library)

        val librarySourceRoots = library.getUrls(OrderRootType.SOURCES)
        for (sourceRoot in librarySourceRoots) {
            if (sourceRoot == oldLibrarySourceRoot) {
                val model = library.modifiableModel
                model.removeRoot(oldLibrarySourceRoot!!, OrderRootType.SOURCES)
                ApplicationManager.getApplication().runWriteAction { model.commit() }

                collector.addMessage("Source root '" + oldLibrarySourceRoot + "' was removed for " + library.name + " library")
                return true
            }
        }
        return false
    }

    companion object {
        val DEFAULT_LIBRARY_DIR = "lib"

        protected fun isApplicable(module: Module): Boolean {
            return !KotlinPluginUtil.isAndroidGradleModule(module) &&
                   !KotlinPluginUtil.isMavenModule(module) &&
                   !KotlinPluginUtil.isGradleModule(module)
        }

        protected fun getPathFromLibrary(library: Library?, type: OrderRootType): String? {
            if (library == null) return null

            val libraryFiles = library.getUrls(type)
            if (libraryFiles.size < 1) return null

            val pathToJarInLib = VfsUtilCore.urlToPath(libraryFiles[0])
            val parentDir = VfsUtil.getParentDir(VfsUtil.getParentDir(pathToJarInLib)) ?: return null

            val parentDirFile = File(parentDir)
            if (!parentDirFile.exists() && !parentDirFile.mkdirs()) {
                return null
            }
            return parentDir
        }

        protected fun addSourcesToLibraryIfNeeded(
                library: Library,
                file: File,
                collector: NotificationMessageCollector
        ): Boolean {
            val librarySourceRoots = library.getUrls(OrderRootType.SOURCES)
            val librarySourceRoot = VfsUtil.getUrlForLibraryRoot(file)
            for (sourceRoot in librarySourceRoots) {
                if (sourceRoot == librarySourceRoot) return false
            }

            val model = library.modifiableModel
            model.addRoot(librarySourceRoot, OrderRootType.SOURCES)

            ApplicationManager.getApplication().runWriteAction { model.commit() }

            collector.addMessage("Source root '" + librarySourceRoot + "' was added to " + library.name + " library")
            return true
        }

        private fun findLibraryOrderEntry(orderEntries: Array<OrderEntry>, library: Library): LibraryOrderEntry? {
            for (orderEntry in orderEntries) {
                if (orderEntry is LibraryOrderEntry && library == orderEntry.library) {
                    return orderEntry
                }
            }

            return null
        }

        private fun getDependencyScope(module: Module): DependencyScope {
            if (hasKotlinFilesOnlyInTests(module)) {
                return DependencyScope.TEST
            }
            return DependencyScope.COMPILE
        }
    }
}
