/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.BaseImportingScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull

class LazyExplicitImportScope(
        private val packageOrClassDescriptor: DeclarationDescriptor,
        private val packageFragmentForVisibilityCheck: PackageFragmentDescriptor?,
        private val declaredName: Name,
        private val aliasName: Name,
        private val storeReferences: (Collection<DeclarationDescriptor>) -> Unit
): BaseImportingScope(null) {

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        if (name != aliasName) return null

        return when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> packageOrClassDescriptor.memberScope.getContributedClassifier(declaredName, location)
            is ClassDescriptor -> packageOrClassDescriptor.unsubstitutedInnerClassesScope.getContributedClassifier(declaredName, location)
            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedFunctions)
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedVariables)
    }

    override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        val descriptors = SmartList<DeclarationDescriptor>()

        descriptors.addIfNotNull(getContributedClassifier(aliasName, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS))
        descriptors.addAll(getContributedFunctions(aliasName, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS))
        descriptors.addAll(getContributedVariables(aliasName, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS))

        if (changeNamesForAliased && aliasName != declaredName) {
            for (i in descriptors.indices) {
                val descriptor = descriptors[i]
                val newDescriptor: DeclarationDescriptor = when (descriptor) {
                    is ClassDescriptor -> {
                        object : ClassDescriptor by descriptor {
                            override fun getName() = aliasName
                        }
                    }

                    is TypeAliasDescriptor -> {
                        object : TypeAliasDescriptor by descriptor {
                            override fun getName() = aliasName
                        }
                    }

                    is CallableMemberDescriptor -> {
                        descriptor
                                .newCopyBuilder()
                                .setName(aliasName)
                                .setOriginal(descriptor)
                                .build()!!
                    }

                    else -> error("Unknown kind of descriptor in import alias: $descriptor")
                }
                descriptors[i] = newDescriptor
            }
        }

        return descriptors
    }

    override fun computeImportedNames() = setOf(aliasName)

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", aliasName)
    }

    // should be called only once
    internal fun storeReferencesToDescriptors() = getContributedDescriptors().apply(storeReferences)

    private fun <D : CallableMemberDescriptor> collectCallableMemberDescriptors(
            location: LookupLocation,
            getDescriptors: MemberScope.(Name, LookupLocation) -> Collection<D>
    ): Collection<D> {
        val descriptors = SmartList<D>()

        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                val packageScope = packageOrClassDescriptor.memberScope
                descriptors.addAll(packageScope.getDescriptors(declaredName, location))
            }

            is ClassDescriptor -> {
                val staticClassScope = packageOrClassDescriptor.staticScope
                descriptors.addAll(staticClassScope.getDescriptors(declaredName, location))

                if (packageOrClassDescriptor.kind == ClassKind.OBJECT) {
                    descriptors.addAll(
                            packageOrClassDescriptor.unsubstitutedMemberScope.getDescriptors(declaredName, location)
                                    .mapNotNull { it.asImportedFromObjectIfPossible() }
                    )
                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        return descriptors.choseOnlyVisibleOrAll()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <D : CallableMemberDescriptor> D.asImportedFromObjectIfPossible(): D? = when (this) {
        is PropertyDescriptor -> asImportedFromObject() as D
        is FunctionDescriptor -> asImportedFromObject() as D
        else -> null
    }

    private fun <D : CallableMemberDescriptor> Collection<D>.choseOnlyVisibleOrAll() =
            filter { isVisible(it, packageFragmentForVisibilityCheck, position = QualifierPosition.IMPORT) }.
                    takeIf { it.isNotEmpty() } ?: this
}
