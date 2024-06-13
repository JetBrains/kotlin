/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaInheritorsProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind

internal class KaFe10InheritorsProvider(
    override val analysisSession: KaFe10Session
) : KaInheritorsProvider(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun getInheritorsOfSealedClass(classSymbol: KaNamedClassOrObjectSymbol): List<KaNamedClassOrObjectSymbol> {
        val classDescriptor = getSymbolDescriptor(classSymbol) as? ClassDescriptor ?: return emptyList()

        val inheritorsProvider = analysisContext.resolveSession.sealedClassInheritorsProvider
        val allowInDifferentFiles = analysisContext.resolveSession.languageVersionSettings
            .supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage)

        return inheritorsProvider.computeSealedSubclasses(classDescriptor, allowInDifferentFiles)
            .mapNotNull { it.toKtClassifierSymbol(analysisContext) as? KaNamedClassOrObjectSymbol }
    }

    override fun getEnumEntries(classSymbol: KaNamedClassOrObjectSymbol): List<KaEnumEntrySymbol> {
        val enumDescriptor = getSymbolDescriptor(classSymbol) as? ClassDescriptor ?: return emptyList()
        if (enumDescriptor.kind != ClassKind.ENUM_CLASS) {
            return emptyList()
        }

        val result = mutableListOf<KaEnumEntrySymbol>()

        for (entryDescriptor in enumDescriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
            if (entryDescriptor is ClassDescriptor && entryDescriptor.kind == ClassKind.ENUM_ENTRY) {
                result += KaFe10DescEnumEntrySymbol(entryDescriptor, analysisContext)
            }
        }

        return result
    }
}