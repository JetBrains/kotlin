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
    public fun getAllDescriptors(): Collection<DeclarationDescriptor> = getDescriptors()

    /**
     * All visible descriptors from current scope possibly filtered by the given name and kind filters
     * (that means that the implementation is not obliged to use the filters but may do so when it gives any performance advantage).
     */
    public fun getDescriptors(kindFilter: (JetScope.DescriptorKind) -> Boolean = DescriptorKind.ALL,
                              nameFilter: (String) -> kotlin.Boolean = { true }): Collection<DeclarationDescriptor>

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

    public enum class DescriptorKind {
        CLASSIFIER
        PACKAGE
        NON_EXTENSION_FUNCTION
        EXTENSION_FUNCTION
        NON_EXTENSION_PROPERTY
        EXTENSION_PROPERTY
        LOCAL_VARIABLE

        class object {
            public val ALL: (DescriptorKind) -> Boolean = { true }
            public val EXTENSIONS: (DescriptorKind) -> Boolean = { it == EXTENSION_FUNCTION || it == EXTENSION_PROPERTY }
            public val FUNCTIONS: (DescriptorKind) -> Boolean = { it == NON_EXTENSION_FUNCTION || it == EXTENSION_FUNCTION }
            public val CALLABLES: (DescriptorKind) -> Boolean = { it != CLASSIFIER && it != PACKAGE }
            public val NON_EXTENSION_CALLABLES: (DescriptorKind) -> Boolean = { it == NON_EXTENSION_FUNCTION || it == NON_EXTENSION_PROPERTY || it == LOCAL_VARIABLE }
            public val NON_EXTENSIONS: (DescriptorKind) -> Boolean = { it != EXTENSION_FUNCTION && it != EXTENSION_PROPERTY }
            public val CLASSIFIERS: (DescriptorKind) -> Boolean = { it == CLASSIFIER }
            public val PACKAGES: (DescriptorKind) -> Boolean = { it == PACKAGE }
            public val VARIABLES_AND_PROPERTIES: (DescriptorKind) -> Boolean = { it == LOCAL_VARIABLE || it == NON_EXTENSION_PROPERTY || it == EXTENSION_PROPERTY }
        }
    }

    class object {
        public val ALL_NAME_FILTER: (String) -> Boolean = { true }
    }
}

