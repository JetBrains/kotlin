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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.CallableKind
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet

public abstract class DeserializedMemberScope protected constructor(
        protected val c: DeserializationContext,
        membersList: Collection<ProtoBuf.Callable>
) : JetScopeImpl() {

    private data class ProtoKey(val name: Name, val kind: Kind, val isExtension: Boolean)
    private enum class Kind { FUNCTION, PROPERTY }

    private fun CallableKind.toKind(): Kind {
        return when (this) {
            CallableKind.FUN -> Kind.FUNCTION
            CallableKind.VAL, CallableKind.VAR -> Kind.PROPERTY
            else -> throw IllegalStateException("Unexpected CallableKind $this")
        }
    }

    private val membersProtos =
            c.storageManager.createLazyValue { groupByKey(filteredMemberProtos(membersList)) }
    private val functions =
            c.storageManager.createMemoizedFunction<Name, Collection<FunctionDescriptor>> { computeFunctions(it) }
    private val properties =
            c.storageManager.createMemoizedFunction<Name, Collection<VariableDescriptor>> { computeProperties(it) }

    protected open fun filteredMemberProtos(allMemberProtos: Collection<ProtoBuf.Callable>): Collection<ProtoBuf.Callable> = allMemberProtos

    private fun groupByKey(membersList: Collection<ProtoBuf.Callable>): Map<ProtoKey, List<ProtoBuf.Callable>> {
        val map = LinkedHashMap<ProtoKey, MutableList<ProtoBuf.Callable>>()
        for (memberProto in membersList) {
            val key = ProtoKey(
                    c.nameResolver.getName(memberProto.getName()),
                    Flags.CALLABLE_KIND[memberProto.getFlags()].toKind(),
                    memberProto.hasReceiverType()
            )
            var protos = map[key]
            if (protos == null) {
                protos = ArrayList(1)
                map.put(key, protos)
            }
            protos.add(memberProto)
        }
        return map
    }

    private fun <D : CallableMemberDescriptor> computeMembers(name: Name, kind: Kind): LinkedHashSet<D> {
        val memberProtos = membersProtos()[ProtoKey(name, kind, isExtension = false)].orEmpty() +
                           membersProtos()[ProtoKey(name, kind, isExtension = true)].orEmpty()

        @suppress("UNCHECKED_CAST")
        return memberProtos.mapTo(LinkedHashSet<D>()) { memberProto ->
            c.memberDeserializer.loadCallable(memberProto) as D
        }
    }

    private fun computeFunctions(name: Name): Collection<FunctionDescriptor> {
        val descriptors = computeMembers<FunctionDescriptor>(name, Kind.FUNCTION)
        computeNonDeclaredFunctions(name, descriptors)
        return descriptors.toReadOnlyList()
    }

    protected open fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<FunctionDescriptor>) {
    }

    override fun getFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = functions(name)

    private fun computeProperties(name: Name): Collection<VariableDescriptor> {
        val descriptors = computeMembers<PropertyDescriptor>(name, Kind.PROPERTY)
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

    protected fun computeDescriptors(kindFilter: DescriptorKindFilter,
                                     nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        //NOTE: descriptors should be in the same order they were serialized in
        // see MemberComparator
        val result = LinkedHashSet<DeclarationDescriptor>(0)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.SINGLETON_CLASSIFIERS_MASK)) {
            addEnumEntryDescriptors(result, nameFilter)
        }

        addFunctionsAndProperties(result, kindFilter, nameFilter)

        addNonDeclaredDescriptors(result)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            addClassDescriptors(result, nameFilter)
        }

        return result.toReadOnlyList()
    }

    private fun addFunctionsAndProperties(
            result: LinkedHashSet<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ) {
        val acceptsProperties = kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)
        val acceptsFunctions = kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)
        if (!(acceptsFunctions || acceptsProperties)) {
            return
        }

        val keys = membersProtos().keySet().filter { nameFilter(it.name) }
        if (acceptsProperties) {
            addMembers(result, keys, Kind.PROPERTY) { getProperties(it) }
        }
        if (acceptsFunctions) {
            addMembers(result, keys, Kind.FUNCTION) { getFunctions(it) }
        }
    }

    private fun addMembers(
            result: MutableCollection<DeclarationDescriptor>,
            keys: Collection<ProtoKey>,
            kind: Kind,
            getMembers: (Name) -> Collection<CallableDescriptor>
    ) {
        val filteredByKind = keys.filter { it.kind == kind }
        listOf(false, true).forEach { isExtension ->
            filteredByKind.filter { it.isExtension == isExtension }
                    .flatMap { getMembers(it.name) }
                    .filterTo(result) { (it.getExtensionReceiverParameter() != null) == isExtension }
        }
    }

    protected abstract fun addNonDeclaredDescriptors(result: MutableCollection<DeclarationDescriptor>)

    protected abstract fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean)

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        val receiver = getImplicitReceiver()
        return if (receiver != null) listOf(receiver) else listOf()
    }

    protected abstract fun getImplicitReceiver(): ReceiverParameterDescriptor?

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("containingDeclaration = " + getContainingDeclaration())

        p.popIndent()
        p.println("}")
    }
}
