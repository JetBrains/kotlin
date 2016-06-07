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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.serialization.deserialization.receiverType
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

abstract class DeserializedMemberScope protected constructor(
        protected val c: DeserializationContext,
        functionList: Collection<ProtoBuf.Function>,
        propertyList: Collection<ProtoBuf.Property>,
        typeAliasList: Collection<ProtoBuf.TypeAlias>
) : MemberScopeImpl() {

    private data class ProtoKey(val name: Name, val isExtension: Boolean)

    private val functionProtos =
            c.storageManager.createLazyValue {
                groupByKey(functionList, { it.name }) { it.receiverType(c.typeTable) != null }
            }
    private val propertyProtos =
            c.storageManager.createLazyValue {
                groupByKey(propertyList, { it.name }) { it.receiverType(c.typeTable) != null }
            }
    private val typeAliasProtos =
            c.storageManager.createLazyValue {
                typeAliasList.groupBy { c.nameResolver.getName(it.name) }
            }
    protected val typeAliasNames =
            c.storageManager.createLazyValue {
                typeAliasList.map { c.nameResolver.getName(it.name) }
            }

    private val functions =
            c.storageManager.createMemoizedFunction<Name, Collection<SimpleFunctionDescriptor>> { computeFunctions(it) }
    private val properties =
            c.storageManager.createMemoizedFunction<Name, Collection<PropertyDescriptor>> { computeProperties(it) }
    private val typeAliases =
            c.storageManager.createMemoizedFunction<Name, Collection<TypeAliasDescriptor>> { computeTypeAliases(it) }

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

    private fun computeFunctions(name: Name): Collection<SimpleFunctionDescriptor> {
        val protos = functionProtos()[ProtoKey(name, isExtension = false)].orEmpty() +
                     functionProtos()[ProtoKey(name, isExtension = true)].orEmpty()

        val descriptors = protos.mapTo(linkedSetOf()) {
            c.memberDeserializer.loadFunction(it)
        }

        computeNonDeclaredFunctions(name, descriptors)
        return descriptors.toReadOnlyList()
    }

    protected open fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<SimpleFunctionDescriptor>) {
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> = functions(name)

    private fun computeProperties(name: Name): Collection<PropertyDescriptor> {
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

    private fun computeTypeAliases(name: Name): Collection<TypeAliasDescriptor> {
        val protos = typeAliasProtos()[name].orEmpty()
        val descriptors = protos.mapTo(linkedSetOf()) {
            c.memberDeserializer.loadTypeAlias(it)
        }
        computeNonDeclaredTypeAliases(name, descriptors)
        return descriptors.toReadOnlyList()
    }

    protected open fun computeNonDeclaredTypeAliases(name: Name, descriptors: MutableCollection<TypeAliasDescriptor>) {
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> = properties(name)

    protected fun getContributedTypeAliases(name: Name): Collection<TypeAliasDescriptor> = typeAliases(name)

    protected abstract fun addClassifierDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean)

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
            addClassifierDescriptors(result, nameFilter)
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
            val keys = propertyProtos().keys.filter { nameFilter(it.name) }
            addMembers(result, keys) { getContributedVariables(it, location) }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            val keys = functionProtos().keys.filter { nameFilter(it.name) }
            addMembers(result, keys) { getContributedFunctions(it, location) }
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

    protected fun addNonDeclaredDescriptors(result: MutableCollection<DeclarationDescriptor>, location: LookupLocation) {
        result.addAll(getNonDeclaredFunctionNames(location).flatMap { getContributedFunctions(it, location) })
        result.addAll(getNonDeclaredVariableNames(location).flatMap { getContributedVariables(it, location) })
    }

    protected abstract fun getNonDeclaredFunctionNames(location: LookupLocation): Set<Name>
    protected abstract fun getNonDeclaredVariableNames(location: LookupLocation): Set<Name>
    protected abstract fun getNonDeclaredTypeAliasNames(location: LookupLocation): Set<Name>

    protected abstract fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean)

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, " {")
        p.pushIndent()

        p.println("containingDeclaration = " + c.containingDeclaration)

        p.popIndent()
        p.println("}")
    }
}
