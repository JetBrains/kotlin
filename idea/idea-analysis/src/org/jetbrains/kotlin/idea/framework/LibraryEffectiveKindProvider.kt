/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.PersistentLibraryKind

interface LibraryEffectiveKindProvider {
    fun getEffectiveKind(library: LibraryEx): PersistentLibraryKind<*>?

    companion object {
        fun getInstance(project: Project) = ServiceManager.getService(project, LibraryEffectiveKindProvider::class.java)!!
    }
}

fun LibraryEx.effectiveKind(project: Project) = LibraryEffectiveKindProvider.getInstance(project).getEffectiveKind(this)