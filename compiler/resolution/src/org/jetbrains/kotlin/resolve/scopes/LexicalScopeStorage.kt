/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.utils.takeSnapshot
import org.jetbrains.kotlin.utils.SmartList
import java.util.*

interface LocalRedeclarationChecker {
    fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor)

    object DO_NOTHING : LocalRedeclarationChecker {
        override fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor) {}
    }
}

abstract class LexicalScopeStorage(
        parent: HierarchicalScope,
        val redeclarationChecker: LocalRedeclarationChecker
): LexicalScope {
    override val parent = parent.takeSnapshot()

    protected val addedDescriptors: MutableList<DeclarationDescriptor> = SmartList()

    private var functionsByName: MutableMap<Name, IntList>? = null
    private var variablesAndClassifiersByName: MutableMap<Name, IntList>? = null

    override fun getContributedClassifier(name: Name, location: LookupLocation) = variableOrClassDescriptorByName(name) as? ClassifierDescriptor
    override fun getContributedVariables(name: Name, location: LookupLocation) = listOfNotNull(variableOrClassDescriptorByName(name) as? VariableDescriptor)

    override fun getContributedFunctions(name: Name, location: LookupLocation) = functionsByName(name)

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = addedDescriptors

    protected fun addVariableOrClassDescriptor(descriptor: DeclarationDescriptor) {
        val name = descriptor.name
        if (name.isSpecial) return
        val descriptorIndex = addDescriptor(descriptor)

        if (variablesAndClassifiersByName == null) {
            variablesAndClassifiersByName = HashMap()
        }
        //TODO: could not use += because of KT-8050
        variablesAndClassifiersByName!![name] = variablesAndClassifiersByName!![name] + descriptorIndex

    }

    protected fun addFunctionDescriptorInternal(functionDescriptor: FunctionDescriptor) {
        val name = functionDescriptor.name
        val descriptorIndex = addDescriptor(functionDescriptor)
        if (functionsByName == null) {
            functionsByName = HashMap(1)
        }
        //TODO: could not use += because of KT-8050
        functionsByName!![name] = functionsByName!![name] + descriptorIndex
    }

    protected fun variableOrClassDescriptorByName(name: Name, descriptorLimit: Int = addedDescriptors.size): DeclarationDescriptor? {
        if (descriptorLimit == 0) return null

        var list = variablesAndClassifiersByName?.get(name)
        while (list != null) {
            val descriptorIndex = list.last
            if (descriptorIndex < descriptorLimit) {
                return descriptorIndex.descriptorByIndex()
            }
            list = list.prev
        }
        return null
    }

    protected fun functionsByName(name: Name, descriptorLimit: Int = addedDescriptors.size): List<FunctionDescriptor> {
        if (descriptorLimit == 0) return emptyList()

        var list = functionsByName?.get(name)
        while (list != null) {
            if (list.last < descriptorLimit) {
                return list.toDescriptors<FunctionDescriptor>()
            }
            list = list.prev
        }
        return emptyList()
    }

    private fun addDescriptor(descriptor: DeclarationDescriptor): Int {
        redeclarationChecker.checkBeforeAddingToScope(this, descriptor)
        addedDescriptors.add(descriptor)
        return addedDescriptors.size - 1
    }

    private class IntList(val last: Int, val prev: IntList?)

    private fun Int.descriptorByIndex() = addedDescriptors[this]

    private operator fun IntList?.plus(value: Int) = IntList(value, this)

    private fun <TDescriptor: DeclarationDescriptor> IntList.toDescriptors(): List<TDescriptor> {
        val result = ArrayList<TDescriptor>(1)
        var rest: IntList? = this
        do {
            result.add(rest!!.last.descriptorByIndex() as TDescriptor)
            rest = rest.prev
        } while (rest != null)
        return result
    }

    override fun definitelyDoesNotContainName(name: Name, location: LookupLocation) =
            functionsByName?.get(name) == null && variablesAndClassifiersByName?.get(name) == null
}
