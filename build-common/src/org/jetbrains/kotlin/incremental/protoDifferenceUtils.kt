/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.ProtoCompareGenerated.ProtoBufClassKind
import org.jetbrains.kotlin.incremental.ProtoCompareGenerated.ProtoBufPackageKind
import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Type
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.expandedType
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.getClassId

data class Difference(
    val isClassAffected: Boolean = false,
    val areSubclassesAffected: Boolean = false,
    val changedMembersNames: Set<String> = emptySet(),
    val changedSupertypes: Set<FqName> = emptySet(),
)

sealed class ProtoData
data class ClassProtoData(val proto: ProtoBuf.Class, val nameResolver: NameResolver) : ProtoData()
data class PackagePartProtoData(val proto: ProtoBuf.Package, val nameResolver: NameResolver, val packageFqName: FqName) : ProtoData()

fun ProtoMapValue.toProtoData(packageFqName: FqName): ProtoData =
    if (isPackageFacade) {
        val [nameResolver, packageProto] = JvmProtoBufUtil.readPackageDataFrom(bytes, strings)
        PackagePartProtoData(packageProto, nameResolver, packageFqName)
    } else {
        val [nameResolver, classProto] = JvmProtoBufUtil.readClassDataFrom(bytes, strings)
        ClassProtoData(classProto, nameResolver)
    }

internal val MessageLite.isPrivate: Boolean
    get() {
        val visibility = when (this) {
            is ProtoBuf.Constructor -> Flags.VISIBILITY.get(flags)
            is ProtoBuf.Function -> Flags.VISIBILITY.get(flags)
            is ProtoBuf.Property -> Flags.VISIBILITY.get(flags)
            is ProtoBuf.TypeAlias -> Flags.VISIBILITY.get(flags)
            is ProtoBuf.EnumEntry -> return false // EnumEntry doesn't have a visibility flag
            else -> error("Unknown message: $this")
        }
        return Visibilities.isPrivate(ProtoEnumFlags.visibility(visibility))
    }

private fun MessageLite.name(nameResolver: NameResolver): String {
    return when (this) {
        is ProtoBuf.Constructor -> "<init>"
        is ProtoBuf.Function -> nameResolver.getString(name)
        is ProtoBuf.Property -> nameResolver.getString(name)
        is ProtoBuf.TypeAlias -> nameResolver.getString(name)
        is ProtoBuf.EnumEntry -> nameResolver.getString(name)
        else -> error("Unknown message: $this")
    }
}

internal fun List<MessageLite>.names(nameResolver: NameResolver): List<String> = map { it.name(nameResolver) }

abstract class DifferenceCalculator {

    abstract fun difference(): Difference

}

