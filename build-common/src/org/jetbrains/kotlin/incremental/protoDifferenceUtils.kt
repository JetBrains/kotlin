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

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.ProtoCompareGenerated.ProtoBufClassKind
import org.jetbrains.kotlin.incremental.ProtoCompareGenerated.ProtoBufPackageKind
import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.Deserialization
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import java.util.*

data class Difference(
        val isClassAffected: Boolean = false,
        val areSubclassesAffected: Boolean = false,
        val changedMembersNames: Set<String> = emptySet()
)

fun difference(oldData: ProtoMapValue, newData: ProtoMapValue): Difference {
    if (!oldData.isPackageFacade && newData.isPackageFacade) return Difference(isClassAffected = true, areSubclassesAffected = true)

    if (oldData.isPackageFacade && !newData.isPackageFacade) return Difference(isClassAffected = true)

    val differenceObject =
            if (oldData.isPackageFacade) {
                DifferenceCalculatorForPackageFacade(oldData, newData)
            }
            else {
                DifferenceCalculatorForClass(oldData, newData)
            }

    return differenceObject.difference()
}

internal val MessageLite.isPrivate: Boolean
    get() = Visibilities.isPrivate(Deserialization.visibility(
            when (this) {
                is ProtoBuf.Constructor -> Flags.VISIBILITY.get(flags)
                is ProtoBuf.Function -> Flags.VISIBILITY.get(flags)
                is ProtoBuf.Property -> Flags.VISIBILITY.get(flags)
                is ProtoBuf.TypeAlias -> Flags.VISIBILITY.get(flags)
                else -> error("Unknown message: $this")
            }))

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

private abstract class DifferenceCalculator() {
    protected abstract val oldNameResolver: NameResolver
    protected abstract val newNameResolver: NameResolver

    protected val compareObject by lazy { ProtoCompareGenerated(oldNameResolver, newNameResolver) }

    abstract fun difference(): Difference

    protected fun calcDifferenceForMembers(oldList: List<MessageLite>, newList: List<MessageLite>): Collection<String> {
        val result = hashSetOf<String>()

        val oldMap =
                oldList.groupBy { it.getHashCode({ compareObject.oldGetIndexOfString(it) }, { compareObject.oldGetIndexOfClassId(it) }) }
        val newMap =
                newList.groupBy { it.getHashCode({ compareObject.newGetIndexOfString(it) }, { compareObject.newGetIndexOfClassId(it) }) }

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
            }
            else {
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

    private fun MessageLite.getHashCode(stringIndexes: (Int) -> Int, fqNameIndexes: (Int) -> Int): Int {
        return when (this) {
            is ProtoBuf.Constructor -> hashCode(stringIndexes, fqNameIndexes)
            is ProtoBuf.Function -> hashCode(stringIndexes, fqNameIndexes)
            is ProtoBuf.Property -> hashCode(stringIndexes, fqNameIndexes)
            is ProtoBuf.TypeAlias -> hashCode(stringIndexes, fqNameIndexes)
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

private class DifferenceCalculatorForClass(oldData: ProtoMapValue, newData: ProtoMapValue) : DifferenceCalculator() {
    val oldClassData = JvmProtoBufUtil.readClassDataFrom(oldData.bytes, oldData.strings)
    val newClassData = JvmProtoBufUtil.readClassDataFrom(newData.bytes, newData.strings)

    val oldProto = oldClassData.classProto
    val newProto = newClassData.classProto

    override val oldNameResolver = oldClassData.nameResolver
    override val newNameResolver = newClassData.nameResolver

    val diff = compareObject.difference(oldProto, newProto)

    override fun difference(): Difference {
        var isClassAffected = false
        var areSubclassesAffected = false
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

                    if (differentNonPrivateConstructors.isNotEmpty()) {
                        isClassAffected = true
                    }
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
                    // TODO
                }
                ProtoBufClassKind.TYPE_TABLE -> {
                    // TODO
                }
                ProtoCompareGenerated.ProtoBufClassKind.SINCE_KOTLIN_INFO,
                ProtoCompareGenerated.ProtoBufClassKind.SINCE_KOTLIN_INFO_TABLE -> {
                    // TODO
                }
                ProtoBufClassKind.FLAGS,
                ProtoBufClassKind.FQ_NAME,
                ProtoBufClassKind.TYPE_PARAMETER_LIST,
                ProtoBufClassKind.SUPERTYPE_LIST,
                ProtoBufClassKind.SUPERTYPE_ID_LIST-> {
                    isClassAffected = true
                    areSubclassesAffected = true
                }
                ProtoBufClassKind.CLASS_MODULE_NAME -> {
                    // TODO
                }
            }
        }

        return Difference(isClassAffected, areSubclassesAffected, names)
    }
}

private class DifferenceCalculatorForPackageFacade(oldData: ProtoMapValue, newData: ProtoMapValue) : DifferenceCalculator() {
    val oldPackageData = JvmProtoBufUtil.readPackageDataFrom(oldData.bytes, oldData.strings)
    val newPackageData = JvmProtoBufUtil.readPackageDataFrom(newData.bytes, newData.strings)

    val oldProto = oldPackageData.packageProto
    val newProto = newPackageData.packageProto

    override val oldNameResolver = oldPackageData.nameResolver
    override val newNameResolver = newPackageData.nameResolver

    val diff = compareObject.difference(oldProto, newProto)

    override fun difference(): Difference {
        val names = hashSetOf<String>()

        fun calcDifferenceForNonPrivateMembers(members: (ProtoBuf.Package) -> List<MessageLite>): Collection<String> {
            val oldMembers = members(oldProto).filterNot { it.isPrivate }
            val newMembers = members(newProto).filterNot { it.isPrivate }
            return calcDifferenceForMembers(oldMembers, newMembers)
        }

        for (kind in diff) {
            when (kind!!) {
                ProtoBufPackageKind.FUNCTION_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Package::getFunctionList))
                ProtoBufPackageKind.PROPERTY_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Package::getPropertyList))
                ProtoBufPackageKind.TYPE_ALIAS_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Package::getTypeAliasList))
                ProtoBufPackageKind.TYPE_TABLE,
                ProtoBufPackageKind.SINCE_KOTLIN_INFO_TABLE,
                ProtoBufPackageKind.PACKAGE_MODULE_NAME -> {
                    // TODO
                }
                else -> throw IllegalArgumentException("Unsupported kind: $kind")
            }
        }

        return Difference(changedMembersNames = names)
    }
}

private val ProtoBuf.Class.isSealed: Boolean
    get() = ProtoBuf.Modality.SEALED == Flags.MODALITY.get(flags)
