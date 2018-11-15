package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement


// NO-OP implementation, full implementation only for AS3.3, AS3.4

fun collectModuleInfoByUserData(
    project: Project,
    virtualFile: VirtualFile
): List<IdeaModuleInfo> = emptyList()

fun collectModuleInfoByUserData(
    psiElement: PsiElement
): List<IdeaModuleInfo> = emptyList()