class DifferenceCalculatorForClass(
    private val oldData: ClassProtoData,
    private val newData: ClassProtoData,
) : DifferenceCalculator() {

    private val compareObject = ProtoCompareGenerated(
        oldNameResolver = oldData.nameResolver,
        newNameResolver = newData.nameResolver,
        oldTypeTable = oldData.proto.typeTableOrNull,
        newTypeTable = newData.proto.typeTableOrNull
    )

    override fun difference(): Difference {
        (val oldProto = proto, val oldNameResolver = nameResolver) = oldData
        (val newProto = proto, val newNameResolver = nameResolver) = newData

        val diff = compareObject.difference(oldProto, newProto)

        var isClassAffected = false
        var areSubclassesAffected = false
        val changedSupertypes = HashSet<FqName>()
        val names = hashSetOf<String>()
        val classIsSealed = newProto.isSealed && oldProto.isSealed

        fun Int.oldToNames() = names.add(oldNameResolver.getString(this))
        fun Int.newToNames() = names.add(newNameResolver.getString(this))

        fun calcDifferenceForNonPrivateMembers(members: (ProtoBuf.Class) -> List<MessageLite>): Collection<String> {
            val oldMembers = members(oldProto).filterNot { it.isPrivate }
            val newMembers = members(newProto).filterNot { it.isPrivate }
            return compareObject.calcDifferenceForMembers(oldMembers, newMembers)
        }

        for (kind in diff) {
            when (kind!!) {
                ProtoBufClassKind.COMPANION_OBJECT_NAME -> {
                    if (oldProto.hasCompanionObjectName()) oldProto.companionObjectName.oldToNames()
                    if (newProto.hasCompanionObjectName()) newProto.companionObjectName.newToNames()
                    isClassAffected = true
                }
                ProtoBufClassKind.NESTED_CLASS_NAME_LIST -> {
                    if (classIsSealed) {
                        // when class is sealed, adding an implementation can break exhaustive when expressions
                        // the workaround is to recompile all class usages
                        isClassAffected = true
                    }

                    names.addAll(calcDifferenceForNames(oldProto.nestedClassNameList, newProto.nestedClassNameList))
                }
                ProtoBufClassKind.CONSTRUCTOR_LIST -> {
                    val differentNonPrivateConstructors = calcDifferenceForNonPrivateMembers(ProtoBuf.Class::getConstructorList)
                    isClassAffected = isClassAffected || differentNonPrivateConstructors.isNotEmpty()
                }
                ProtoBufClassKind.FUNCTION_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Class::getFunctionList))
                ProtoBufClassKind.PROPERTY_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Class::getPropertyList))
                ProtoBufClassKind.TYPE_ALIAS_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Class::getTypeAliasList))
                ProtoBufClassKind.ENUM_ENTRY_LIST -> {
                    isClassAffected = true
                }
                ProtoBufClassKind.SEALED_SUBCLASS_FQ_NAME_LIST -> {
                    isClassAffected = true
                    // Subclasses are considered to be affected to fix the case where
                    // an implementation is added to an nth-level (n > 1) sealed class.
                    // In case of the following hierarchy:
                    //     Base <- Intermediate <- Impl
                    // the change of the SEALED_SUBCLASS_FQ_NAME_LIST will be detected in the Intermediate,
                    // but there can be usages, that should be rebuilt, without direct references to the Intermediate:
                    //     when (x as Base) { is Impl -> ... }
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.VERSION_REQUIREMENT_LIST,
                ProtoBufClassKind.VERSION_REQUIREMENT_TABLE,
                    -> {
                    // TODO
                }
                ProtoBufClassKind.FLAGS,
                ProtoBufClassKind.FQ_NAME,
                ProtoBufClassKind.TYPE_PARAMETER_LIST,
                    -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }

                ProtoBufClassKind.SUPERTYPE_LIST,
                ProtoBufClassKind.SUPERTYPE_ID_LIST,
                    -> {
                    isClassAffected = true
                    areSubclassesAffected = true

                    val oldTypeTable = oldProto.typeTableOrNull?.let { TypeTable(it) }
                    val newTypeTable = newProto.typeTableOrNull?.let { TypeTable(it) }

                    fun getSupertypes(supertypeList: List<Type>, nameResolver: NameResolver): List<FqName> {
                        return supertypeList.map { nameResolver.getClassId(it.className).asSingleFqName() }
                    }

                    fun getSupertypesById(supertypeIdList: List<Int>, typeTable: TypeTable?, nameResolver: NameResolver): List<FqName> {
                        return supertypeIdList.mapNotNull { id ->
                            typeTable?.get(id)?.className?.let {
                                nameResolver.getClassId(it).asSingleFqName()
                            }
                        }
                    }

                    val oldSupertypes = getSupertypes(oldProto.supertypeList, oldNameResolver)
                    val newSupertypes = getSupertypes(newProto.supertypeList, newNameResolver)

                    val oldSupertypesById = getSupertypesById(
                        oldProto.supertypeIdList,
                        oldTypeTable,
                        oldNameResolver
                    )

                    val newSupertypesById = getSupertypesById(
                        newProto.supertypeIdList,
                        newTypeTable,
                        newNameResolver
                    )

                    val changed = (oldSupertypes union newSupertypes) subtract (oldSupertypes intersect newSupertypes)
                    val changedById = (oldSupertypesById union newSupertypesById) subtract (oldSupertypesById intersect newSupertypesById)
                    val elements: Set<FqName> = changed + changedById
                    changedSupertypes.addAll(elements)
                }
                ProtoBufClassKind.JVM_EXT_CLASS_MODULE_NAME,
                ProtoBufClassKind.JVM_EXT_CLASS_LOCAL_VARIABLE_LIST,
                    -> {
                    // Not affected, local variables are not accessible outside of a file
                }
                ProtoBufClassKind.JAVA_EXT_IS_PACKAGE_PRIVATE_CLASS -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.JVM_EXT_ANONYMOUS_OBJECT_ORIGIN_NAME -> {
                    // Not affected, this extension is not used in the compiler
                }
                ProtoBufClassKind.JVM_EXT_JVM_CLASS_FLAGS -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.INLINE_CLASS_UNDERLYING_PROPERTY_NAME,
                ProtoBufClassKind.INLINE_CLASS_UNDERLYING_TYPE,
                ProtoBufClassKind.INLINE_CLASS_UNDERLYING_TYPE_ID,
                    -> {
                    isClassAffected = true
                }
                ProtoBufClassKind.CONTEXT_RECEIVER_TYPE_LIST,
                ProtoBufClassKind.CONTEXT_RECEIVER_TYPE_ID_LIST,
                    -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.COMPILER_PLUGIN_DATA_LIST -> {
                    // plugins may modify the whole hierarchy depending on metadata written by them,
                    // so we should be conservative if this metadata has changed
                    isClassAffected = true
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.ANNOTATION_LIST,
                ProtoBufClassKind.KLIB_EXT_CLASS_ANNOTATION_LIST,
                ProtoBufClassKind.BUILT_INS_EXT_CLASS_ANNOTATION_LIST,
                    -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
            }
        }

        return Difference(isClassAffected, areSubclassesAffected, names, changedSupertypes)
    }

    private fun calcDifferenceForNames(
        oldList: List<Int>,
        newList: List<Int>,
    ): Collection<String> {
        val oldNames = oldList.map { compareObject.oldNameResolver.getString(it) }.toSet()
        val newNames = newList.map { compareObject.newNameResolver.getString(it) }.toSet()

        return oldNames.union(newNames) - oldNames.intersect(newNames)
    }

    companion object {
        private val membersResolvers: List<(ProtoBuf.Class) -> List<MessageLite>> = listOf(
            // This list must match the logic in `DifferenceCalculatorForClass.difference`
            // TODO: Consider adding COMPANION_OBJECT_NAME and NESTED_CLASS_NAME_LIST as they are also members of a class (see
            // `DifferenceCalculatorForClass.difference`)
            ProtoBuf.Class::getConstructorList,
            ProtoBuf.Class::getFunctionList,
            ProtoBuf.Class::getPropertyList,
            ProtoBuf.Class::getTypeAliasList,
            ProtoBuf.Class::getEnumEntryList
        )

        fun ClassProtoData.getNonPrivateMembers(): List<String> {
            return membersResolvers.flatMap { membersResolver ->
                membersResolver(proto).filterNot { it.isPrivate }.names(nameResolver)
            }
        }
    }
}

