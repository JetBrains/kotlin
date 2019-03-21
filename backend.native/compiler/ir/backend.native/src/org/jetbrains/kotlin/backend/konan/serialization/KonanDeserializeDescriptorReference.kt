package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.DescriptorReferenceDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DescriptorUniqIdAware
import org.jetbrains.kotlin.backend.common.serialization.UniqId
import org.jetbrains.kotlin.backend.common.serialization.UniqIdKey
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
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
class KonanDescriptorReferenceDeserializer(
    currentModule: ModuleDescriptor,
    resolvedForwardDeclarations: MutableMap<UniqIdKey, UniqIdKey>)
    : DescriptorReferenceDeserializer(currentModule, resolvedForwardDeclarations),
      DescriptorUniqIdAware by KonanDescriptorUniqIdAware{

    // TODO: these are dummies. Eliminate them.
    override fun resolveSpecialDescriptor(fqn: FqName): DeclarationDescriptor = currentModule
    override fun checkIfSpecialDescriptorId(id: Long): Boolean = false
    override fun getDescriptorIdOrNull(descriptor: DeclarationDescriptor): Long? = null

    // Most of this function duplicates the parent, but it deals with forward declarations.
    // TODO: Refactor me.
    override fun deserializeDescriptorReference(
        packageFqNameString: String,
        classFqNameString: String,
        name: String,
        index: Long?,
        isEnumEntry: Boolean,
        isEnumSpecial: Boolean,
        isDefaultConstructor: Boolean,
        isFakeOverride: Boolean,
        isGetter: Boolean,
        isSetter: Boolean,
        isTypeParameter: Boolean
    ): DeclarationDescriptor {
        val packageFqName = packageFqNameString.let {
            if (it == "<root>") FqName.ROOT else FqName(it)
        }// TODO: whould we store an empty string in the protobuf?

        val classFqName = FqName(classFqNameString)
        val protoIndex = index

        val (clazz, members) = if (classFqNameString == "") {
            Pair(null, getContributedDescriptors(packageFqNameString, name))
        } else {
            val clazz = currentModule.findClassAcrossModuleDependencies(ClassId(packageFqName, classFqName, false))!!
            Pair(clazz, clazz.unsubstitutedMemberScope.getContributedDescriptors() + clazz.getConstructors())
        }

        if (packageFqNameString.startsWith("cnames.") || packageFqNameString.startsWith("objcnames.")) {
            val descriptor =
                currentModule.findClassAcrossModuleDependencies(ClassId(packageFqName, FqName(name), false))!!
            if (!descriptor.fqNameUnsafe.asString().startsWith("cnames") && !descriptor.fqNameUnsafe.asString().startsWith(
                    "objcnames"
                )
            ) {
                if (descriptor is DeserializedClassDescriptor) {
                    val uniqId = UniqId(descriptor.getUniqId()!!, false)
                    val newKey = UniqIdKey(null, uniqId)
                    val oldKey = UniqIdKey(null, UniqId(protoIndex!!, false))

                    resolvedForwardDeclarations.put(oldKey, newKey)
                } else {
                    /* ??? */
                }
            }
            return descriptor
        }

        if (isEnumEntry) {
            val memberScope = (clazz as DeserializedClassDescriptor).getUnsubstitutedMemberScope()
            return memberScope.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND)!!
        }

        if (isEnumSpecial) {
            return clazz!!.getStaticScope()
                .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).single()
        }

        if (isTypeParameter) {
            return clazz!!.declaredTypeParameters.first { it.name.asString() == name }
        }

        val membersWithIndices = getMembers(members)

        return when {
            isDefaultConstructor -> membersWithIndices.defaultConstructor

            else -> {
                val map = if (isFakeOverride) membersWithIndices.realMembers else membersWithIndices.members
                map[protoIndex]?.let { member ->
                    when {
                        member is PropertyDescriptor && isSetter -> member.setter!!
                        member is PropertyDescriptor && isGetter -> member.getter!!
                        else -> member
                    }
                }
            }
        } ?: error("Could not find serialized descriptor for index: ${index} ${packageFqName},${classFqName},${name}")
    }
}
