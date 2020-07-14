package org.jetbrains.konan.resolve

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesPackListener
import gnu.trove.THashSet
import gnu.trove.TObjectHash
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.konan.resolve.symbols.KtDependencyMarker
import org.jetbrains.konan.resolve.translation.KtFileTranslator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.project.getModuleInfoByVirtualFile
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import java.util.*

@Service
class KtDependencyGraph(val project: Project) : Disposable {
    private val dependent: MultiMap<VirtualFile, VirtualFile> = MultiMap.createSet()

    @Volatile
    private var invalidationWorker: CancellablePromise<Void>? = null

    @Volatile
    private var verificationWorker: CancellablePromise<Void>? = null

    private val pendingVerificationFiles: Queue<VirtualFile> = ArrayDeque()
    private val changedFiles: Queue<VirtualFile> = ArrayDeque()
    private val createdFileParents: Queue<VirtualFile> = ArrayDeque()

    init {
        VirtualFileManager.getInstance().addAsyncFileListener(AsyncFileListener { events ->
            val changedFiles = THashSet<VirtualFile>()
            val createdFileParents = THashSet<VirtualFile>()

            for (event in events) {
                ProgressManager.checkCanceled()
                when (event) {
                    is VFileMoveEvent -> if (isRelevant(event.file)) {
                        changedFiles += event.file
                        createdFileParents += event.newParent
                    }
                    is VFileCreateEvent -> if (isRelevant(event.childName)) createdFileParents += event.parent
                    is VFilePropertyChangeEvent -> when {
                        isSymlinkChange(event) -> {
                            if (isRelevant(event.file)) changedFiles += event.file
                        }
                        isFileNameChange(event) -> {
                            if (isRelevant(event.file)) changedFiles += event.file
                            if (isRelevant(event.newValue.toString())) createdFileParents += event.file.parent
                        }
                    }
                    is VFileDeleteEvent -> if (isRelevant(event.file)) changedFiles += event.file
                    is VFileContentChangeEvent -> if (isRelevant(event.file)) changedFiles += event.file
                    is VFileCopyEvent -> if (isRelevant(event.newChildName)) createdFileParents += event.newParent
                }
            }

            if (changedFiles.isEmpty && createdFileParents.isEmpty) return@AsyncFileListener null
            object : AsyncFileListener.ChangeApplier {
                override fun afterVfsChange() {
                    scheduleInvalidation(changedFiles, createdFileParents)
                }
            }
        }, this)
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

    private inner class InvalidationWorker : Runnable {
        private val dirtyFiles = THashSet<VirtualFile>()
        private var changedFile: VirtualFile? = null
        private var createdFileParent: VirtualFile? = null

        override fun run() {
            try {
                // write action might add new files to queues, so only remember processed files for duration of read action
                val alreadyProcessedFiles = THashSet<VirtualFile>()
                val alreadyProcessedModules = THashSet<Module>()

                while (true) {
                    while (true) {
                        ProgressManager.checkCanceled()
                        val file = changedFile
                            ?: synchronized(changedFiles) { changedFiles.poll() }.also { changedFile = it }
                            ?: break

                        if (alreadyProcessedFiles.add(file)) dirtyFiles.addAll(getDependentFiles(file))
                        changedFile = null
                    }

                    while (true) {
                        ProgressManager.checkCanceled()
                        val parent = createdFileParent
                            ?: synchronized(changedFiles) { createdFileParents.poll() }.also { createdFileParent = it }
                            ?: break

                        ModuleUtil.findModuleForFile(parent, project)?.takeIf { alreadyProcessedModules.add(it) }?.let { module ->
                            //TODO: Is there a better way with LookupTracker?
                            dirtyFiles.addAll(FileTypeIndex.getFiles(KotlinFileType.INSTANCE, module.moduleWithDependentsScope))
                        }
                        createdFileParent = null
                    }

                    FileSymbolTablesCache.getInstance(project).invalidateDirtyIncludeFiles(dirtyFiles)
                    dirtyFiles.clear()

                    synchronized(changedFiles) {
                        if (changedFiles.isEmpty() && createdFileParents.isEmpty()) {
                            invalidationWorker = null
                            return@run
                        }
                    }
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (t: Throwable) {
                LOG.error("Error during invalidation", t)
                throw t
            }
        }
    }

    private fun scheduleInvalidation(newlyChangedFiles: Collection<VirtualFile>, newlyCreatedFileParents: Collection<VirtualFile>) {
        if (newlyChangedFiles.isEmpty() && newlyCreatedFileParents.isEmpty()) return
        synchronized(changedFiles) {
            changedFiles += newlyChangedFiles
            createdFileParents += newlyCreatedFileParents
            if (invalidationWorker == null) {
                invalidationWorker =
                    ReadAction.nonBlocking(InvalidationWorker())
                        .inSmartMode(project)
                        .expireWith(this)
                        .submit(AppExecutorUtil.getAppExecutorService())
                        .onError { invalidationWorker = null }
            }
        }
    }

    fun waitForPendingInvalidation() {
        ProgressIndicatorUtils.awaitWithCheckCanceled(invalidationWorker ?: return)
    }

    private inner class VerificationWorker : Runnable {
        private val dirtyFiles = THashSet<VirtualFile>()
        private var pendingVerificationFile: VirtualFile? = null

        override fun run() {
            try {
                // write action might add new files to queues, so only remember processed files for duration of read action
                val alreadyProcessedFiles = THashSet<VirtualFile>()

                while (true) {
                    while (true) {
                        ProgressManager.checkCanceled()
                        val file = pendingVerificationFile
                            ?: synchronized(pendingVerificationFiles) { pendingVerificationFiles.poll() }.also {
                                pendingVerificationFile = it
                            }
                            ?: break

                        if (alreadyProcessedFiles.add(file)) {
                            val moduleInfo by lazy(LazyThreadSafetyMode.NONE) {
                                getModuleInfoByVirtualFile(project, file)?.unwrapModuleSourceInfo()
                            }
                            for (table in FileSymbolTablesCache.getInstance(project).allTablesForFile(file)) {
                                val marker = table.getKtDependencyMarker()
                                if (marker == null || !marker.needsVerification) continue
                                if (!KtFileTranslator.verifyDependencies(file, moduleInfo, marker.dependencies)) {
                                    dirtyFiles += file
                                    break
                                }
                                marker.needsVerification = false
                            }
                        }

                        if (alreadyProcessedFiles.add(file)) dirtyFiles.addAll(getDependentFiles(file))
                        pendingVerificationFile = null
                    }

                    FileSymbolTablesCache.getInstance(project).invalidateDirtyIncludeFiles(dirtyFiles)
                    dirtyFiles.clear()

                    synchronized(pendingVerificationFiles) {
                        if (pendingVerificationFiles.isEmpty()) {
                            verificationWorker = null
                            return@run
                        }
                    }
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (t: Throwable) {
                LOG.error("Error during verification", t)
                throw t
            }
        }
    }

    private fun scheduleVerification(file: VirtualFile) {
        synchronized(pendingVerificationFiles) {
            pendingVerificationFiles += file
            if (verificationWorker == null) {
                verificationWorker =
                    ReadAction.nonBlocking(VerificationWorker())
                        .inSmartMode(project)
                        .expireWith(this)
                        .submit(AppExecutorUtil.getAppExecutorService())
                        .onError { verificationWorker = null }
            }
        }
    }

    fun waitForPendingVerifications() {
        ProgressIndicatorUtils.awaitWithCheckCanceled(verificationWorker ?: return)
    }

    @Synchronized
    private fun getDependentFiles(file: VirtualFile): ImmutableList<VirtualFile> = dependent.get(file).toImmutableList()

    class Listener(val project: Project) : FileSymbolTablesPackListener {
        override fun afterTableAddedToPack(table: FileSymbolTable) {
            val marker = table.getKtDependencyMarker() ?: return
            with(getInstance(project)) {
                addDependencies(marker.containingFile, marker.dependencies)
                if (marker.needsVerification) scheduleVerification(marker.containingFile)
            }
        }

        override fun beforeTableRemovedFromPack(table: FileSymbolTable) {
            val marker = table.getKtDependencyMarker() ?: return
            getInstance(project)
                .removeDependencies(marker.containingFile, marker.dependencies)
        }
    }

    override fun dispose(): Unit = Unit

    companion object {
        fun getInstance(project: Project): KtDependencyGraph = project.service()

        private val LOG = Logger.getInstance("#org.jetbrains.konan.resolve.KtDependencyGraph")

        private fun isRelevant(file: VirtualFile): Boolean = FileTypeManager.getInstance().isFileOfType(file, KotlinFileType.INSTANCE)

        private fun isRelevant(name: String): Boolean = FileTypeManager.getInstance().run {
            getFileTypeByFileName(name) === KotlinFileType.INSTANCE && !isFileIgnored(name)
        }

        private fun isFileNameChange(propertyChangeEvent: VFilePropertyChangeEvent): Boolean =
            propertyChangeEvent.propertyName == VirtualFile.PROP_NAME && propertyChangeEvent.oldValue != propertyChangeEvent.newValue

        private fun isSymlinkChange(propertyChangeEvent: VFilePropertyChangeEvent): Boolean =
            propertyChangeEvent.propertyName == VirtualFile.PROP_SYMLINK_TARGET

        private fun FileSymbolTable.getKtDependencyMarker(): KtDependencyMarker? =
            contents.lastOrNull() as? KtDependencyMarker
    }
}