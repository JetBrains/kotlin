/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.incremental.ProtoCompareGenerated.ProtoBufClassKind
import org.jetbrains.kotlin.incremental.ProtoCompareGenerated.ProtoBufPackageKind
import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptorVisibility
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import java.util.*

data class Difference(
    val isClassAffected: Boolean = false,
    val areSubclassesAffected: Boolean = false,
    val changedMembersNames: Set<String> = emptySet(),
    val changedSupertypes: Set<FqName> = emptySet()
)

sealed class ProtoData
data class ClassProtoData(val proto: ProtoBuf.Class, val nameResolver: NameResolver) : ProtoData()
data class PackagePartProtoData(val proto: ProtoBuf.Package, val nameResolver: NameResolver, val packageFqName: FqName) : ProtoData()

fun ProtoMapValue.toProtoData(packageFqName: FqName): ProtoData =
    if (isPackageFacade) {
        val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(bytes, strings)
        PackagePartProtoData(packageProto, nameResolver, packageFqName)
    } else {
        val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(bytes, strings)
        ClassProtoData(classProto, nameResolver)
    }

internal val MessageLite.isPrivate: Boolean
    get() = DescriptorVisibilities.isPrivate(
        ProtoEnumFlags.descriptorVisibility(
            when (this) {
                is ProtoBuf.Constructor -> Flags.VISIBILITY.get(flags)
                is ProtoBuf.Function -> Flags.VISIBILITY.get(flags)
                is ProtoBuf.Property -> Flags.VISIBILITY.get(flags)
                is ProtoBuf.TypeAlias -> Flags.VISIBILITY.get(flags)
                else -> error("Unknown message: $this")
            }
        )
    )

private fun MessageLite.name(nameResolver: NameResolver): String {
    return when (this) {
        is ProtoBuf.Constructor -> "<init>"
        is ProtoBuf.Function -> nameResolver.getString(name)
        is ProtoBuf.Property -> nameResolver.getString(name)
        is ProtoBuf.TypeAlias -> nameResolver.getString(name)
        else -> error("Unknown message: $this")
    }
}

internal fun List<MessageLite>.names(nameResolver: NameResolver): List<String> = map { it.name(nameResolver) }

abstract class DifferenceCalculator {
    protected abstract val compareObject: ProtoCompareGenerated

    abstract fun difference(): Difference

    protected fun calcDifferenceForMembers(oldList: List<MessageLite>, newList: List<MessageLite>): Collection<String> {
        val result = hashSetOf<String>()

        val oldMap =
            oldList.groupBy {
                it.getHashCode(
                    compareObject::oldGetIndexOfString,
                    compareObject::oldGetIndexOfClassId,
                    compareObject::oldGetTypeById
                )
            }
        val newMap =
            newList.groupBy {
                it.getHashCode(
                    compareObject::newGetIndexOfString,
                    compareObject::newGetIndexOfClassId,
                    compareObject::newGetTypeById
                )
            }

        val hashes = oldMap.keys + newMap.keys
        for (hash in hashes) {
            val oldMembers = oldMap[hash]
            val newMembers = newMap[hash]

            val differentMembers = when {
                newMembers == null -> oldMembers!!.names(compareObject.oldNameResolver)
                oldMembers == null -> newMembers.names(compareObject.newNameResolver)
                else -> calcDifferenceForEqualHashes(oldMembers, newMembers)
            }
            result.addAll(differentMembers)
        }

        return result
    }

    private fun calcDifferenceForEqualHashes(
        oldList: List<MessageLite>,
        newList: List<MessageLite>
    ): Collection<String> {
        val result = hashSetOf<String>()
        val newSet = HashSet(newList)

        oldList.forEach { oldMember ->
            val newMember = newSet.firstOrNull { compareObject.checkEquals(oldMember, it) }
            if (newMember != null) {
                newSet.remove(newMember)
            } else {
                result.add(oldMember.name(compareObject.oldNameResolver))
            }
        }

        newSet.forEach { newMember ->
            result.add(newMember.name(compareObject.newNameResolver))
        }

        return result
    }

    protected fun calcDifferenceForNames(
        oldList: List<Int>,
        newList: List<Int>
    ): Collection<String> {
        val oldNames = oldList.map { compareObject.oldNameResolver.getString(it) }.toSet()
        val newNames = newList.map { compareObject.newNameResolver.getString(it) }.toSet()
        return oldNames.union(newNames) - oldNames.intersect(newNames)
    }

    private fun MessageLite.getHashCode(stringIndexes: (Int) -> Int, fqNameIndexes: (Int) -> Int, typeTable: (Int) -> ProtoBuf.Type): Int {
        return when (this) {
            is ProtoBuf.Constructor -> hashCode(stringIndexes, fqNameIndexes, typeTable)
            is ProtoBuf.Function -> hashCode(stringIndexes, fqNameIndexes, typeTable)
            is ProtoBuf.Property -> hashCode(stringIndexes, fqNameIndexes, typeTable)
            is ProtoBuf.TypeAlias -> hashCode(stringIndexes, fqNameIndexes, typeTable)
            else -> error("Unknown message: $this")
        }
    }

