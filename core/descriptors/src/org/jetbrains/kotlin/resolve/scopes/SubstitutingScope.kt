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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.newHashSetWithExpectedSize
import java.util.HashMap

public class SubstitutingScope(private val workerScope: JetScope, private val substitutor: TypeSubstitutor) : JetScope {

    private var substitutedDescriptors: MutableMap<DeclarationDescriptor, DeclarationDescriptor?>? = null

    private val _allDescriptors by lazy { substitute(workerScope.getDescriptors()) }

    private fun <D : DeclarationDescriptor> substitute(descriptor: D?): D? {
        if (descriptor == null) return null
        if (substitutor.isEmpty()) return descriptor

        if (substitutedDescriptors == null) {
            substitutedDescriptors = HashMap<DeclarationDescriptor, DeclarationDescriptor?>()
        }

        val substituted = substitutedDescriptors!!.getOrPut(descriptor, { descriptor.substitute(substitutor) })

        @suppress("UNCHECKED_CAST")
        return substituted as D?
    }

    private fun <D : DeclarationDescriptor> substitute(descriptors: Collection<D>): Collection<D> {
        if (substitutor.isEmpty()) return descriptors
        if (descriptors.isEmpty()) return descriptors

        val result = newHashSetWithExpectedSize<D>(descriptors.size())
        for (descriptor in descriptors) {
            val substitute = substitute(descriptor)
            if (substitute != null) {
                result.add(substitute)
            }
        }

        return result
    }

    override fun getProperties(name: Name, location: UsageLocation) = substitute(workerScope.getProperties(name, location))

    override fun getLocalVariable(name: Name) = substitute(workerScope.getLocalVariable(name))

    override fun getClassifier(name: Name, location: UsageLocation) = substitute(workerScope.getClassifier(name, location))

    override fun getFunctions(name: Name, location: UsageLocation) = substitute(workerScope.getFunctions(name, location))

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>, name: Name, location: UsageLocation): Collection<PropertyDescriptor>
            = substitute(workerScope.getSyntheticExtensionProperties(receiverTypes, name, location))

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>, name: Name, location: UsageLocation): Collection<FunctionDescriptor>
            = substitute(workerScope.getSyntheticExtensionFunctions(receiverTypes, name, location))

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>): Collection<PropertyDescriptor>
            = substitute(workerScope.getSyntheticExtensionProperties(receiverTypes))

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>): Collection<FunctionDescriptor>
            = substitute(workerScope.getSyntheticExtensionFunctions(receiverTypes))

    override fun getPackage(name: Name) = workerScope.getPackage(name)

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        throw UnsupportedOperationException() // TODO
    }

    override fun getContainingDeclaration() = workerScope.getContainingDeclaration()

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> {
        throw UnsupportedOperationException() // TODO
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean) = _allDescriptors

    override fun getOwnDeclaredDescriptors() = substitute(workerScope.getOwnDeclaredDescriptors())

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("substitutor = ")
        p.pushIndent()
        p.println(substitutor)
        p.popIndent()

        p.print("workerScope = ")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
