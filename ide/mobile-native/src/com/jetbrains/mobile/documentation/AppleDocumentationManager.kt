/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.documentation

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.documentation.Xcode8ApiReferenceManager

class AppleDocumentationManager(project: Project) : Xcode8ApiReferenceManager(project) {
    override fun getPlatformName(psiElement: PsiElement): String? = "ios"
}