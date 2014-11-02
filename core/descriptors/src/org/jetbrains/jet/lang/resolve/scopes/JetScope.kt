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
    public fun getDescriptors(kindFilterMask: Int = ALL_KINDS_MASK,
                              nameFilter: (Name) -> Boolean = { true }): Collection<DeclarationDescriptor>

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

    class object {
        public val TYPE: Int = 0x001 // class, trait ot type parameter
        public val ENUM_ENTRY: Int = 0x002
        public val OBJECT: Int = 0x004
        public val PACKAGE: Int = 0x008
        public val ORDINARY_FUNCTION: Int = 0x010 // not extension and not SAM-constructor
        public val EXTENSION_FUNCTION: Int = 0x020
        public val SAM_CONSTRUCTOR: Int = 0x040
        public val NON_EXTENSION_PROPERTY: Int = 0x080
        public val EXTENSION_PROPERTY: Int = 0x100
        public val LOCAL_VARIABLE: Int = 0x200

        public val ALL_KINDS_MASK: Int = 0xFFFF

        public val EXTENSIONS_MASK: Int = EXTENSION_FUNCTION or EXTENSION_PROPERTY
        public val FUNCTIONS_MASK: Int = ORDINARY_FUNCTION or EXTENSION_FUNCTION or SAM_CONSTRUCTOR
        public val PROPERTIES_MASK: Int = NON_EXTENSION_PROPERTY or EXTENSION_PROPERTY
        public val CALLABLES_MASK: Int = ORDINARY_FUNCTION or EXTENSION_FUNCTION or SAM_CONSTRUCTOR or NON_EXTENSION_PROPERTY or EXTENSION_PROPERTY or LOCAL_VARIABLE
        public val NON_EXTENSIONS_MASK: Int = ALL_KINDS_MASK and (EXTENSION_FUNCTION or EXTENSION_PROPERTY).inv()
        public val CLASSIFIERS_MASK: Int = TYPE or ENUM_ENTRY or OBJECT
        public val VARIABLES_AND_PROPERTIES_MASK: Int = LOCAL_VARIABLE or NON_EXTENSION_PROPERTY or EXTENSION_PROPERTY

        public val ALL_NAME_FILTER: (Name) -> Boolean = { true }
    }
}

