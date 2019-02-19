package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

// This is all information needed to find a descriptor in the
// tree of deserialized descriptors. Think of it as base + offset.
// packageFqName + classFqName + index allow to localize some deserialized descriptor.
// Then the rest of the fields allow to find the needed descriptor relative to the one with index.
class DescriptorReferenceDeserializer(val currentModule: ModuleDescriptor, val resolvedForwardDeclarations: MutableMap<UniqIdKey, UniqIdKey>) {

    private val cache = mutableMapOf<String, Collection<DeclarationDescriptor>>()

    private fun getContributedDescriptors(packageFqNameString: String): Collection<DeclarationDescriptor> =
            cache.getOrPut(packageFqNameString) {
                val packageFqName = packageFqNameString.let {
                    if (it == "<root>") FqName.ROOT else FqName(it)
                }// TODO: whould we store an empty string in the protobuf?

                currentModule.getPackage(packageFqName).memberScope.getContributedDescriptors()
            }

    private data class ClassName(val packageFqName: String, val classFqName: String)

    private class ClassMembers(val defaultConstructor: ClassConstructorDescriptor?,
                               val members: Map<Long, DeclarationDescriptor>,
                               val realMembers: Map<Long, DeclarationDescriptor>)

    private val membersCache = mutableMapOf<ClassName, ClassMembers>()

    private fun getMembers(packageFqNameString: String, classFqNameString: String,
                           members: Collection<DeclarationDescriptor>): ClassMembers =
            membersCache.getOrPut(ClassName(packageFqNameString, classFqNameString)) {
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

                    member.getUniqId()?.index?.let { allMembersMap[it] = member }
                    realMembers.mapNotNull { it.getUniqId()?.index }.forEach { realMembersMap[it] = member }
                }
                ClassMembers(classConstructorDescriptor, allMembersMap, realMembersMap)
            }

    fun deserializeDescriptorReference(
        packageFqNameString: String,
        classFqNameString: String,
        name: String,
        index: Long?,
        isEnumEntry: Boolean = false,
        isEnumSpecial: Boolean = false,
        isDefaultConstructor: Boolean = false,
        isFakeOverride: Boolean = false,
        isGetter: Boolean = false,
        isSetter: Boolean = false
    ): DeclarationDescriptor {
        val packageFqName = packageFqNameString.let {
            if (it == "<root>") FqName.ROOT else FqName(it)
        }// TODO: whould we store an empty string in the protobuf?

        val classFqName = FqName(classFqNameString)
        val protoIndex = index

        val (clazz, members) = if (classFqNameString == "") {
            Pair(null, getContributedDescriptors(packageFqNameString))
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
                    val uniqId = UniqId(descriptor.getUniqId()!!.index, false)
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

        val membersWithIndices = getMembers(packageFqNameString, classFqNameString, members)

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