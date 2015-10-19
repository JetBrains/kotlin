/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.utils.Printer

// see ScopeUtils.kt in the frontend module

public interface LexicalScope {
    public val parent: LexicalScope?

    public val ownerDescriptor: DeclarationDescriptor
    public val isOwnerDescriptorAccessibleByLabel: Boolean

    public val implicitReceiver: ReceiverParameterDescriptor?

    public fun getDeclaredDescriptors(): Collection<DeclarationDescriptor>

    public fun getDeclaredClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?

    // need collection here because there may be extension property foo and usual property foo
    public fun getDeclaredVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor>
    public fun getDeclaredFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor>

    public fun printStructure(p: Printer)
}

public interface FileScope: LexicalScope {
    override val parent: LexicalScope?
        get() = null

    override val isOwnerDescriptorAccessibleByLabel: Boolean
        get() = false

    override val implicitReceiver: ReceiverParameterDescriptor?
        get() = null

    // methods getDeclaredSmth for this scope will be delegated to importScope

    fun getPackage(name: Name): PackageViewDescriptor?

    public fun getSyntheticExtensionProperties(receiverTypes: Collection<KtType>, name: Name, location: LookupLocation): Collection<PropertyDescriptor>
    public fun getSyntheticExtensionFunctions(receiverTypes: Collection<KtType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor>

    public fun getSyntheticExtensionProperties(receiverTypes: Collection<KtType>): Collection<PropertyDescriptor>
    public fun getSyntheticExtensionFunctions(receiverTypes: Collection<KtType>): Collection<FunctionDescriptor>

    public fun getDescriptors(
            kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
            nameFilter: (Name) -> Boolean = KtScope.ALL_NAME_FILTER
    ): Collection<DeclarationDescriptor>
}
