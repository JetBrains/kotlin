package com.jetbrains.mobile.documentation

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.documentation.Xcode8ApiReferenceManager

class AppleDocumentationManager(project: Project) : Xcode8ApiReferenceManager(project) {
    override fun getPlatformName(psiElement: PsiElement): String? = "ios"
}