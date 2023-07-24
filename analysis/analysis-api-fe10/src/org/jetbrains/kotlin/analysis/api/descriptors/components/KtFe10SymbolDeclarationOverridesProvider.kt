/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KtSymbolDeclarationOverridesProviderBase
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf

internal class KtFe10SymbolDeclarationOverridesProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtSymbolDeclarationOverridesProviderBase(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun <T : KtSymbol> getAllOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol> {
        if (callableSymbol is KtValueParameterSymbol) {
            return callableSymbol.getAllOverriddenSymbols()
        }
        val descriptor = getSymbolDescriptor(callableSymbol) as? CallableMemberDescriptor ?: return emptyList()
        return getOverriddenDescriptors(descriptor, true).mapNotNull { it.toKtCallableSymbol(analysisContext) }.distinct()
    }

    override fun <T : KtSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol> {
        if (callableSymbol is KtValueParameterSymbol) {
            return callableSymbol.getDirectlyOverriddenSymbols()
        }
        val descriptor = getSymbolDescriptor(callableSymbol) as? CallableMemberDescriptor ?: return emptyList()
        return getOverriddenDescriptors(descriptor, false).mapNotNull { it.toKtCallableSymbol(analysisContext) }.distinct()
    }

    private fun getOverriddenDescriptors(
        descriptor: CallableMemberDescriptor,
        collectAllOverrides: Boolean
    ): Collection<CallableMemberDescriptor> {
        val overriddenDescriptors = LinkedHashSet<CallableMemberDescriptor>()
        val queue = ArrayDeque<CallableMemberDescriptor>().apply { addAll(descriptor.overriddenDescriptors) }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (current.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                overriddenDescriptors.add(current)

                if (!collectAllOverrides) {
                    continue
                }
            }

            val overriddenDescriptorsForCurrent = current.overriddenDescriptors
            for (overriddenDescriptor in overriddenDescriptorsForCurrent) {
                if (overriddenDescriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                    overriddenDescriptors.add(overriddenDescriptor)
                }
            }
            queue.addAll(overriddenDescriptorsForCurrent)
        }

        return overriddenDescriptors
    }

    override fun isSubClassOf(subClass: KtClassOrObjectSymbol, superClass: KtClassOrObjectSymbol): Boolean {
        val subClassDescriptor = getSymbolDescriptor(subClass) as? ClassDescriptor ?: return false
        val superClassDescriptor = getSymbolDescriptor(superClass) as? ClassDescriptor ?: return false
        return subClassDescriptor.isSubclassOf(superClassDescriptor)
    }

    override fun isDirectSubClassOf(subClass: KtClassOrObjectSymbol, superClass: KtClassOrObjectSymbol): Boolean {
        val subClassDescriptor = getSymbolDescriptor(subClass) as? ClassDescriptor ?: return false
        val superClassDescriptor = getSymbolDescriptor(superClass) as? ClassDescriptor ?: return false
        return subClassDescriptor.getSuperClassOrAny() == superClassDescriptor
    }

    override fun getIntersectionOverriddenSymbols(symbol: KtCallableSymbol): Collection<KtCallableSymbol> {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}