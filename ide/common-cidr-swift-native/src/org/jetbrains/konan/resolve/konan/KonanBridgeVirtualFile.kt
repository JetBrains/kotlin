/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.konan

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.testFramework.LightVirtualFile

class KonanBridgeVirtualFile(
    val target: KonanTarget,
    name: String,
    val project: Project,
    modificationStamp: Long
) : LightVirtualFile(name, null, "", modificationStamp) {
    init {
        val managerEx = PsiManagerEx.getInstanceEx(project)
        managerEx.fileManager.setViewProvider(this, MySingleRootFileViewProvider(managerEx))
    }

    override fun hashCode(): Int = (name.hashCode() * 31 + target.hashCode()) * 31 + modificationStamp.toInt()

    override fun equals(other: Any?): Boolean = other is KonanBridgeVirtualFile &&
            modificationStamp == other.modificationStamp &&
            name == other.name &&
            other.target == target

    override fun isWritable(): Boolean = false

    private inner class MySingleRootFileViewProvider(manager: PsiManager) : SingleRootFileViewProvider(manager, this) {
        override fun createFile(lang: Language): PsiFile? = KonanBridgePsiFile(target, this)
    }
}