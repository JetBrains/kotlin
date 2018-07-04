/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches

import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.PerModulePackageCacheService.Companion.FULL_DROP_THRESHOLD
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfoByVirtualFile
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.idea.util.getSourceRoot
import org.jetbrains.kotlin.idea.util.sourceRoot
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class KotlinPackageContentModificationListener(private val project: Project) {
    init {
        val connection = project.messageBus.connect()

        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun before(events: MutableList<out VFileEvent>) = onEvents(events)
            override fun after(events: List<VFileEvent>) = onEvents(events)

            private fun isRelevant(it: VFileEvent): Boolean =
                it is VFileMoveEvent || it is VFileCreateEvent || it is VFileCopyEvent || it is VFileDeleteEvent

            fun onEvents(events: List<VFileEvent>) {
                val service = PerModulePackageCacheService.getInstance(project)
                if (events.size >= FULL_DROP_THRESHOLD) {
                    service.onTooComplexChange()
                } else {
                    events
                        .asSequence()
                        .filter { it.isValid }
                        .filter { it.file != null }
                        .filter(::isRelevant)
                        .filter {
                            val vFile = it.file!!
                            vFile.isDirectory || FileTypeRegistry.getInstance().getFileTypeByFileName(vFile.name) == KotlinFileType.INSTANCE
                        }
                        .forEach { event -> service.notifyPackageChange(event) }
                }
            }
        })

        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent?) {
                PerModulePackageCacheService.getInstance(project).onTooComplexChange()
            }
        })
    }
}

class KotlinPackageStatementPsiTreeChangePreprocessor(private val project: Project) : PsiTreeChangePreprocessor {
    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        val file = event.file as? KtFile ?: return

        when (event.code) {
            PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED -> {
                val child = event.child ?: return
                if (child.getParentOfType<KtPackageDirective>(false) != null)
                    ServiceManager.getService(project, PerModulePackageCacheService::class.java).notifyPackageChange(file)
            }
            PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED -> {
                val parent = event.parent ?: return
                if (parent.getChildrenOfType<KtPackageDirective>().any())
                    ServiceManager.getService(project, PerModulePackageCacheService::class.java).notifyPackageChange(file)
            }
            else -> {
            }
        }
    }
}

private typealias ImplicitPackageData = MutableMap<FqName, MutableList<VirtualFile>>

class ImplicitPackagePrefixCache(private val project: Project) {
    private val implicitPackageCache = ConcurrentHashMap<VirtualFile, ImplicitPackageData>()

    fun getPrefix(sourceRoot: VirtualFile): FqName {
        val implicitPackageMap = implicitPackageCache.getOrPut(sourceRoot) { analyzeImplicitPackagePrefixes(sourceRoot) }
        return implicitPackageMap.keys.singleOrNull() ?: FqName.ROOT
    }

    internal fun clear() {
        implicitPackageCache.clear()
    }

    private fun analyzeImplicitPackagePrefixes(sourceRoot: VirtualFile): MutableMap<FqName, MutableList<VirtualFile>> {
        val result = mutableMapOf<FqName, MutableList<VirtualFile>>()
        val ktFiles = sourceRoot.children.filter { it.fileType == KotlinFileType.INSTANCE }
        for (ktFile in ktFiles) {
            result.addFile(ktFile)
        }
        return result
    }

    private fun ImplicitPackageData.addFile(ktFile: VirtualFile) {
        synchronized(this) {
            val psiFile = PsiManager.getInstance(project).findFile(ktFile) as? KtFile ?: return
            addPsiFile(psiFile, ktFile)
        }
    }

    private fun ImplicitPackageData.addPsiFile(
        psiFile: KtFile,
        ktFile: VirtualFile
    ) = getOrPut(psiFile.packageFqName) { mutableListOf() }.add(ktFile)

    private fun ImplicitPackageData.removeFile(file: VirtualFile) {
        synchronized(this) {
            for ((key, value) in this) {
                if (value.remove(file)) {
                    if (value.isEmpty()) remove(key)
                    break
                }
            }
        }
    }

    private fun ImplicitPackageData.updateFile(file: KtFile) {
        synchronized(this) {
            removeFile(file.virtualFile)
            addPsiFile(file, file.virtualFile)
        }
    }

    internal fun update(event: VFileEvent) {
        when (event) {
            is VFileCreateEvent -> checkNewFileInSourceRoot(event.file)
            is VFileDeleteEvent -> checkDeletedFileInSourceRoot(event.file)
            is VFileCopyEvent -> {
                val newParent = event.newParent
                if (newParent.isValid) {
                    checkNewFileInSourceRoot(newParent.findChild(event.newChildName))
                }
            }
            is VFileMoveEvent -> {
                checkNewFileInSourceRoot(event.file)
                if (event.oldParent.getSourceRoot(project) == event.oldParent) {
                    implicitPackageCache[event.oldParent]?.removeFile(event.file)
                }
            }
        }
    }

    private fun checkNewFileInSourceRoot(file: VirtualFile?) {
        if (file == null) return
        if (file.getSourceRoot(project) == file.parent) {
            implicitPackageCache[file.parent]?.addFile(file)
        }
    }

