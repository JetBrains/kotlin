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
    public fun getDescriptors(kindFilter: JetScope.KindFilter = KindFilter(ALL_KINDS_MASK),
                              nameFilter: (Name) -> Boolean = ALL_NAME_FILTER): Collection<DeclarationDescriptor>

    /**
     * Adds receivers to the list in order of locality, so that the closest (the most local) receiver goes first
     */
    public fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor>

    public fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor>

    /**
     * Is supposed to be used in tests and debug only
     */
    public fun printScopeStructure(p: Printer)

    public object Empty : JetScopeImpl() {
        override fun getContainingDeclaration(): DeclarationDescriptor {
            throw UnsupportedOperationException("Don't take containing declaration of the Empty scope")
        }

        override fun toString() = "Empty"

        override fun printScopeStructure(p: Printer) {
            p.println("Empty")
        }
    }

    public trait DescriptorKindExclude {
        public fun matches(descriptor: DeclarationDescriptor): Boolean

        public object Extensions : DescriptorKindExclude {
            override fun matches(descriptor: DeclarationDescriptor)
                    = descriptor is CallableDescriptor && descriptor.getExtensionReceiverParameter() != null
        }

        public object NonExtensions : DescriptorKindExclude {
            override fun matches(descriptor: DeclarationDescriptor)
                    = descriptor !is CallableDescriptor || descriptor.getExtensionReceiverParameter() == null
        }
    }

    public data class KindFilter(
            public val kindMask: Int,
            public val excludes: List<DescriptorKindExclude> = listOf()
    ) {
        public fun accepts(descriptor: DeclarationDescriptor): Boolean
                = kindMask and descriptor.kind() != 0 && excludes.all { !it.matches(descriptor) }

        public fun acceptsKind(kinds: Int): Boolean
                = kindMask and kinds != 0

        public fun exclude(exclude: DescriptorKindExclude): KindFilter
                = KindFilter(kindMask, excludes + listOf(exclude))

        public fun withoutKind(kinds: Int): KindFilter
                = KindFilter(kindMask and kinds.inv(), excludes)

        public fun restrictedToKinds(kinds: Int): KindFilter? {
            val mask = kindMask and kinds
            if (mask == 0) return null
            return KindFilter(mask, excludes)
        }

        public val isEmpty: Boolean
            get() = kindMask == 0

        class object {
            public val ALL: KindFilter = KindFilter(ALL_KINDS_MASK)
            public val CALLABLES: KindFilter = KindFilter(FUNCTION or VARIABLE)
            public val NON_SINGLETON_CLASSIFIERS: KindFilter = KindFilter(NON_SINGLETON_CLASSIFIER)
            public val SINGLETON_CLASSIFIERS: KindFilter = KindFilter(SINGLETON_CLASSIFIER)
            public val CLASSIFIERS: KindFilter = KindFilter(CLASSIFIERS_MASK)
            public val PACKAGES: KindFilter = KindFilter(PACKAGE)
            public val FUNCTIONS: KindFilter = KindFilter(FUNCTION)
            public val VARIABLES: KindFilter = KindFilter(VARIABLE)
            public val VALUES: KindFilter = KindFilter(VALUES_MASK)
        }
    }

    class object {
        public val NON_SINGLETON_CLASSIFIER: Int = 0x01
        public val SINGLETON_CLASSIFIER: Int = 0x02
        public val PACKAGE: Int = 0x04
        public val FUNCTION: Int = 0x08
        public val VARIABLE: Int = 0x10

        public val ALL_KINDS_MASK: Int = 0x1F
        public val CLASSIFIERS_MASK: Int = NON_SINGLETON_CLASSIFIER or SINGLETON_CLASSIFIER
        public val VALUES_MASK: Int = SINGLETON_CLASSIFIER or FUNCTION or VARIABLE

        public fun DeclarationDescriptor.kind(): Int {
            return when (this) {
                is ClassDescriptor -> if (this.getKind().isSingleton()) SINGLETON_CLASSIFIER else NON_SINGLETON_CLASSIFIER
                is ClassifierDescriptor -> NON_SINGLETON_CLASSIFIER
                is PackageFragmentDescriptor, is PackageViewDescriptor -> PACKAGE
                is FunctionDescriptor -> FUNCTION
                is VariableDescriptor -> VARIABLE
                else -> 0
            }
        }

        public val ALL_NAME_FILTER: (Name) -> Boolean = { true }
    }
}

/**
 * The same as getDescriptors(kindFilter, nameFilter) but the result is guaranteed to be filtered by kind and name.
 */
public fun JetScope.getDescriptorsFiltered(
        kindFilter: JetScope.KindFilter,
        nameFilter: (Name) -> Boolean
): Collection<DeclarationDescriptor> {
    return getDescriptors(kindFilter, nameFilter).filter { kindFilter.accepts(it) && nameFilter(it.getName()) }
}



