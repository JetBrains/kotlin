/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

open class KotlinReferenceProvidersService {
    open fun getReferences(psiElement: PsiElement): Array<PsiReference> = PsiReference.EMPTY_ARRAY

    companion object {
        private val NO_REFERENCES_SERVICE = KotlinReferenceProvidersService()

        @JvmStatic
        fun getInstance(project: Project): KotlinReferenceProvidersService {
            return project.getService(KotlinReferenceProvidersService::class.java) ?: NO_REFERENCES_SERVICE
        }

        @JvmStatic
        fun getReferencesFromProviders(psiElement: PsiElement): Array<PsiReference> {
            return getInstance(psiElement.project).getReferences(psiElement)
        }
    }
}
