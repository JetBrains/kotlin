/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKaSymbolDeclarationOverridesProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf

internal class KaFe10SymbolDeclarationOverridesProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : AbstractKaSymbolDeclarationOverridesProvider<KaFe10Session>(), KaFe10SessionComponent {
    fun <T : KaSymbol> getAllOverriddenSymbols(callableSymbol: T): Sequence<KaCallableSymbol> {
        if (callableSymbol is KaValueParameterSymbol) {
            return getAllOverriddenSymbolsForParameter(callableSymbol)
        }

        val descriptor = getSymbolDescriptor(callableSymbol) as? CallableMemberDescriptor ?: return emptySequence()

        return getOverriddenDescriptors(descriptor, true)
            .mapNotNull { it.toKtCallableSymbol(analysisContext) }
            .distinct()
            .asSequence()
    }

    fun <T : KaSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): Sequence<KaCallableSymbol> {
        if (callableSymbol is KaValueParameterSymbol) {
            return getDirectlyOverriddenSymbolsForParameter(callableSymbol)
        }

        val descriptor = getSymbolDescriptor(callableSymbol) as? CallableMemberDescriptor ?: return emptySequence()

        return getOverriddenDescriptors(descriptor, false)
            .mapNotNull { it.toKtCallableSymbol(analysisContext) }
            .distinct()
            .asSequence()
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

    fun isSubClassOf(subClass: KaClassSymbol, superClass: KaClassSymbol): Boolean {
        if (subClass == superClass) return false

        val subClassDescriptor = getSymbolDescriptor(subClass) as? ClassDescriptor ?: return false
        val superClassDescriptor = getSymbolDescriptor(superClass) as? ClassDescriptor ?: return false
        return subClassDescriptor.isSubclassOf(superClassDescriptor)
    }

    fun isDirectSubClassOf(subClass: KaClassSymbol, superClass: KaClassSymbol): Boolean {
        val subClassDescriptor = getSymbolDescriptor(subClass) as? ClassDescriptor ?: return false
        val superClassDescriptor = getSymbolDescriptor(superClass) as? ClassDescriptor ?: return false
        return subClassDescriptor.getSuperClassOrAny() == superClassDescriptor
    }
}