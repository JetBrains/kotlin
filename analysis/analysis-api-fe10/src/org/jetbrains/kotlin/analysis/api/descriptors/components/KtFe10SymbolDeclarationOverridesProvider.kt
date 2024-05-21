/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSymbolDeclarationOverridesProviderBase
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf

internal class KaFe10SymbolDeclarationOverridesProvider(
    override val analysisSession: KaFe10Session
) : KaSymbolDeclarationOverridesProviderBase(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun <T : KaSymbol> getAllOverriddenSymbols(callableSymbol: T): List<KaCallableSymbol> {
        if (callableSymbol is KaValueParameterSymbol) {
            return callableSymbol.getAllOverriddenSymbols()
        }
        val descriptor = getSymbolDescriptor(callableSymbol) as? CallableMemberDescriptor ?: return emptyList()
        return getOverriddenDescriptors(descriptor, true).mapNotNull { it.toKtCallableSymbol(analysisContext) }.distinct()
    }

    override fun <T : KaSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KaCallableSymbol> {
        if (callableSymbol is KaValueParameterSymbol) {
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

    override fun isSubClassOf(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): Boolean {
        if (subClass == superClass) return false

        val subClassDescriptor = getSymbolDescriptor(subClass) as? ClassDescriptor ?: return false
        val superClassDescriptor = getSymbolDescriptor(superClass) as? ClassDescriptor ?: return false
        return subClassDescriptor.isSubclassOf(superClassDescriptor)
    }

    override fun isDirectSubClassOf(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): Boolean {
        val subClassDescriptor = getSymbolDescriptor(subClass) as? ClassDescriptor ?: return false
        val superClassDescriptor = getSymbolDescriptor(superClass) as? ClassDescriptor ?: return false
        return subClassDescriptor.getSuperClassOrAny() == superClassDescriptor
    }

    override fun getIntersectionOverriddenSymbols(symbol: KaCallableSymbol): Collection<KaCallableSymbol> {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}