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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

public abstract class DeserializedMemberScope protected constructor(
        protected val c: DeserializationContext,
        functionList: Collection<ProtoBuf.Function>,
        propertyList: Collection<ProtoBuf.Property>
) : JetScopeImpl() {

    private data class ProtoKey(val name: Name, val isExtension: Boolean)

    private val functionProtos =
            c.storageManager.createLazyValue {
                groupByKey(filteredFunctionProtos(functionList), { it.name }) { it.hasReceiverType() }
            }
    private val propertyProtos =
            c.storageManager.createLazyValue {
                groupByKey(filteredPropertyProtos(propertyList), { it.name }) { it.hasReceiverType() }
            }

    private val functions =
            c.storageManager.createMemoizedFunction<Name, Collection<FunctionDescriptor>> { computeFunctions(it) }
    private val properties =
            c.storageManager.createMemoizedFunction<Name, Collection<VariableDescriptor>> { computeProperties(it) }

    protected open fun filteredFunctionProtos(protos: Collection<ProtoBuf.Function>): Collection<ProtoBuf.Function> = protos

    protected open fun filteredPropertyProtos(protos: Collection<ProtoBuf.Property>): Collection<ProtoBuf.Property> = protos

    private fun <M : MessageLite> groupByKey(
            protos: Collection<M>, getNameIndex: (M) -> Int, isExtension: (M) -> Boolean
    ): Map<ProtoKey, List<M>> {
        val map = LinkedHashMap<ProtoKey, MutableList<M>>()
        for (proto in protos) {
            val key = ProtoKey(c.nameResolver.getName(getNameIndex(proto)), isExtension(proto))
            map.getOrPut(key) { ArrayList(1) }.add(proto)
        }
        return map
    }

    private fun computeFunctions(name: Name): Collection<FunctionDescriptor> {
        val protos = functionProtos()[ProtoKey(name, isExtension = false)].orEmpty() +
                     functionProtos()[ProtoKey(name, isExtension = true)].orEmpty()

        val descriptors = protos.mapTo(linkedSetOf()) {
            c.memberDeserializer.loadFunction(it)
        }

        computeNonDeclaredFunctions(name, descriptors)
        return descriptors.toReadOnlyList()
    }

    protected open fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<FunctionDescriptor>) {
    }

    override fun getFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = functions(name)

    private fun computeProperties(name: Name): Collection<VariableDescriptor> {
        val protos = propertyProtos()[ProtoKey(name, isExtension = false)].orEmpty() +
                     propertyProtos()[ProtoKey(name, isExtension = true)].orEmpty()

        val descriptors = protos.mapTo(linkedSetOf()) {
            c.memberDeserializer.loadProperty(it)
        }

        computeNonDeclaredProperties(name, descriptors)
        return descriptors.toReadOnlyList()
    }

    protected open fun computeNonDeclaredProperties(name: Name, descriptors: MutableCollection<PropertyDescriptor>) {
    }

    override fun getProperties(name: Name, location: LookupLocation): Collection<VariableDescriptor> = properties.invoke(name)

    override fun getClassifier(name: Name, location: LookupLocation) = getClassDescriptor(name)

    protected abstract fun getClassDescriptor(name: Name): ClassifierDescriptor?

    protected abstract fun addClassDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean)

    override fun getContainingDeclaration() = c.containingDeclaration

    protected fun computeDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
    ): Collection<DeclarationDescriptor> {
        //NOTE: descriptors should be in the same order they were serialized in
        // see MemberComparator
        val result = LinkedHashSet<DeclarationDescriptor>(0)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.SINGLETON_CLASSIFIERS_MASK)) {
            addEnumEntryDescriptors(result, nameFilter)
        }

        addFunctionsAndProperties(result, kindFilter, nameFilter, location)

        addNonDeclaredDescriptors(result, location)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            addClassDescriptors(result, nameFilter)
        }

        return result.toReadOnlyList()
    }

    private fun addFunctionsAndProperties(
            result: LinkedHashSet<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
    ) {
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            val keys = propertyProtos().keySet().filter { nameFilter(it.name) }
            addMembers(result, keys) { getProperties(it, location) }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            val keys = functionProtos().keySet().filter { nameFilter(it.name) }
            addMembers(result, keys) { getFunctions(it, location) }
        }
    }

    private fun addMembers(
            result: MutableCollection<DeclarationDescriptor>,
            keys: Collection<ProtoKey>,
            getMembers: (Name) -> Collection<CallableDescriptor>
    ) {
        listOf(false, true).forEach { isExtension ->
            keys.filter { it.isExtension == isExtension }
                    .flatMap { getMembers(it.name) }
                    .filterTo(result) { (it.extensionReceiverParameter != null) == isExtension }
        }
    }

    protected abstract fun addNonDeclaredDescriptors(result: MutableCollection<DeclarationDescriptor>, location: LookupLocation)

    protected abstract fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean)

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        val receiver = getImplicitReceiver()
        return if (receiver != null) listOf(receiver) else listOf()
    }

    protected abstract fun getImplicitReceiver(): ReceiverParameterDescriptor?

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, " {")
        p.pushIndent()

        p.println("containingDeclaration = " + getContainingDeclaration())

        p.popIndent()
        p.println("}")
    }
}
