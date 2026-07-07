/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

/**
 * A project service that collects the [PsiReference]s contributed to a PSI element by the registered reference
 * providers.
 *
 * Reference contribution is environment-specific, so the concrete implementation is supplied by the surrounding
 * platform (for example, the IDE). When no implementation is registered, the default returns no references.
 */
open class KotlinReferenceProvidersService {
    /**
     * Returns the references contributed to the given element, or an empty array if none are contributed.
     */
    open fun getReferences(psiElement: PsiElement): Array<PsiReference> = PsiReference.EMPTY_ARRAY

    companion object {
        private val NO_REFERENCES_SERVICE = KotlinReferenceProvidersService()

        /**
         * Returns the service registered in the given [project], or a no-op fallback if none is registered.
         */
        @JvmStatic
        fun getInstance(project: Project): KotlinReferenceProvidersService {
            return project.getService(KotlinReferenceProvidersService::class.java) ?: NO_REFERENCES_SERVICE
        }

        /**
         * A convenience shortcut that resolves the service for the element's project and returns its references.
         */
        @JvmStatic
        fun getReferencesFromProviders(psiElement: PsiElement): Array<PsiReference> {
            return getInstance(psiElement.project).getReferences(psiElement)
        }
    }
}
