/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.util.containers.SoftFactoryMap

class LibraryEffectiveKindProviderImpl(project: Project) : LibraryEffectiveKindProvider {
    private val effectiveKindMap = object : SoftFactoryMap<LibraryEx, PersistentLibraryKind<*>?>() {
        override fun create(key: LibraryEx) = detectLibraryKind(key.getFiles(OrderRootType.CLASSES))
    }

    init {
        project.messageBus.connect().subscribe(
            ProjectTopics.PROJECT_ROOTS,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    synchronized(effectiveKindMap) {
                        effectiveKindMap.clear()
                    }
                }
            }
        )
    }

    override fun getEffectiveKind(library: LibraryEx): PersistentLibraryKind<*>? {
        if (library.isDisposed) {
            return null
        }

        return when (val kind = library.kind) {
            is KotlinLibraryKind -> kind
            else -> synchronized(effectiveKindMap) {
                effectiveKindMap.get(library)
            }
        }
    }
}