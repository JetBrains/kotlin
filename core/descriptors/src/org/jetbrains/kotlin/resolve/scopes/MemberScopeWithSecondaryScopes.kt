/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer

class MemberScopeWithSecondaryScope(
    private val primaryScope: MemberScope,
    private val secondaryScope: MemberScope?,
) : MemberScope {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        getContributedDescriptorsDiscriminatingSecondary { getContributedVariables(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        getContributedDescriptorsDiscriminatingSecondary { getContributedFunctions(name, location) }

    override fun getFunctionNames(): Set<Name> {
        return collectNames { getFunctionNames() }
    }

    override fun getVariableNames(): Set<Name> {
        return collectNames { getVariableNames() }
    }

    override fun getClassifierNames(): Set<Name>? {
        return collectNames { getClassifierNames() }
    }

    override fun printScopeStructure(p: Printer) {
        TODO("Not yet implemented")
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        val primaryCandidate = primaryScope.getContributedClassifier(name, location)
        if (primaryCandidate != null) return primaryCandidate

        return secondaryScope?.getContributedClassifier(name, location)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
    ): Collection<DeclarationDescriptor> = getContributedDescriptorsDiscriminatingSecondary {
        getContributedDescriptors(kindFilter, nameFilter)
    }

    private fun <T : DeclarationDescriptor> getContributedDescriptorsDiscriminatingSecondary(
        getDescriptors: MemberScope.() -> Collection<T>,
    ): Collection<T> {
        val primaryCandidates = primaryScope.getDescriptors()
        if (primaryCandidates.isNotEmpty()) return primaryCandidates

        return secondaryScope?.getDescriptors().orEmpty()
    }

    private fun collectNames(getNames: MemberScope.() -> Set<Name>?): Set<Name> {
        val result = primaryScope.getNames()?.toMutableSet() ?: mutableSetOf()

        result += secondaryScope?.getNames() ?: emptyList()

        return result
    }
}

val SECONDARY_MODULE_CAPABILITY = ModuleDescriptor.Capability<Boolean>("secondary module")

val ModuleDescriptor.isSecondaryModule: Boolean
    get() = this.getCapability(SECONDARY_MODULE_CAPABILITY) == true