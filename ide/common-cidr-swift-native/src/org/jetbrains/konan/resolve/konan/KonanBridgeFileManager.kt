/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import org.jetbrains.konan.resolve.KtModificationCount
import org.jetbrains.kotlin.idea.caches.trackers.KotlinCodeBlockModificationListener.Companion.getInsideCodeBlockModificationScope
import org.jetbrains.kotlin.psi.KtFile

class KonanBridgeFileManager(private val project: Project) {
    private val myLock = Object()

    private var myStamp: Long = -1
    private var myActualFiles: MutableMap<KonanTarget, KonanBridgeVirtualFile>? = null

    init {
        (PsiManager.getInstance(project) as? PsiManagerImpl)?.addTreeChangePreprocessor(ModificationListener())
    }

    fun forTarget(target: KonanTarget, name: String = target.run { "$productModuleName/$productModuleName.h" }): KonanBridgeVirtualFile {
        val modificationStamp = KtModificationCount.getInstance(project).get()
        synchronized(myLock) {
            val map = if (myActualFiles == null || myStamp < modificationStamp) {
                val map = hashMapOf<KonanTarget, KonanBridgeVirtualFile>()
                myActualFiles = map
                myStamp = modificationStamp
                map
            } else {
                myActualFiles!!
            }

            map[target]?.let { return it }

            val newFile = KonanBridgeVirtualFile(target, name, project, modificationStamp)
            map[target] = newFile
            return newFile
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KonanBridgeFileManager = project.service()
    }

    inner class ModificationListener : PsiTreeChangePreprocessor {
        override fun treeChanged(event: PsiTreeChangeEventImpl) {
            var file = event.file
            val child = event.child

            if (file == null && child != null) {
                file = child.containingFile
            }
            if (file == null && event.parent != null) {
                file = event.parent.containingFile
            }

            val parent = event.parent
            if (file !is KtFile) return

            when (event.code) {
                PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILDREN_CHANGE,
                PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_REPLACEMENT,
                PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_ADDITION,
                PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_REMOVAL,
                PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_MOVEMENT -> {
                }

                PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED -> {
                    assert(child != null)
                    if (child!!.isValid) {
                        processChange(parent)
                    }
                }

                PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED -> {
                    processChange(parent)
                }

                PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED -> {
                    assert(child != null)
                    processChange(parent)
                }

                PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED -> if (!event.isGenericChange) {
                    processChange(parent)
                }

                PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED,
                PsiTreeChangeEventImpl.PsiEventType.BEFORE_PROPERTY_CHANGE,
                PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED -> processChange(null)
                else -> OCLog.LOG.error("Unknown code:" + event.code)
            }
        }

        private fun processChange(parent: PsiElement?) {
            ApplicationManager.getApplication().assertIsDispatchThread()
            if (!isOutOfCodeBlockChange(parent)) return

            val dirtyFiles = synchronized(myLock) { myActualFiles?.values } ?: return
            FileSymbolTablesCache.getInstance(project).invalidateDirtyIncludeFiles(dirtyFiles as Collection<VirtualFile>)
        }

        private fun isOutOfCodeBlockChange(parent: PsiElement?): Boolean =
            parent == null || getInsideCodeBlockModificationScope(parent) == null
    }
}