/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.atMostOne

class SimpleMemberScope(val members: List<DeclarationDescriptor>) : MemberScopeImpl() {

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        members.filterIsInstance<ClassifierDescriptor>()
            .atMostOne { it.name == name }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        members.filterIsInstance<PropertyDescriptor>()
            .filter { it.name == name }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        members.filterIsInstance<SimpleFunctionDescriptor>()
            .filter { it.name == name }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> =
        members.filter { kindFilter.accepts(it) && nameFilter(it.name) }

    override fun printScopeStructure(p: Printer) = TODO("not implemented")
}
