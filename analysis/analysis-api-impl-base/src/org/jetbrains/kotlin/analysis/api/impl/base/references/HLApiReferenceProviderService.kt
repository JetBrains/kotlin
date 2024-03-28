/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.references

import com.intellij.openapi.project.Project
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.references.KotlinPsiReferenceProvider
import org.jetbrains.kotlin.idea.references.KotlinPsiReferenceRegistrar
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.utils.SmartList

internal class HLApiReferenceProviderService(val project: Project) : KotlinReferenceProvidersService() {
    private val originalProvidersBinding: MultiMap<Class<out PsiElement>, KotlinPsiReferenceProvider>
    private val providersBindingCache: Map<Class<out PsiElement>, List<KotlinPsiReferenceProvider>>

    init {
        val registrar = KotlinPsiReferenceRegistrar()
        KotlinReferenceProviderContributor.getInstance(project).registerReferenceProviders(registrar)
        originalProvidersBinding = registrar.providers

        providersBindingCache = ConcurrentFactoryMap.createMap<Class<out PsiElement>, List<KotlinPsiReferenceProvider>> { klass ->
            val result = SmartList<KotlinPsiReferenceProvider>()
            for (bindingClass in originalProvidersBinding.keySet()) {
                if (bindingClass.isAssignableFrom(klass)) {
                    result.addAll(originalProvidersBinding.get(bindingClass))
                }
            }
            result
        }
    }

    private fun doGetKotlinReferencesFromProviders(context: PsiElement): Array<PsiReference> {
        val providers: List<KotlinPsiReferenceProvider>? = providersBindingCache[context.javaClass]
        if (providers.isNullOrEmpty()) return PsiReference.EMPTY_ARRAY

        val result = SmartList<PsiReference>()
        for (provider in providers) {
            result.addAll(provider.getReferencesByElement(context))
        }

        if (result.isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }

        return result.toTypedArray()
    }

    override fun getReferences(psiElement: PsiElement): Array<PsiReference> {
        if (psiElement is ContributedReferenceHost) {
            return ReferenceProvidersRegistry.getReferencesFromProviders(psiElement, PsiReferenceService.Hints.NO_HINTS)
        }

        return CachedValuesManager.getCachedValue(psiElement) {
            CachedValueProvider.Result.create(
                doGetKotlinReferencesFromProviders(psiElement),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }
}
