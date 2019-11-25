/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile

fun Project.getKtFile(
    virtualFile: VirtualFile?,
    ktFile: KtFile? = null
): KtFile? {
    if (virtualFile == null) return null
    if (ktFile != null) {
        check(ktFile.originalFile.virtualFile == virtualFile)
        return ktFile
    } else {
        return runReadAction { PsiManager.getInstance(this).findFile(virtualFile) as? KtFile }
    }
}