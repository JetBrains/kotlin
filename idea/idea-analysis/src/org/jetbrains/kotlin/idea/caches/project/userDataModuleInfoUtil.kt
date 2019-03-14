/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.LIBRARY_KEY
import org.jetbrains.kotlin.idea.MODULE_ROOT_TYPE_KEY
import org.jetbrains.kotlin.idea.SDK_KEY
import org.jetbrains.kotlin.idea.caches.project.UserDataModuleContainer.ForPsiElement
import org.jetbrains.kotlin.idea.caches.project.UserDataModuleContainer.ForVirtualFile
import org.jetbrains.kotlin.idea.core.getSourceType
import org.jetbrains.kotlin.utils.addIfNotNull

// This file declares non-exported API for overriding module info with user-data


private sealed class UserDataModuleContainer {
    abstract fun <T> getUserData(key: Key<T>): T?
    abstract fun getModule(): Module?

    data class ForVirtualFile(val virtualFile: VirtualFile, val project: Project) : UserDataModuleContainer() {
        override fun <T> getUserData(key: Key<T>): T? = virtualFile.getUserData(key)
        override fun getModule(): Module? = ModuleUtilCore.findModuleForFile(virtualFile, project)
    }

    data class ForPsiElement(val psiElement: PsiElement) : UserDataModuleContainer() {
        override fun <T> getUserData(key: Key<T>): T? {
            return psiElement.getUserData(key)
                ?: psiElement.containingFile?.getUserData(key)
                ?: psiElement.containingFile?.originalFile?.virtualFile?.getUserData(key)
        }

        override fun getModule(): Module? = ModuleUtilCore.findModuleForPsiElement(psiElement)
    }
}


private fun collectModuleInfoByUserData(
    project: Project,
    container: UserDataModuleContainer
): List<IdeaModuleInfo> {
    fun forModule(): ModuleSourceInfo? {
        val rootType = container.getUserData(MODULE_ROOT_TYPE_KEY) ?: return null
        val module = container.getModule() ?: return null

        return when (rootType.getSourceType()) {
            null -> null
            SourceType.PRODUCTION -> module.productionSourceInfo()
            SourceType.TEST -> module.testSourceInfo()
        }
    }

    val result = mutableListOf<IdeaModuleInfo>()
    result.addIfNotNull(forModule())

    val library = container.getUserData(LIBRARY_KEY)
    if (library != null) {
        result.addAll(createLibraryInfo(project, library))
    }

    val sdk = container.getUserData(SDK_KEY)
    if (sdk != null) {
        result.add(SdkInfo(project, sdk))
    }

    return result
}


fun collectModuleInfoByUserData(
    project: Project,
    virtualFile: VirtualFile
): List<IdeaModuleInfo> = collectModuleInfoByUserData(project, ForVirtualFile(virtualFile, project))

fun collectModuleInfoByUserData(
    psiElement: PsiElement
): List<IdeaModuleInfo> = collectModuleInfoByUserData(psiElement.project, ForPsiElement(psiElement))
