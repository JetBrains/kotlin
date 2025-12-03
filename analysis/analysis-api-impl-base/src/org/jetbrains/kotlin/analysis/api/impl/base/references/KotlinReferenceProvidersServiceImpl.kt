/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.references

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor.ReferenceProvider
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

internal class KotlinReferenceProvidersServiceImpl(val project: Project) : KotlinReferenceProvidersService() {
    private val originalProvidersBinding: MultiMap<Class<out PsiElement>, ReferenceProvider<KtElement>> by lazy {
        MultiMap<Class<out PsiElement>, ReferenceProvider<KtElement>>(LinkedHashMap()).apply {
            EP_NAME.getExtensionList(project).forEach { contributor ->
                runSafely {
                    putValue(contributor.elementClass, contributor.referenceProvider)
                }
            }
        }
    }

    private val providersBindingCache: Map<Class<out PsiElement>, List<ReferenceProvider<KtElement>>> =
        ConcurrentFactoryMap.createMap { klass ->
            buildSmartList {
                for (bindingClass in originalProvidersBinding.keySet()) {
                    if (bindingClass.isAssignableFrom(klass)) {
                        addAll(originalProvidersBinding[bindingClass])
                    }
                }
            }
        }

    private fun doGetKotlinReferencesFromProviders(context: KtElement): Array<PsiReference> {
        val providers: List<ReferenceProvider<KtElement>>? = providersBindingCache[context.javaClass]
        if (providers.isNullOrEmpty()) return PsiReference.EMPTY_ARRAY

        val result = buildSmartList {
            for (provider in providers) {
                runSafely {
                    val references = provider(context)
                    addAll(references)
                }
            }
        }

        return result.ifNotEmpty { toTypedArray() } ?: PsiReference.EMPTY_ARRAY
    }

    override fun getReferences(psiElement: PsiElement): Array<PsiReference> = when (psiElement) {
        is ContributedReferenceHost -> ReferenceProvidersRegistry.getReferencesFromProviders(psiElement)
        !is KtElement -> PsiReference.EMPTY_ARRAY
        else -> CachedValuesManager.getCachedValue(psiElement) {
            CachedValueProvider.Result.create(
                doGetKotlinReferencesFromProviders(psiElement),
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }
    }

    companion object {
        private val EP_NAME: ExtensionPointName<KotlinPsiReferenceProviderContributor<in KtElement>> = ExtensionPointName(
            "org.jetbrains.kotlin.psiReferenceProvider",
        )
    }
}

private inline fun runSafely(action: () -> Unit) {
    try {
        action()
    } catch (e: Throwable) {
        rethrowIntellijPlatformExceptionIfNeeded(e)

        logger<KotlinPsiReferenceProviderContributor<*>>().error(e)
    }
}