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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.utils.takeSnapshot
import org.jetbrains.kotlin.util.collectionUtils.getFirstMatch
import org.jetbrains.kotlin.util.collectionUtils.getFromAllScopes
import org.jetbrains.kotlin.utils.Printer

public class LexicalChainedScope(
        parent: LexicalScope,
        override val ownerDescriptor: DeclarationDescriptor,
        override val isOwnerDescriptorAccessibleByLabel: Boolean,
        override val implicitReceiver: ReceiverParameterDescriptor?,
        private val debugName: String,
        vararg memberScopes: KtScope, // todo JetScope -> MemberScope
        @Deprecated("This value is temporary hack for resolve -- don't use it!")
        val isStaticScope: Boolean = false
): LexicalScope {
    override val parent = parent.takeSnapshot()
    private val scopeChain = memberScopes.clone()

    override fun getDeclaredDescriptors() = getFromAllScopes(scopeChain) { it.getAllDescriptors() }

    override fun getDeclaredClassifier(name: Name, location: LookupLocation) = getFirstMatch(scopeChain) { it.getClassifier(name, location) }

    override fun getDeclaredVariables(name: Name, location: LookupLocation) = getFromAllScopes(scopeChain) { it.getProperties(name, location) }

    override fun getDeclaredFunctions(name: Name, location: LookupLocation) = getFromAllScopes(scopeChain) { it.getFunctions(name, location) }

    override fun toString(): String = debugName

    override fun printStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugName, "; for descriptor: ", ownerDescriptor.name,
                  " with implicitReceiver: ", implicitReceiver?.value ?: "NONE", " {")
        p.pushIndent()

        for (scope in scopeChain) {
            scope.printScopeStructure(p)
        }

        p.print("parent = ")
        parent.printStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }

}