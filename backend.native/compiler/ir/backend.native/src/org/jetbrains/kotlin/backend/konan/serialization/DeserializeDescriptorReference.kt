package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

class DescriptorReferenceDeserializer(val currentModule: ModuleDescriptor, val resolvedForwardDeclarations: MutableMap<UniqIdKey, UniqIdKey>) {

    fun deserializeDescriptorReference(proto: KonanIr.DescriptorReference): DeclarationDescriptor {
        val packageFqName =
            if (proto.packageFqName == "<root>") FqName.ROOT else FqName(proto.packageFqName) // TODO: whould we store an empty string in the protobuf?
        val classFqName = FqName(proto.classFqName)
        val protoIndex = if (proto.hasUniqId()) proto.uniqId.index else null

        val (clazz, members) = if (proto.classFqName == "") {
            Pair(null, currentModule.getPackage(packageFqName).memberScope.getContributedDescriptors())
        } else {
            val clazz = currentModule.findClassAcrossModuleDependencies(ClassId(packageFqName, classFqName, false))!!
            Pair(clazz, clazz.unsubstitutedMemberScope.getContributedDescriptors() + clazz.getConstructors())
        }

        if (proto.packageFqName.startsWith("cnames.") || proto.packageFqName.startsWith("objcnames.")) {
            val descriptor =
                currentModule.findClassAcrossModuleDependencies(ClassId(packageFqName, FqName(proto.name), false))!!
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

        if (proto.isEnumEntry) {
            val name = proto.name
            val memberScope = (clazz as DeserializedClassDescriptor).getUnsubstitutedMemberScope()
            return memberScope.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND)!!
        }

        if (proto.isEnumSpecial) {
            val name = proto.name
            return clazz!!.getStaticScope()
                .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).single()
        }

        members.forEach { member ->
            if (proto.isDefaultConstructor && member is ClassConstructorDescriptor) return member

            val realMembers =
                if (proto.isFakeOverride && member is CallableMemberDescriptor && member.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                    member.resolveFakeOverrideMaybeAbstract()
                else
                    setOf(member)

            val memberIndices = realMembers.map { it.getUniqId()?.index }.filterNotNull()

            if (memberIndices.contains(protoIndex)) {
                return when {
                    member is PropertyDescriptor && proto.isSetter -> member.setter!!
                    member is PropertyDescriptor && proto.isGetter -> member.getter!!
                    else -> member
                }
            }
        }
        error("Could not find serialized descriptor for index: ${proto.uniqId.index} ${proto.packageFqName},${proto.classFqName},${proto.name}")
    }
}