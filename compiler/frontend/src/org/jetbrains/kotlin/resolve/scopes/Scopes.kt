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

// see utils/ScopeUtils.kt

interface HierarchicalScope : ResolutionScope {
    val parent: HierarchicalScope?

    fun printStructure(p: Printer)
}

interface LexicalScope: HierarchicalScope {
    override val parent: HierarchicalScope

    val ownerDescriptor: DeclarationDescriptor
    val isOwnerDescriptorAccessibleByLabel: Boolean

    val implicitReceiver: ReceiverParameterDescriptor?

    val kind: LexicalScopeKind

    companion object {
        fun empty(parent: HierarchicalScope, ownerDescriptor: DeclarationDescriptor): BaseLexicalScope {
            return object : BaseLexicalScope(parent, ownerDescriptor) {
                override val kind: LexicalScopeKind get() = LexicalScopeKind.EMPTY

                override fun printStructure(p: Printer) {
                    p.println("Empty lexical scope with owner = $ownerDescriptor and parent = ${parent}.")
                }
            }
        }
    }
}

enum class LexicalScopeKind {
    EMPTY,

    CLASS_HEADER,
    CLASS_INHERITANCE,
    CONSTRUCTOR_HEADER,
    PRIMARY_CONSTRUCTOR_DEFAULT_VALUE,
    CLASS_STATIC_SCOPE,
    CLASS_MEMBER_SCOPE,
    CLASS_INITIALIZER,

    PROPERTY_HEADER,
    PROPERTY_INITIALIZER_OR_DELEGATE,
    PROPERTY_ACCESSOR,
    PROPERTY_DELEGATE_METHOD,

    FUNCTION_HEADER,
    FUNCTION_INNER_SCOPE,

    CODE_BLOCK,

    LEFT_BOOLEAN_EXPRESSION,
    RIGHT_BOOLEAN_EXPRESSION,

    THEN,
    ELSE,
    DO_WHILE,
    CATCH,
    FOR,
    WHILE,
    WHEN,

    FILE,
    CALLABLE_REFERENCE,

    // for tests, KDoc & IDE
    SYNTHETIC
}

interface ImportingScope : HierarchicalScope {
    override val parent: ImportingScope?

    fun getContributedPackage(name: Name): PackageViewDescriptor?

    fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<PropertyDescriptor>
    fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor>

    fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>): Collection<PropertyDescriptor>
    fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor>

    object Empty : BaseImportingScope(null) {
        override fun printStructure(p: Printer) {
            p.println("ImportingScope.Empty")
        }
    }
}

abstract class BaseHierarchicalScope(override val parent: HierarchicalScope?) : HierarchicalScope {
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> = emptyList()

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> = emptyList()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = emptyList()
}

abstract class BaseLexicalScope(
        parent: HierarchicalScope,
        override val ownerDescriptor: DeclarationDescriptor
): BaseHierarchicalScope(parent), LexicalScope {
    override val parent: HierarchicalScope
        get() = super.parent!!

    override val isOwnerDescriptorAccessibleByLabel: Boolean
        get() = false

    override val implicitReceiver: ReceiverParameterDescriptor?
        get() = null
}

abstract class BaseImportingScope(parent: ImportingScope?) : BaseHierarchicalScope(parent), ImportingScope {
    override val parent: ImportingScope?
        get() = super.parent as ImportingScope?

    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    override fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<PropertyDescriptor> = emptyList()

    override fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor> = emptyList()

    override fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>): Collection<PropertyDescriptor> = emptyList()

    override fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor> = emptyList()
}