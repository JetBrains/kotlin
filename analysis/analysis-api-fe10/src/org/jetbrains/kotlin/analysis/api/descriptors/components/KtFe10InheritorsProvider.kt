/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtInheritorsProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind

internal class KtFe10InheritorsProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtInheritorsProvider(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun getInheritorsOfSealedClass(classSymbol: KtNamedClassOrObjectSymbol): List<KtNamedClassOrObjectSymbol> {
        val classDescriptor = getSymbolDescriptor(classSymbol) as? ClassDescriptor ?: return emptyList()

        val inheritorsProvider = analysisContext.resolveSession.sealedClassInheritorsProvider
        val allowInDifferentFiles = analysisContext.resolveSession.languageVersionSettings
            .supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage)

        return inheritorsProvider.computeSealedSubclasses(classDescriptor, allowInDifferentFiles)
            .mapNotNull { it.toKtClassifierSymbol(analysisContext) as? KtNamedClassOrObjectSymbol }
    }

    override fun getEnumEntries(classSymbol: KtNamedClassOrObjectSymbol): List<KtEnumEntrySymbol> {
        val enumDescriptor = getSymbolDescriptor(classSymbol) as? ClassDescriptor ?: return emptyList()
        if (enumDescriptor.kind != ClassKind.ENUM_CLASS) {
            return emptyList()
        }

        val result = mutableListOf<KtEnumEntrySymbol>()

        for (entryDescriptor in enumDescriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
            if (entryDescriptor is ClassDescriptor && entryDescriptor.kind == ClassKind.ENUM_ENTRY) {
                result += KtFe10DescEnumEntrySymbol(entryDescriptor, analysisContext)
            }
        }

        return result
    }
}