class DifferenceCalculatorForPackageFacade(
    private val oldData: PackagePartProtoData,
    private val newData: PackagePartProtoData,
) : DifferenceCalculator() {

    private val compareObject = ProtoCompareGenerated(
        oldNameResolver = oldData.nameResolver,
        newNameResolver = newData.nameResolver,
        oldTypeTable = oldData.proto.typeTableOrNull,
        newTypeTable = newData.proto.typeTableOrNull
    )

    override fun difference(): Difference {
        val oldProto = oldData.proto
        val newProto = newData.proto

        val diff = compareObject.difference(oldProto, newProto)

        val names = hashSetOf<String>()

        fun calcDifferenceForNonPrivateMembers(members: (ProtoBuf.Package) -> List<MessageLite>): Collection<String> {
            val oldMembers = members(oldProto).filterNot { it.isPrivate }
            val newMembers = members(newProto).filterNot { it.isPrivate }
            return compareObject.calcDifferenceForMembers(oldMembers, newMembers)
        }

        for (kind in diff) {
            @Suppress("UNUSED_VARIABLE") // To make this 'when' exhaustive
            val unused: Any = when (kind!!) {
                ProtoBufPackageKind.FUNCTION_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Package::getFunctionList))
                ProtoBufPackageKind.PROPERTY_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Package::getPropertyList))
                ProtoBufPackageKind.TYPE_ALIAS_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Package::getTypeAliasList))
                ProtoBufPackageKind.VERSION_REQUIREMENT_TABLE,
                ProtoBufPackageKind.JVM_EXT_PACKAGE_MODULE_NAME,
                ProtoBufPackageKind.JVM_EXT_PACKAGE_LOCAL_VARIABLE_LIST,
                    -> {
                    // Not affected, local variables are not accessible outside of a file
                }
                ProtoBufPackageKind.BUILT_INS_EXT_PACKAGE_FQ_NAME -> {
                    // Not affected
                }
                ProtoBufPackageKind.KLIB_EXT_PACKAGE_FQ_NAME -> {
                    // Not affected
                }
            }
        }

        return Difference(changedMembersNames = names)
    }

    companion object {
        private val membersResolvers: List<(ProtoBuf.Package) -> List<MessageLite>> = listOf(
            // This list must match the logic in `DifferenceCalculatorForPackageFacade.difference`
            ProtoBuf.Package::getFunctionList,
            ProtoBuf.Package::getPropertyList,
            ProtoBuf.Package::getTypeAliasList
        )

        fun PackagePartProtoData.getNonPrivateMembers(): List<String> {
            return membersResolvers.flatMap { membersResolver ->
                membersResolver(proto).filterNot { it.isPrivate }.names(nameResolver)
            }
        }

        fun PackagePartProtoData.getVisibleTypeAliasFqNames(): List<FqName> {
            return proto.typeAliasList.filterNot { it.isPrivate }
                .map { packageFqName.child(Name.identifier(nameResolver.getString(it.name))) }
        }

        fun PackagePartProtoData.getVisibleTypeAliasExpansions(): List<Pair<ClassId, ClassId>> {
            if (proto.typeAliasList.isEmpty()) return emptyList()
            val typeTable = TypeTable(proto.typeTable)

            return proto.typeAliasList.filterNot { it.isPrivate }.mapNotNull { alias ->
                val expandedType = alias.expandedType(typeTable)
                if (!expandedType.hasClassName()) {
                    return@mapNotNull null
                }

                val aliasClassId = ClassId(packageFqName, Name.identifier(nameResolver.getString(alias.name)))
                aliasClassId to nameResolver.getClassId(expandedType.className)
            }
        }
    }
}

