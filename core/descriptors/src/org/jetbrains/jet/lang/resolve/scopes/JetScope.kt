/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.scopes

import org.jetbrains.annotations.ReadOnly
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.utils.Printer

public trait JetScope {

    /**
     * Should not return object (class object or enum entry) class descriptors.
     */
    public fun getClassifier(name: Name): ClassifierDescriptor?

    public fun getPackage(name: Name): PackageViewDescriptor?

    public fun getProperties(name: Name): Collection<VariableDescriptor>

    public fun getLocalVariable(name: Name): VariableDescriptor?

    public fun getFunctions(name: Name): Collection<FunctionDescriptor>

    public fun getContainingDeclaration(): DeclarationDescriptor

    public fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor>

    /**
     * All visible descriptors from current scope.
     *
     * @return All visible descriptors from current scope.
     */
    public fun getAllDescriptors(): Collection<DeclarationDescriptor>

    /**
     * Adds receivers to the list in order of locality, so that the closest (the most local) receiver goes first
     */
    public fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor>

    public fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor>

    /**
     * Is supposed to be used in tests and debug only
     */
    public fun printScopeStructure(p: Printer)

    object Empty : JetScopeImpl() {
        override fun getContainingDeclaration(): DeclarationDescriptor {
            throw UnsupportedOperationException("Don't take containing declaration of the Empty scope")
        }

        override fun toString() = "Empty"

        override fun printScopeStructure(p: Printer) {
            p.println("Empty")
        }
    }
}