    private fun ProtoCompareGenerated.checkEquals(old: MessageLite, new: MessageLite): Boolean {
        return when {
            old is ProtoBuf.Constructor && new is ProtoBuf.Constructor -> checkEquals(old, new)
            old is ProtoBuf.Function && new is ProtoBuf.Function -> checkEquals(old, new)
            old is ProtoBuf.Property && new is ProtoBuf.Property -> checkEquals(old, new)
            old is ProtoBuf.TypeAlias && new is ProtoBuf.TypeAlias -> checkEquals(old, new)
            else -> error("Unknown message: $this")
        }
    }
}

class DifferenceCalculatorForClass(
    private val oldData: ClassProtoData,
    private val newData: ClassProtoData
) : DifferenceCalculator() {
    override val compareObject = ProtoCompareGenerated(
        oldNameResolver = oldData.nameResolver,
        newNameResolver = newData.nameResolver,
        oldTypeTable = oldData.proto.typeTableOrNull,
        newTypeTable = newData.proto.typeTableOrNull
    )

    override fun difference(): Difference {
        val (oldProto, oldNameResolver) = oldData
        val (newProto, newNameResolver) = newData

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
            return calcDifferenceForMembers(oldMembers, newMembers)
        }

        for (kind in diff) {
            @Suppress("UNUSED_VARIABLE") // To make this 'when' exhaustive
            val unused: Any = when (kind!!) {
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
                ProtoBufClassKind.VERSION_REQUIREMENT_TABLE -> {
                    // TODO
                }
                ProtoBufClassKind.FLAGS,
                ProtoBufClassKind.FQ_NAME,
                ProtoBufClassKind.TYPE_PARAMETER_LIST,
                ProtoBufClassKind.JS_EXT_CLASS_ANNOTATION_LIST -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }

                ProtoBufClassKind.SUPERTYPE_LIST,
                ProtoBufClassKind.SUPERTYPE_ID_LIST -> {
                    isClassAffected = true
                    areSubclassesAffected = true

                    val oldSupertypes = oldProto.supertypeList.map { oldNameResolver.getClassId(it.className).asSingleFqName() }
                    val newSupertypes = newProto.supertypeList.map { newNameResolver.getClassId(it.className).asSingleFqName() }
                    val changed = (oldSupertypes union newSupertypes) subtract (oldSupertypes intersect newSupertypes)
                    changedSupertypes.addAll(changed)
                }
                ProtoBufClassKind.JVM_EXT_CLASS_MODULE_NAME,
                ProtoBufClassKind.JS_EXT_CLASS_CONTAINING_FILE_ID -> {
                    // TODO
                }
                ProtoBufClassKind.JVM_EXT_CLASS_LOCAL_VARIABLE_LIST -> {
                    // Not affected, local variables are not accessible outside of a file
                }
                ProtoBufClassKind.JAVA_EXT_IS_PACKAGE_PRIVATE_CLASS -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.BUILT_INS_EXT_CLASS_ANNOTATION_LIST -> {
                    isClassAffected = true
                }
                ProtoBufClassKind.JVM_EXT_ANONYMOUS_OBJECT_ORIGIN_NAME -> {
                    // Not affected, this extension is not used in the compiler
                }
                ProtoBufClassKind.KLIB_EXT_CLASS_ANNOTATION_LIST -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.JVM_EXT_JVM_CLASS_FLAGS -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.INLINE_CLASS_UNDERLYING_PROPERTY_NAME,
                ProtoBufClassKind.INLINE_CLASS_UNDERLYING_TYPE,
                ProtoBufClassKind.INLINE_CLASS_UNDERLYING_TYPE_ID -> {
                    isClassAffected = true
                }
                ProtoBufClassKind.CONTEXT_RECEIVER_TYPE_LIST,
                ProtoBufClassKind.CONTEXT_RECEIVER_TYPE_ID_LIST -> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
            }
        }

        return Difference(isClassAffected, areSubclassesAffected, names, changedSupertypes)
    }
}

class DifferenceCalculatorForPackageFacade(
    private val oldData: PackagePartProtoData,
    private val newData: PackagePartProtoData
) : DifferenceCalculator() {
    override val compareObject = ProtoCompareGenerated(
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
            return calcDifferenceForMembers(oldMembers, newMembers)
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
                ProtoBufPackageKind.JS_EXT_PACKAGE_FQ_NAME -> {
                    // TODO
                }
                ProtoBufPackageKind.JVM_EXT_PACKAGE_LOCAL_VARIABLE_LIST -> {
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
}

private val ProtoBuf.Class.isSealed: Boolean
    get() = ProtoBuf.Modality.SEALED == Flags.MODALITY.get(flags)

val ProtoBuf.Class.typeTableOrNull: ProtoBuf.TypeTable?
    get() = if (hasTypeTable()) typeTable else null

val ProtoBuf.Package.typeTableOrNull: ProtoBuf.TypeTable?
    get() = if (hasTypeTable()) typeTable else null
