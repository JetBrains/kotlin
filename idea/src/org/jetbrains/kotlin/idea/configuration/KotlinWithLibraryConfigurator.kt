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
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.annotations.Contract
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.framework.ui.CreateLibraryDialogWithModules
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.idea.versions.findKotlinRuntimeLibrary
import java.io.File
import java.util.*

abstract class KotlinWithLibraryConfigurator internal constructor() : KotlinProjectConfigurator {

    protected abstract val libraryName: String

    protected abstract val messageForOverrideDialog: String

    protected abstract val dialogTitle: String

    protected abstract val libraryCaption: String

    open val libraryType: LibraryType<DummyLibraryProperties>? = null

    protected  val libraryKind: PersistentLibraryKind<*>? = libraryType?.kind

    override fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus {
        val module = moduleSourceRootGroup.baseModule
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
            getCanBeConfiguredModules(project, this)
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

        val collector = createConfigureKotlinNotificationCollector(project)
        for (module in modulesToConfigure) {
            configureModuleWithLibrary(module, defaultPathToJar, copyLibraryIntoPath, collector)
        }

        configureKotlinSettings(modulesToConfigure)

        collector.showNotification()
    }

    @Suppress("unused") // Please do not delete this function (used in ProcessingKt plugin)
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
        val classesPath =  getPathToCopyFileTo(module.project, OrderRootType.CLASSES, defaultPath, pathFromDialog)
        val sourcesPath =  getPathToCopyFileTo(module.project, OrderRootType.SOURCES, defaultPath, pathFromDialog)
        configureModuleWithLibrary(module, classesPath, sourcesPath, collector, useBundled = pathFromDialog == null)
    }

     fun configureModuleWithLibrary(
            module: Module,
            classesPath: String,
            sourcesPath: String,
            collector: NotificationMessageCollector,
            forceJarState: FileState? = null,
            useBundled: Boolean = false
    ) {
        val project = module.project

        val library = getKotlinLibrary(module)
                      ?: getKotlinLibrary(project)
                      ?: createNewLibrary(project, collector)

        val sdk = ModuleRootManager.getInstance(module).sdk
        val model = library.modifiableModel

        for (descriptor in getLibraryJarDescriptors(sdk)) {
            val dirToCopyJar = if (descriptor.orderRootType == OrderRootType.SOURCES)
                sourcesPath
            else
                classesPath

            val runtimeState = forceJarState ?: getJarState(project,
                                                            File(dirToCopyJar, descriptor.jarName),
                                                            descriptor.orderRootType, useBundled)

            configureLibraryJar(model, runtimeState, dirToCopyJar, descriptor, collector)
        }
        ApplicationManager.getApplication().runWriteAction { model.commit() }

        addLibraryToModuleIfNeeded(module, library, collector)
    }

    fun configureLibraryJar(
            library: Library.ModifiableModel,
            jarState: FileState,
            dirToCopyJarTo: String,
            libraryJarDescriptor: LibraryJarDescriptor,
            collector: NotificationMessageCollector
    ) {
        val jarFile = if (jarState == KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY)
            libraryJarDescriptor.getPathInPlugin()
        else
            File(dirToCopyJarTo, libraryJarDescriptor.jarName)

        if (jarState == KotlinWithLibraryConfigurator.FileState.COPY) {
            copyFileToDir(libraryJarDescriptor.getPathInPlugin(), dirToCopyJarTo, collector)
        }

        val jarVFile = LocalFileSystem.getInstance().findFileByIoFile(jarFile)
        if (jarVFile == null) {
            collector.addMessage("Can't find library JAR file " + jarFile)
            return
        }
        val jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(jarVFile)
        if (jarRoot == null) {
            collector.addMessage("Couldn't configure library; JAR file $jarVFile may be corrupted")
            return
        }

        if (jarRoot !in library.getFiles(libraryJarDescriptor.orderRootType)) {
            library.addRoot(jarRoot, libraryJarDescriptor.orderRootType)

            collector.addMessage("Added $jarFile to library configuration")
        }
    }

    fun getKotlinLibrary(project: Project): Library? {
        return LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraries.firstOrNull(this::isKotlinLibrary) ?:
               LibraryTablesRegistrar.getInstance().libraryTable.libraries.firstOrNull(this::isKotlinLibrary)
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

    fun addLibraryToModuleIfNeeded(module: Module, library: Library, collector: NotificationMessageCollector) {
        val expectedDependencyScope = getDependencyScope(module)
        val kotlinLibrary = getKotlinLibrary(module)
        if (kotlinLibrary == null) {
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

    fun createNewLibrary(
            project: Project,
            collector: NotificationMessageCollector
    ): Library {
        val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        val library = runWriteAction {
            table.modifiableModel.run {
                val library = createLibrary(libraryName, libraryKind)
                commit()
                library
            }
        }

        collector.addMessage(library.name!! + " library was created")
        return library!!
    }

    private fun isProjectLibraryPresent(project: Project): Boolean {
        val library = getKotlinLibrary(project)
        return library != null && library.getUrls(OrderRootType.CLASSES).size > 0
    }

    protected abstract val libraryMatcher: (Library) -> Boolean

    fun getKotlinLibrary(module: Module): Library? {
        return findKotlinRuntimeLibrary(module, this::isKotlinLibrary)
    }

    private fun isKotlinLibrary(library: Library) = library.name == libraryName || libraryMatcher(library)

    protected fun needToChooseJarPath(project: Project): Boolean {
        val defaultPath = getDefaultPathToJarFile(project)
        return !isProjectLibraryPresent(project) &&
               !File(defaultPath, getLibraryJarDescriptors(null).first().jarName).exists()
    }

    open fun getDefaultPathToJarFile(project: Project): String {
        return FileUIUtils.createRelativePath(project, project.baseDir, DEFAULT_LIBRARY_DIR)
    }

    enum class FileState {
        EXISTS,
        COPY,
        DO_NOT_COPY
    }

    protected fun getJarState(
            project: Project,
            targetFile: File,
            jarType: OrderRootType,
            useBundled: Boolean
    ): FileState = when {
        targetFile.exists() -> FileState.EXISTS
        getPathFromLibrary(project, jarType) != null -> FileState.COPY
        useBundled -> FileState.DO_NOT_COPY
        else -> FileState.COPY
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

    abstract fun getLibraryJarDescriptors(sdk: Sdk?): List<LibraryJarDescriptor>

    protected open fun configureKotlinSettings(modules: List<Module>) {
    }

    companion object {
        val DEFAULT_LIBRARY_DIR = "lib"

        protected fun isApplicable(module: Module): Boolean {
            return !KotlinPluginUtil.isAndroidGradleModule(module) &&
                   !KotlinPluginUtil.isMavenModule(module) &&
                   !KotlinPluginUtil.isGradleModule(module)
        }

        fun getPathFromLibrary(library: Library?, type: OrderRootType): String? {
            if (library == null) return null

            val libraryFiles = library.getUrls(type)
            return getPathFromLibraryUrls(libraryFiles)
        }

        fun getPathFromLibraryUrls(libraryFiles: Array<String>): String? {
            if (libraryFiles.size < 1) return null

            val pathToJarInLib = VfsUtilCore.urlToPath(libraryFiles[0])
            val parentDir = VfsUtil.getParentDir(VfsUtil.getParentDir(pathToJarInLib)) ?: return null

            val parentDirFile = File(parentDir)
            if (!parentDirFile.exists() && !parentDirFile.mkdirs()) {
                return null
            }
            return parentDir
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
