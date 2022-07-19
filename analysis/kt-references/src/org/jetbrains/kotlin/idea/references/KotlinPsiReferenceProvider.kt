/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.psi.KtElement

interface KotlinPsiReferenceProvider {
    fun getReferencesByElement(element: PsiElement): Array<PsiReference>
}

interface KotlinReferenceProviderContributor {
    fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar)

    companion object {
        fun getInstance(project: Project): KotlinReferenceProviderContributor =
            project.getService(KotlinReferenceProviderContributor::class.java)
    }
}


class KotlinPsiReferenceRegistrar {
    val providers: MultiMap<Class<out PsiElement>, KotlinPsiReferenceProvider> = MultiMap(LinkedHashMap())

    inline fun <reified E : KtElement> registerProvider(crossinline factory: (E) -> PsiReference?) {
        registerMultiProvider<E> { element ->
            factory(element)?.let { reference -> arrayOf(reference) } ?: PsiReference.EMPTY_ARRAY
        }
    }

    inline fun <reified E : KtElement>  registerMultiProvider(crossinline factory: (E) -> Array<PsiReference>) {
        val provider: KotlinPsiReferenceProvider = object : KotlinPsiReferenceProvider {
            override fun getReferencesByElement(element: PsiElement): Array<PsiReference> {
                return factory(element as E)
            }
        }

        registerMultiProvider(E::class.java, provider)
    }

    fun registerMultiProvider(klass: Class<out PsiElement>, provider: KotlinPsiReferenceProvider) {
        providers.putValue(klass, provider)
    }
}