    private fun checkDeletedFileInSourceRoot(file: VirtualFile?) {
        val directory = file?.parent
        if (directory == null || !directory.isValid) return
        if (directory.getSourceRoot(project) == directory) {
            implicitPackageCache[directory]?.removeFile(file)
        }
    }

    internal fun update(ktFile: KtFile) {
        val parent = ktFile.virtualFile.parent
        if (ktFile.sourceRoot == parent) {
            implicitPackageCache[parent]?.updateFile(ktFile)
        }
    }
}

class PerModulePackageCacheService(private val project: Project) {

    /*
     * Disposal of entries handled by Module child Disposable registered in packageExists
     * Actually an StrongMap<Module, SoftMap<ModuleSourceInfo, SoftMap<FqName, Boolean>>>
     */
    private val cache = ConcurrentHashMap<Module, ConcurrentMap<ModuleSourceInfo, ConcurrentMap<FqName, Boolean>>>()
    private val implicitPackagePrefixCache = ImplicitPackagePrefixCache(project)

    private val pendingVFileChanges: MutableSet<VFileEvent> = mutableSetOf()
    private val pendingKtFileChanges: MutableSet<KtFile> = mutableSetOf()

    private val projectScope = GlobalSearchScope.projectScope(project)

    internal fun onTooComplexChange(): Unit = synchronized(this) {
        pendingVFileChanges.clear()
        pendingKtFileChanges.clear()
        cache.clear()
        implicitPackagePrefixCache.clear()
    }

    internal fun notifyPackageChange(file: VFileEvent): Unit = synchronized(this) {
        pendingVFileChanges += file
    }

    internal fun notifyPackageChange(file: KtFile): Unit = synchronized(this) {
        pendingKtFileChanges += file
    }

    private fun invalidateCacheForModuleSourceInfo(moduleSourceInfo: ModuleSourceInfo) {
        val perSourceInfoData = cache[moduleSourceInfo.module] ?: return
        val dataForSourceInfo = perSourceInfoData[moduleSourceInfo] ?: return
        dataForSourceInfo.clear()
    }

    private fun checkPendingChanges() = synchronized(this) {
        if (pendingVFileChanges.size + pendingKtFileChanges.size >= FULL_DROP_THRESHOLD) {
            onTooComplexChange()
        } else {
            pendingVFileChanges.processPending { event ->
                val vfile = event.file ?: return@processPending
                // When VirtualFile !isValid (deleted for example), it impossible to use getModuleInfoByVirtualFile
                // For directory we must check both is it in some sourceRoot, and is it contains some sourceRoot
                if (vfile.isDirectory || !vfile.isValid) {
                    for ((module, data) in cache) {
                        val sourceRootUrls = module.rootManager.sourceRootUrls
                        if (sourceRootUrls.any { url ->
                                vfile.containedInOrContains(url)
                            }) {
                            data.clear()
                        }
                    }
                } else {
                    (getModuleInfoByVirtualFile(project, vfile) as? ModuleSourceInfo)?.let {
                        invalidateCacheForModuleSourceInfo(it)
                    }
                }

                implicitPackagePrefixCache.update(event)
            }

            pendingKtFileChanges.processPending { file ->
                if (file.virtualFile != null && file.virtualFile !in projectScope) {
                    return@processPending
                }
                (file.getNullableModuleInfo() as? ModuleSourceInfo)?.let { invalidateCacheForModuleSourceInfo(it) }
                implicitPackagePrefixCache.update(file)
            }
        }
    }

    private inline fun <T> MutableCollection<T>.processPending(crossinline body: (T) -> Unit) {
        this.removeIf { value ->
            try {
                body(value)
            } catch (pce: ProcessCanceledException) {
                throw pce
            } catch (exc: Exception) {
                // Log and proceed. Otherwise pending object processing won't be cleared and exception will be thrown forever.
                LOG.error(exc)
            }

            return@removeIf true
        }
    }

    private fun VirtualFile.containedInOrContains(root: String) =
        (VfsUtilCore.isEqualOrAncestor(url, root)
                || isDirectory && VfsUtilCore.isEqualOrAncestor(root, url))


    fun packageExists(packageFqName: FqName, moduleInfo: ModuleSourceInfo): Boolean {
        val module = moduleInfo.module
        checkPendingChanges()

        val perSourceInfoCache = cache.getOrPut(module) {
            Disposer.register(module, Disposable { cache.remove(module) })
            ContainerUtil.createConcurrentSoftMap()
        }
        val cacheForCurrentModuleInfo = perSourceInfoCache.getOrPut(moduleInfo) {
            ContainerUtil.createConcurrentSoftMap()
        }
        return cacheForCurrentModuleInfo.getOrPut(packageFqName) {
            PackageIndexUtil.packageExists(packageFqName, moduleInfo.contentScope(), project)
        }
    }

    fun getImplicitPackagePrefix(sourceRoot: VirtualFile): FqName {
        checkPendingChanges()
        return implicitPackagePrefixCache.getPrefix(sourceRoot)
    }

    companion object {
        const val FULL_DROP_THRESHOLD = 1000
        private val LOG = Logger.getInstance(this::class.java)

        fun getInstance(project: Project): PerModulePackageCacheService =
            ServiceManager.getService(project, PerModulePackageCacheService::class.java)
    }
}