package org.jetbrains.konan.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import gnu.trove.TObjectIntHashMap
import org.jetbrains.kotlin.analyzer.ModuleDescriptorListener
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.util.concurrent.ConcurrentMap

class KtPendingTranslationsManager(val project: Project) : ModuleDescriptorListener {
    init {
        project.messageBus.connect().subscribe(ModuleDescriptorListener.TOPIC, this)
    }

    private val pendingTranslationRegistries: ConcurrentMap<ModuleDescriptor, Registry> = ContainerUtil.createConcurrentWeakMap()

    fun getPendingTranslationsRegistry(moduleDescriptor: ModuleDescriptor): KtPendingTranslationsRegistry {
        ApplicationManager.getApplication().assertReadAccessAllowed()
        return pendingTranslationRegistries.computeIfAbsent(moduleDescriptor) { Registry() }
    }

    override fun moduleDescriptorInvalidated(moduleDescriptor: ModuleDescriptor) {
        invokeLater {
            runWriteAction {
                val inDescriptor = pendingTranslationRegistries.remove(moduleDescriptor) ?: return@runWriteAction
                // we rely on the fact that symbol building and therefore starting to translate happens in read action
                // and the registry is always freshly retrieved, therefore no new files can appear here
                FileSymbolTablesCache.getInstance(project).invalidateDirtyIncludeFiles(inDescriptor.files)
            }
        }
    }

    private class Registry : TObjectIntHashMap<VirtualFile>(), KtPendingTranslationsRegistry {
        @get:Synchronized
        @get:Suppress("UNCHECKED_CAST")
        internal val files: List<VirtualFile>
            get() = keys().asList() as List<VirtualFile>

        @Synchronized
        override fun newPendingTranslation(file: VirtualFile) {
            ApplicationManager.getApplication().assertReadAccessAllowed()
            val index = insertionIndex(file)
            if (index < 0) {
                _values[-index - 1]++
            } else {
                val usedFreeSlot = _set[index] == null
                _set[index] = file
                _values[index] = 1
                postInsertHook(usedFreeSlot)
            }
        }

        @Synchronized
        override fun successfullyCompletedTranslation(file: VirtualFile) {
            // we might miss unregistrations that happen after files were retrieved, but invalidating too much is safe
            val index = index(file)
            if (index >= 0) {
                val value = _values[index]
                if (value > 0) {
                    when (value) {
                        1 -> removeAt(index)
                        else -> _values[index] = value - 1
                    }
                    return
                }
            }
            throw AssertionError("Imbalanced reference count")
        }
    }

    companion object {
        fun getInstance(project: Project): KtPendingTranslationsManager = project.service()
    }
}

interface KtPendingTranslationsRegistry {
    fun newPendingTranslation(file: VirtualFile)
    fun successfullyCompletedTranslation(file: VirtualFile)
}