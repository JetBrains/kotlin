/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.FqNameUnsafe

interface NotPropertiesService {

    fun getNotProperties(element: PsiElement): Set<FqNameUnsafe>

    companion object {
        fun getInstance(project: Project): NotPropertiesService {
            return ServiceManager.getService(project, NotPropertiesService::class.java)
        }

        fun getNotProperties(element: PsiElement) = getInstance(element.project).getNotProperties(element)
    }
}