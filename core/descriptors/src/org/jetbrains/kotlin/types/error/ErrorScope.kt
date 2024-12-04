/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.Printer

open class ErrorScope(val kind: ErrorScopeKind, vararg formatParams: String) : MemberScope {
    protected val debugMessage = kind.debugMessage.format(*formatParams)

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor =
        ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format(name)))

    override fun getContributedClassifierIncludeDeprecated(
        name: Name, location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Set<PropertyDescriptor> = ErrorUtils.errorPropertyGroup

    override fun getContributedFunctions(name: Name, location: LookupLocation): Set<SimpleFunctionDescriptor> =
        setOf(ErrorFunctionDescriptor(ErrorUtils.errorClass))

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter, nameFilter: Function1<Name, Boolean>
    ): Collection<DeclarationDescriptor> = emptyList()

    override fun getFunctionNames(): Set<Name> = emptySet()
    override fun getVariableNames(): Set<Name> = emptySet()
    override fun getClassifierNames(): Set<Name> = emptySet()

    override fun recordLookup(name: Name, location: LookupLocation) {}
    override fun definitelyDoesNotContainName(name: Name): Boolean = false

    override fun toString(): String = "ErrorScope{$debugMessage}"

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugMessage)
    }
}