private fun ProtoCompareGenerated.calcDifferenceForMembers(
    oldList: List<MessageLite>,
    newList: List<MessageLite>,
): Set<String> {
    val oldProtoContext = ProtoContext(oldNameResolver, oldTypeTable)
    val newProtoContext = ProtoContext(newNameResolver, newTypeTable)

    return DifferenceCalculatorForMembers { old, new -> checkEquals(old.proto, new.proto) }.difference(
        oldList.map { ProtoMember(it, oldProtoContext) },
        newList.map { ProtoMember(it, newProtoContext) },
    )
}

class DifferenceCalculatorForPackageFacades(
    private val oldData: List<PackagePartProtoData>,
    private val newData: List<PackagePartProtoData>,
) : DifferenceCalculator() {

    private val compareObjects = HashMap<Pair<ProtoContext, ProtoContext>, ProtoCompareGenerated>()

    private val differenceCalculatorForMembers = DifferenceCalculatorForMembers { old, new -> areEqual(old, new) }

    override fun difference(): Difference {
        val oldMembers = oldData.getNonPrivateMembers()
        val newMembers = newData.getNonPrivateMembers()

        return Difference(changedMembersNames = differenceCalculatorForMembers.difference(oldMembers, newMembers))
    }

    private fun areEqual(old: ProtoMember, new: ProtoMember): Boolean =
        compareObject(old.context, new.context).checkEquals(old.proto, new.proto)

    private fun compareObject(first: ProtoContext, second: ProtoContext): ProtoCompareGenerated =
        compareObjects.getOrPut(first to second) {
            ProtoCompareGenerated(first.nameResolver, second.nameResolver, first.typeTable, second.typeTable)
        }

    private fun List<PackagePartProtoData>.getNonPrivateMembers() =
        buildList {
            this@getNonPrivateMembers.forEach { (val proto, val nameResolver) ->
                val context = ProtoContext(nameResolver, proto.typeTableOrNull)
                proto.functionList.filterNot { it.isPrivate }.forEach { function ->
                    add(ProtoMember(function, context))
                }
                proto.propertyList.filterNot { it.isPrivate }.forEach { property ->
                    add(ProtoMember(property, context))
                }
                proto.typeAliasList.filterNot { it.isPrivate }.forEach { typeAlias ->
                    add(ProtoMember(typeAlias, context))
                }
            }
        }
}

private class ProtoContext(val nameResolver: NameResolver, typeTable: ProtoBuf.TypeTable?) {
    val typeTable: ProtoBuf.TypeTable = typeTable ?: ProtoBuf.TypeTable.getDefaultInstance()

    private val stringHashCodes = HashMap<Int, Int>()
    private val classIdHashCodes = HashMap<Int, Int>()

