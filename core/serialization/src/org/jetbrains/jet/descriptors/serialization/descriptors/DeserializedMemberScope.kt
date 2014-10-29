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

package org.jetbrains.jet.descriptors.serialization.descriptors

import org.jetbrains.jet.descriptors.serialization.Flags
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContextWithTypes
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.utils.Printer

import java.util.*
import org.jetbrains.jet.utils.toReadOnlyList

public abstract class DeserializedMemberScope protected(
        private val context: DeserializationContextWithTypes,
        membersList: Collection<ProtoBuf.Callable>)
: JetScope {

    private val membersProtos = context.storageManager.createLazyValue { groupByName(filteredMemberProtos(membersList)) }
    private val functions = context.storageManager.createMemoizedFunction<Name, Collection<FunctionDescriptor>> { computeFunctions(it) }
    private val properties = context.storageManager.createMemoizedFunction<Name, Collection<VariableDescriptor>> { computeProperties(it) }
    private val allDescriptors = context.storageManager.createLazyValue { computeAllDescriptors() }

    protected open fun filteredMemberProtos(allMemberProtos: Collection<ProtoBuf.Callable>): Collection<ProtoBuf.Callable> = allMemberProtos

    private fun groupByName(membersList: Collection<ProtoBuf.Callable>): Map<Name, List<ProtoBuf.Callable>> {
        val map = HashMap<Name, MutableList<ProtoBuf.Callable>>()
        for (memberProto in membersList) {
            val name = context.nameResolver.getName(memberProto.getName())
            var protos = map[name]
            if (protos == null) {
                protos = ArrayList(1)
                map.put(name, protos)
            }
            protos!!.add(memberProto)
        }
        return map
    }

    private fun <D : CallableMemberDescriptor> computeMembersByName(name: Name, callableKind: (ProtoBuf.Callable.CallableKind) -> Boolean): LinkedHashSet<D> {
        val memberProtos = membersProtos()[name] ?: return LinkedHashSet()

        val descriptors = LinkedHashSet<D>(memberProtos.size())
        for (memberProto in memberProtos) {
            if (callableKind(Flags.CALLABLE_KIND[memberProto.getFlags()])) {
                [suppress("UNCHECKED_CAST")]
                descriptors.add(context.deserializer.loadCallable(memberProto) as D)
            }
        }
        return descriptors
    }

    private fun computeFunctions(name: Name): Collection<FunctionDescriptor> {
        val descriptors = computeMembersByName<FunctionDescriptor>(name) { it == ProtoBuf.Callable.CallableKind.FUN }
        computeNonDeclaredFunctions(name, descriptors)
        return descriptors.toReadOnlyList()
    }

    protected open fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<FunctionDescriptor>) {
    }

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> = functions(name)

    private fun computeProperties(name: Name): Collection<VariableDescriptor> {
        val descriptors = computeMembersByName<PropertyDescriptor>(name) { it == ProtoBuf.Callable.CallableKind.VAL || it == ProtoBuf.Callable.CallableKind.VAR }
        computeNonDeclaredProperties(name, descriptors)
        return descriptors.toReadOnlyList()
    }

    protected open fun computeNonDeclaredProperties(name: Name, descriptors: MutableCollection<PropertyDescriptor>) {
    }

    override fun getProperties(name: Name): Collection<VariableDescriptor> = properties.invoke(name)

    override fun getClassifier(name: Name) = getClassDescriptor(name)

    protected abstract fun getClassDescriptor(name: Name): ClassifierDescriptor?

    protected abstract fun addAllClassDescriptors(result: MutableCollection<DeclarationDescriptor>)

    override fun getPackage(name: Name): PackageViewDescriptor? = null

    override fun getLocalVariable(name: Name): VariableDescriptor? = null

    override fun getContainingDeclaration() = context.containingDeclaration

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = listOf()

    private fun computeAllDescriptors(): Collection<DeclarationDescriptor> {
        val result = LinkedHashSet<DeclarationDescriptor>(0)

        for (name in membersProtos().keySet()) {
            result.addAll(getFunctions(name))
            result.addAll(getProperties(name))
        }

        addNonDeclaredDescriptors(result)

        addAllClassDescriptors(result)

        return result.toReadOnlyList()
    }

    protected abstract fun addNonDeclaredDescriptors(result: MutableCollection<DeclarationDescriptor>)

    override fun getDescriptors(kindFilter: (JetScope.DescriptorKind) -> Boolean,
                                nameFilter: (String) -> Boolean): Collection<DeclarationDescriptor> = allDescriptors.invoke()

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
