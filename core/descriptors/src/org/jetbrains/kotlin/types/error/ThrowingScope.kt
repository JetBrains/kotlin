/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.Printer

class ThrowingScope(kind: ErrorScopeKind, vararg formatParams: String) : ErrorScope(kind, *formatParams) {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor =
        throw IllegalStateException("$debugMessage, required name: $name")

    override fun getContributedClassifierIncludeDeprecated(
        name: Name, location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor> = throw IllegalStateException("$debugMessage, required name: $name")

    override fun getContributedVariables(name: Name, location: LookupLocation): Set<PropertyDescriptor> =
        throw IllegalStateException("$debugMessage, required name: $name")

    override fun getContributedFunctions(name: Name, location: LookupLocation): Set<SimpleFunctionDescriptor> =
        throw IllegalStateException("$debugMessage, required name: $name")

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter, nameFilter: Function1<Name, Boolean>
    ): Collection<DeclarationDescriptor> = throw IllegalStateException(debugMessage)

    override fun getFunctionNames(): Set<Name> = throw IllegalStateException()
    override fun getVariableNames(): Set<Name> = throw IllegalStateException()
    override fun getClassifierNames(): Set<Name> = throw IllegalStateException()
    override fun recordLookup(name: Name, location: LookupLocation) = throw IllegalStateException()
    override fun definitelyDoesNotContainName(name: Name): Boolean = false
    override fun toString(): String = "ThrowingScope{$debugMessage}"
    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugMessage)
    }
}
