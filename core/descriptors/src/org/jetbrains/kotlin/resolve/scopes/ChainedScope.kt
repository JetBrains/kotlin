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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer
import java.util.ArrayList

public open class ChainedScope(
        private val containingDeclaration: DeclarationDescriptor?/* it's nullable as a hack for TypeUtils.intersect() */,
        private val debugName: String,
        vararg scopes: JetScope
) : JetScope {
    private val scopeChain = scopes.clone()
    private var implicitReceiverHierarchy: List<ReceiverParameterDescriptor>? = null

    private inline fun getFirstMatch<T>(callback: (JetScope) -> T): T {
        // NOTE: This is performance-sensitive; please don't replace with map().firstOrNull()
        for (scope in scopeChain) {
            val result = callback(scope)
            if (result != null) return result
        }
        return null
    }

    private inline fun getFromAllScopes<T>(callback: (JetScope) -> Collection<T>): Collection<T> {
        if (scopeChain.isEmpty()) return emptySet()
        var result: Collection<T>? = null
        for (scope in scopeChain) {
            result = result.concat(callback(scope))
        }
        return result ?: emptySet()
    }

    override fun getClassifier(name: Name, location: UsageLocation): ClassifierDescriptor?
            = getFirstMatch { it.getClassifier(name, location) }

    override fun getPackage(name: Name): PackageViewDescriptor?
            = getFirstMatch { it.getPackage(name) }

    override fun getProperties(name: Name, location: UsageLocation): Collection<VariableDescriptor>
            = getFromAllScopes { it.getProperties(name, location) }

    override fun getLocalVariable(name: Name): VariableDescriptor?
            = getFirstMatch { it.getLocalVariable(name) }

    override fun getFunctions(name: Name, location: UsageLocation): Collection<FunctionDescriptor>
            = getFromAllScopes { it.getFunctions(name, location) }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>, name: Name): Collection<PropertyDescriptor>
            = getFromAllScopes { it.getSyntheticExtensionProperties(receiverTypes, name) }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>, name: Name): Collection<FunctionDescriptor>
            = getFromAllScopes { it.getSyntheticExtensionFunctions(receiverTypes, name) }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>): Collection<PropertyDescriptor>
            = getFromAllScopes { it.getSyntheticExtensionProperties(receiverTypes) }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>): Collection<FunctionDescriptor>
            = getFromAllScopes { it.getSyntheticExtensionFunctions(receiverTypes) }

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        if (implicitReceiverHierarchy == null) {
            val result = ArrayList<ReceiverParameterDescriptor>()
            scopeChain.flatMapTo(result) { it.getImplicitReceiversHierarchy() }
            result.trimToSize()
            implicitReceiverHierarchy = result
        }
        return implicitReceiverHierarchy!!
    }

    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclaration!!

    override fun getDeclarationsByLabel(labelName: Name) = scopeChain.flatMap { it.getDeclarationsByLabel(labelName) }

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = getFromAllScopes { it.getDescriptors(kindFilter, nameFilter) }

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> {
        throw UnsupportedOperationException()
    }

    override fun toString() = debugName

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), ": ", debugName, " {")
        p.pushIndent()

        for (scope in scopeChain) {
            scope.printScopeStructure(p)
        }

        p.popIndent()
        p.println("}")
    }
}
