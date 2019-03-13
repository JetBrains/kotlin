/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.findUsages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import org.jetbrains.konan.resolve.symbols.KotlinLightSymbol
import org.jetbrains.konan.resolve.symbols.KotlinOCPsiWrapper
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.objcexport.createNamer
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty

class KotlinOCUsageSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val target = parameters.getUnwrappedTarget()
        val symbols = target.toSymbols()
        val optimizer = parameters.optimizer
        var effectiveSearchScope: SearchScope? = null
        symbols.forEach { symbol ->
            val psiWrapper = KotlinOCPsiWrapper(target, symbol)
            if (effectiveSearchScope == null) {
                //infer effectiveSearchScope only once. it's the same for all symbols
                val symbolParameters = parameters.duplicateWith(psiWrapper)
                effectiveSearchScope = symbolParameters.effectiveSearchScope
            }
            optimizer.searchWord(symbol.name, effectiveSearchScope!!, UsageSearchContext.IN_CODE, true, psiWrapper)
        }
    }

    private fun PsiElement.toSymbols(): List<OCSymbol> {
        return when (this) {
            is KtClass -> toSymbols<ClassDescriptor>(OCSymbolKind.INTERFACE) { d -> getClassOrProtocolName(d).objCName }
            is KtProperty -> toSymbols<PropertyDescriptor>(OCSymbolKind.PROPERTY) { d -> getPropertyName(d) }
            else -> emptyList()
        }
    }
}

internal fun KtElement.getNamer(): ObjCExportNamer {
    //todo cache
    val moduleDescriptor = findModuleDescriptor()
    return createNamer(moduleDescriptor, "KNF")
}

internal fun ReferencesSearch.SearchParameters.getUnwrappedTarget(): PsiElement {
    val elementToSearch = elementToSearch
    return (elementToSearch as? KotlinOCPsiWrapper)?.psi ?: elementToSearch
}

internal inline fun <reified T : DeclarationDescriptor> KtDeclaration.toSymbols(
    kind: OCSymbolKind,
    getName: ObjCExportNamer.(T) -> String
): List<KotlinLightSymbol> {
    val descriptor = toDescriptor() as? T ?: return emptyList()
    val namer = getNamer()

    val descriptors = getSuperDescriptorsOrTheSameDescriptor(descriptor)
    return descriptors.map { KotlinLightSymbol(this, namer.getName(it), kind) }
}

private fun <T: DeclarationDescriptor> getSuperDescriptorsOrTheSameDescriptor(descriptor: T): Collection<T> {
    if (descriptor is CallableDescriptor) {
        val superDescriptors = descriptor.overriddenDescriptors as Collection<T>
        if (superDescriptors.isNotEmpty()) {
            return superDescriptors
        }
    }
    return listOf(descriptor)
}

internal fun ReferencesSearch.SearchParameters.duplicateWith(psi: PsiElement): ReferencesSearch.SearchParameters {
    return ReferencesSearch.SearchParameters(psi, scopeDeterminedByUser, isIgnoreAccessScope, optimizer)
}
