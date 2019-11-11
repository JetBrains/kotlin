/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

// This is all information needed to find a descriptor in the
// tree of deserialized descriptors. Think of it as base + offset.
// packageFqName + classFqName + index allow to localize some deserialized descriptor.
// Then the rest of the fields allow to find the needed descriptor relative to the one with index.
abstract class DescriptorReferenceDeserializer(
    val currentModule: ModuleDescriptor,
    val mangler: KotlinMangler,
    val builtIns: IrBuiltIns,
    val resolvedForwardDeclarations: MutableMap<UniqId, UniqId>
) : DescriptorUniqIdAware {

    protected open fun resolveSpecialDescriptor(fqn: FqName) = builtIns.builtIns.getBuiltInClassByFqName(fqn)
    open fun checkIfSpecialDescriptorId(id: Long) = with(mangler) { id.isSpecial }

    protected open fun getDescriptorIdOrNull(descriptor: DeclarationDescriptor) =
        if (isBuiltInFunction(descriptor)) {
            val uniqName = when (descriptor) {
                is FunctionClassDescriptor -> KotlinMangler.functionClassSymbolName(descriptor.name)
                is FunctionInvokeDescriptor -> KotlinMangler.functionInvokeSymbolName(descriptor.containingDeclaration.name)
                else -> error("Unexpected descriptor type: $descriptor")
            }
            with(mangler) { uniqName.hashMangle }
        } else null

    protected fun getContributedDescriptors(packageFqName: FqName, name: String): Collection<DeclarationDescriptor> {
        val memberScope = currentModule.getPackage(packageFqName).memberScope
        return getContributedDescriptors(memberScope, name)
    }

    protected fun getContributedDescriptors(memberScope: MemberScope, name: String): Collection<DeclarationDescriptor> {
        val contributedNameString = if (name.startsWith("<get-") || name.startsWith("<set-")) {

            name.substring(5, name.length - 1) // FIXME: rework serialization format.
        } else {
            name
        }
        val contributedName = Name.identifier(contributedNameString)
        return memberScope.getContributedFunctions(contributedName, NoLookupLocation.FROM_BACKEND) +
                memberScope.getContributedVariables(contributedName, NoLookupLocation.FROM_BACKEND) +
                listOfNotNull(memberScope.getContributedClassifier(contributedName, NoLookupLocation.FROM_BACKEND))
 
    }

    protected class ClassMembers(val defaultConstructor: ClassConstructorDescriptor?,
                               val members: Map<Long, DeclarationDescriptor>,
                               val realMembers: Map<Long, DeclarationDescriptor>)

    private fun computeUniqIdIndex(descriptor: DeclarationDescriptor) = descriptor.getUniqId() ?: getDescriptorIdOrNull(descriptor)

    protected fun getMembers(members: Collection<DeclarationDescriptor>): ClassMembers {
        val allMembersMap = mutableMapOf<Long, DeclarationDescriptor>()
        val realMembersMap = mutableMapOf<Long, DeclarationDescriptor>()
        var classConstructorDescriptor: ClassConstructorDescriptor? = null
        members.forEach { member ->
            if (member is ClassConstructorDescriptor)
                classConstructorDescriptor = member
            val realMembers =
                    if (member is CallableMemberDescriptor && member.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                        member.resolveFakeOverrideMaybeAbstract()
                    else
                        setOf(member)

            computeUniqIdIndex(member)?.let { allMembersMap[it] = member }
            realMembers.mapNotNull { computeUniqIdIndex(it) }.forEach { realMembersMap[it] = member }
        }
        return ClassMembers(classConstructorDescriptor, allMembersMap, realMembersMap)
    }

    open fun deserializeDescriptorReference(
        packageFqName: FqName,
        classFqName: FqName,
        name: String,
        flags: Int,
        index: Long?
    ): DeclarationDescriptor {

        val protoIndex = index

        val (clazz, members) = if (classFqName.isRoot) {
            Pair(null, getContributedDescriptors(packageFqName, name))
        } else {
            val clazz = currentModule.findClassAcrossModuleDependencies(ClassId(packageFqName, classFqName, false))!!
            Pair(clazz, getContributedDescriptors(clazz.unsubstitutedMemberScope, name) + clazz.getConstructors())
        }

        // TODO: This is still native specific. Eliminate.
        val fqnString = packageFqName.asString()
        if (fqnString.startsWith("cnames.") || fqnString.startsWith("objcnames.")) {
            val descriptor =
                currentModule.findClassAcrossModuleDependencies(ClassId(packageFqName, FqName(name), false))!!
            if (!descriptor.fqNameUnsafe.asString().startsWith("cnames") && !descriptor.fqNameUnsafe.asString().startsWith(
                    "objcnames"
                )
            ) {
                if (descriptor is DeserializedClassDescriptor) {
                    val uniqId = UniqId(descriptor.getUniqId()!!)
                    val newKey = uniqId
                    val oldKey = UniqId(protoIndex!!)

                    resolvedForwardDeclarations.put(oldKey, newKey)
                } else {
                    /* ??? */
                }
            }
            return descriptor
        }

        if (DescriptorReferenceFlags.IS_ENUM_ENTRY.decode(flags)) {
            val memberScope = (clazz as DeserializedClassDescriptor).getUnsubstitutedMemberScope()
            return memberScope.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND)!!
        }

        if (DescriptorReferenceFlags.IS_ENUM_SPECIAL.decode(flags)) {
            return clazz!!.getStaticScope()
                .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).single()
        }

        if (DescriptorReferenceFlags.IS_TYPE_PARAMETER.decode(flags)) {

            for (m in (listOfNotNull(clazz) + members)) {
                val typeParameters = when (m) {
                    is PropertyDescriptor -> m.typeParameters
                    is ClassDescriptor -> m.declaredTypeParameters
                    is SimpleFunctionDescriptor -> m.typeParameters
                    is ClassConstructorDescriptor -> m.typeParameters
                    else -> emptyList()
                }

                typeParameters.firstOrNull { it.getUniqId() == index }?.let { return it }
            }
        }

        if (protoIndex?.let { checkIfSpecialDescriptorId(it) } == true) {
            return resolveSpecialDescriptor(packageFqName.child(Name.identifier(name)))
        }

        val membersWithIndices = getMembers(members)

        return when {
            DescriptorReferenceFlags.IS_DEFAULT_CONSTRUCTOR.decode(flags) -> membersWithIndices.defaultConstructor

            else -> {
                val map = if (DescriptorReferenceFlags.IS_FAKE_OVERRIDE.decode(flags)) membersWithIndices.realMembers else membersWithIndices.members
                map[protoIndex]?.let { member ->
                    when {
                        member is PropertyDescriptor && DescriptorReferenceFlags.IS_SETTER.decode(flags) -> member.setter!!
                        member is PropertyDescriptor && DescriptorReferenceFlags.IS_GETTER.decode(flags) -> member.getter!!
                        else -> member
                    }
                }
            }
        } ?:
        error("Could not find serialized descriptor for index: ${index} ${packageFqName},${classFqName},${name}")
    }
}
