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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.compact
import java.util.*

abstract class DeserializedMemberScope protected constructor(
        protected val c: DeserializationContext,
        functionList: Collection<ProtoBuf.Function>,
        propertyList: Collection<ProtoBuf.Property>,
        typeAliasList: Collection<ProtoBuf.TypeAlias>,
        classNames: () -> Collection<Name>
) : MemberScopeImpl() {

    private val functionProtos by
            c.storageManager.createLazyValue {
                functionList.groupByName { it.name }
            }
    private val propertyProtos by
            c.storageManager.createLazyValue {
                propertyList.groupByName { it.name }
            }
    private val typeAliasProtos by
            c.storageManager.createLazyValue {
                if (c.components.configuration.typeAliasesAllowed)
                    typeAliasList.groupByName { it.name }
                else emptyMap()
            }

    private val functions =
            c.storageManager.createMemoizedFunction<Name, Collection<SimpleFunctionDescriptor>> { computeFunctions(it) }
    private val properties =
            c.storageManager.createMemoizedFunction<Name, Collection<PropertyDescriptor>> { computeProperties(it) }
    private val typeAliasByName =
            c.storageManager.createMemoizedFunctionWithNullableValues<Name, TypeAliasDescriptor> { createTypeAlias(it) }

    private val functionNamesLazy by c.storageManager.createLazyValue {
        functionProtos.keys + getNonDeclaredFunctionNames()
    }

    private val variableNamesLazy by c.storageManager.createLazyValue {
        propertyProtos.keys + getNonDeclaredVariableNames()
    }

    private val typeAliasNames: Set<Name> get() = typeAliasProtos.keys

    internal val classNames by c.storageManager.createLazyValue { classNames().toSet() }

    override fun getFunctionNames() = functionNamesLazy
    override fun getVariableNames() = variableNamesLazy
    override fun getClassifierNames(): Set<Name>? = classNames + typeAliasNames

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return name !in functionNamesLazy && name !in variableNamesLazy && name !in classNames && name !in typeAliasNames
    }

    private inline fun <M : MessageLite> Collection<M>.groupByName(
            getNameIndex: (M) -> Int
    ) = groupBy { c.nameResolver.getName(getNameIndex(it)) }

    private fun computeFunctions(name: Name) =
            computeDescriptors(
                    name,
                    functionProtos,
                    { c.memberDeserializer.loadFunction(it) },
                    { computeNonDeclaredFunctions(name, it)}
            )

    inline private fun <M : MessageLite, D : DeclarationDescriptor> computeDescriptors(
            name: Name,
            protosByName: Map<Name, Collection<M>>,
            factory: (M) -> D,
            computeNonDeclared: (MutableCollection<D>) -> Unit
    ): Collection<D> {
        val protos = protosByName[name].orEmpty()

        val descriptors = protos.mapTo(arrayListOf(), factory)

        computeNonDeclared(descriptors)
        return descriptors.compact()
    }

    protected open fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<SimpleFunctionDescriptor>) {
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        if (name !in getFunctionNames()) return emptyList()
        return functions(name)
    }

    private fun computeProperties(name: Name) =
            computeDescriptors(
                    name,
                    propertyProtos,
                    { c.memberDeserializer.loadProperty(it) },
                    { computeNonDeclaredProperties(name, it) }
            )

    protected open fun computeNonDeclaredProperties(name: Name, descriptors: MutableCollection<PropertyDescriptor>) {
    }

    private fun createTypeAlias(name: Name) =
            typeAliasProtos[name]?.singleOrNull()?.let {
                c.memberDeserializer.loadTypeAlias(it)
            }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        if (name !in getVariableNames()) return emptyList()
        return properties(name)
    }

    protected fun computeDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
    ): Collection<DeclarationDescriptor> {
        //NOTE: descriptors should be in the same order they were serialized in
        // see MemberComparator
        val result = ArrayList<DeclarationDescriptor>(0)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.SINGLETON_CLASSIFIERS_MASK)) {
            addEnumEntryDescriptors(result, nameFilter)
        }

        addFunctionsAndProperties(result, kindFilter, nameFilter, location)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            for (className in classNames) {
                if (nameFilter(className)) {
                    result.addIfNotNull(deserializeClass(className))
                }
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.TYPE_ALIASES_MASK)) {
            for (typeAliasName in typeAliasNames) {
                if (nameFilter(typeAliasName)) {
                    result.addIfNotNull(typeAliasByName(typeAliasName))
                }
            }
        }

        return result.compact()
    }

    private fun addFunctionsAndProperties(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
    ) {
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            addMembers(
                    getVariableNames(),
                    nameFilter,
                    result
            ) { getContributedVariables(it, location) }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            addMembers(
                    getFunctionNames(),
                    nameFilter,
                    result
            ) { getContributedFunctions(it, location) }
        }
    }

    private inline fun addMembers(
            names: Collection<Name>,
            nameFilter: (Name) -> Boolean,
            result: MutableCollection<DeclarationDescriptor>,
            descriptorsByName: (Name) -> Collection<DeclarationDescriptor>
    ) {
        val subResult = ArrayList<DeclarationDescriptor>()
        for (name in names) {
            if (nameFilter(name)) {
                subResult.addAll(descriptorsByName(name))
            }
        }

        subResult.sortWith(MemberComparator.NameAndTypeMemberComparator.INSTANCE)
        result.addAll(subResult)
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            when {
                hasClass(name) -> deserializeClass(name)
                name in typeAliasNames -> typeAliasByName(name)
                else -> null
            }

    private fun deserializeClass(name: Name): ClassDescriptor? =
            c.components.deserializeClass(createClassId(name))

    protected open fun hasClass(name: Name): Boolean =
            name in classNames

    protected abstract fun createClassId(name: Name): ClassId

    protected abstract fun getNonDeclaredFunctionNames(): Set<Name>
    protected abstract fun getNonDeclaredVariableNames(): Set<Name>

    protected abstract fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean)

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("containingDeclaration = " + c.containingDeclaration)

        p.popIndent()
        p.println("}")
    }
}
