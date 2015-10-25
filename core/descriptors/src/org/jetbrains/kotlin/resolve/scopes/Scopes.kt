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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

// see ScopeUtils.kt in the frontend module

interface LexicalScope {
    val parent: LexicalScope?

    val ownerDescriptor: DeclarationDescriptor
    val isOwnerDescriptorAccessibleByLabel: Boolean

    val implicitReceiver: ReceiverParameterDescriptor?

    /**
     * All visible descriptors from current scope possibly filtered by the given name and kind filters
     * (that means that the implementation is not obliged to use the filters but may do so when it gives any performance advantage).
     */
    fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
            nameFilter: (Name) -> Boolean = KtScope.ALL_NAME_FILTER
    ): Collection<DeclarationDescriptor>

    fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?

    fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor>

    fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor>

    fun printStructure(p: Printer)

    object Empty : LexicalScope {
        override val parent: LexicalScope?
            get() = null

        override val ownerDescriptor: DeclarationDescriptor
            get() = throw UnsupportedOperationException()

        override val isOwnerDescriptorAccessibleByLabel: Boolean
            get() = false

        override val implicitReceiver: ReceiverParameterDescriptor?
            get() = null

        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> = emptyList()

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> = emptyList()

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = emptyList()

        override fun printStructure(p: Printer) = throw UnsupportedOperationException()
    }
}

// TODO: common base interface instead direct inheritance
interface ImportingScope : LexicalScope {
    override val parent: ImportingScope?

    override val isOwnerDescriptorAccessibleByLabel: Boolean
        get() = false

    override val implicitReceiver: ReceiverParameterDescriptor?
        get() = null

    // methods getDeclaredSmth for this scope will be delegated to importScope

    fun getContributedPackage(name: Name): PackageViewDescriptor?

    fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<PropertyDescriptor>
    fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor>

    fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>): Collection<PropertyDescriptor>
    fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor>

    object Empty : ImportingScope, LexicalScope by LexicalScope.Empty {
        override fun getContributedPackage(name: Name) = null

        override fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<PropertyDescriptor> = emptyList()

        override fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor> = emptyList()

        override fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>): Collection<PropertyDescriptor> = emptyList()

        override fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor> = emptyList()
    }
}