    fun stringHashCode(index: Int): Int = stringHashCodes.getOrPut(index) { nameResolver.getString(index).hashCode() }
    fun classIdHashCode(index: Int): Int = classIdHashCodes.getOrPut(index) { nameResolver.getClassId(index).hashCode() }
    fun typeById(id: Int): ProtoBuf.Type = typeTable.getType(id)
}

private class ProtoMember(val proto: MessageLite, val context: ProtoContext) {

    val name: String get() = proto.name(context.nameResolver)

    val contentHashCode: Int = proto.getHashCode(context::stringHashCode, context::classIdHashCode, context::typeById)

    private fun MessageLite.getHashCode(stringIndexes: (Int) -> Int, fqNameIndexes: (Int) -> Int, typeTable: (Int) -> ProtoBuf.Type): Int {
        return when (this) {
            is ProtoBuf.Constructor -> hashCode(stringIndexes, fqNameIndexes, typeTable)
            is ProtoBuf.Function -> hashCode(stringIndexes, fqNameIndexes, typeTable)
            is ProtoBuf.Property -> hashCode(stringIndexes, fqNameIndexes, typeTable)
            is ProtoBuf.TypeAlias -> hashCode(stringIndexes, fqNameIndexes, typeTable)
            else -> error("Unknown message: $this")
        }
    }
}

private class DifferenceCalculatorForMembers(
    private val areEqual: (ProtoMember, ProtoMember) -> Boolean,
) {

    fun difference(oldMembers: List<ProtoMember>, newMembers: List<ProtoMember>): Set<String> {
        val result = hashSetOf<String>()

        val oldMap = oldMembers.groupBy { it.contentHashCode }
        val newMap = newMembers.groupBy { it.contentHashCode }

        val hashes = oldMap.keys + newMap.keys
        for (hash in hashes) {
            val oldBucket = oldMap[hash]
            val newBucket = newMap[hash]

            val differentMembers = when {
                newBucket == null -> oldBucket!!.map { it.name }
                oldBucket == null -> newBucket.map { it.name }
                else -> calcDifferenceForEqualHashes(oldBucket, newBucket)
            }
            result.addAll(differentMembers)
        }

        return result
    }

    private fun calcDifferenceForEqualHashes(
        oldList: List<ProtoMember>,
        newList: List<ProtoMember>,
    ): Collection<String> {
        val result = hashSetOf<String>()
        val newSet = HashSet(newList)

        oldList.forEach { oldMember ->
            val newMember = newSet.firstOrNull { areEqual(oldMember, it) }
            if (newMember != null) {
                newSet.remove(newMember)
            } else {
                result.add(oldMember.name)
            }
        }

        newSet.forEach { newMember ->
            result.add(newMember.name)
        }

        return result
    }
}

private fun ProtoCompareGenerated.checkEquals(old: MessageLite, new: MessageLite): Boolean {
    return when {
        old is ProtoBuf.Constructor && new is ProtoBuf.Constructor -> checkEquals(old, new)
        old is ProtoBuf.Function && new is ProtoBuf.Function -> checkEquals(old, new)
        old is ProtoBuf.Property && new is ProtoBuf.Property -> checkEquals(old, new)
        old is ProtoBuf.TypeAlias && new is ProtoBuf.TypeAlias -> checkEquals(old, new)
        else -> false
    }
}

private val ProtoBuf.Class.isSealed: Boolean
    get() = ProtoBuf.Modality.SEALED == Flags.MODALITY.get(flags)

internal val ProtoBuf.Class.isCompanionObject: Boolean
    get() = ProtoBuf.Class.Kind.COMPANION_OBJECT == Flags.CLASS_KIND.get(flags)

val ProtoBuf.Class.typeTableOrNull: ProtoBuf.TypeTable?
    get() = if (hasTypeTable()) typeTable else null

val ProtoBuf.Package.typeTableOrNull: ProtoBuf.TypeTable?
    get() = if (hasTypeTable()) typeTable else null

internal fun ClassProtoData.getCompanionObjectName(): String? {
    return if (proto.hasCompanionObjectName()) {
        nameResolver.getString(proto.companionObjectName)
    } else null
}

internal fun ClassProtoData.getConstants(): List<String> {
    return proto.propertyList
        .filter { Flags.IS_CONST.get(it.flags) }
        .map { nameResolver.getString(it.name) }
}
