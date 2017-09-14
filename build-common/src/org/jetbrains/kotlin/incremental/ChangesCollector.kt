/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver

class ChangesCollector {
    private val removedMembers = hashMapOf<FqName, MutableSet<String>>()
    private val changedMembers = hashMapOf<FqName, MutableSet<String>>()
    private val areSubclassesAffected = hashMapOf<FqName, Boolean>()

    fun changes(): List<ChangeInfo> {
        val changes = arrayListOf<ChangeInfo>()

        for ((fqName, members) in removedMembers) {
            if (members.isNotEmpty()) {
                changes.add(ChangeInfo.Removed(fqName, members))
            }
        }

        for ((fqName, members) in changedMembers) {
            if (members.isNotEmpty()) {
                changes.add(ChangeInfo.MembersChanged(fqName, members))
            }
        }

        for ((fqName, areSubclassesAffected) in areSubclassesAffected) {
            changes.add(ChangeInfo.SignatureChanged(fqName, areSubclassesAffected))
        }

        return changes
    }

    private fun <T, R> MutableMap<T, MutableSet<R>>.getSet(key: T) =
            getOrPut(key) { HashSet() }

    private fun collectChangedMember(scope: FqName, name: String) {
        changedMembers.getSet(scope).add(name)
    }

    private fun collectRemovedMember(scope: FqName, name: String) {
        removedMembers.getSet(scope).add(name)
    }

    private fun collectChangedMembers(scope: FqName, names: Collection<String>) {
        if (names.isNotEmpty()) {
            changedMembers.getSet(scope).addAll(names)
        }
    }

    private fun collectRemovedMembers(scope: FqName, names: Collection<String>) {
        if (names.isNotEmpty()) {
            removedMembers.getSet(scope).addAll(names)
        }
    }

    fun collectProtoChanges(oldData: ProtoData?, newData: ProtoData?) {
        if (oldData == null && newData == null) {
            throw IllegalStateException("Old and new value are null")
        }

        if (oldData == null) {
            newData!!.collectAll(isRemoved = false)
            return
        }

        if (newData == null) {
            oldData.collectAll(isRemoved = true)
            return
        }

        when (oldData) {
            is ClassProtoData -> {
                when (newData) {
                    is ClassProtoData -> {
                        val fqName = oldData.nameResolver.getClassId(oldData.proto.fqName).asSingleFqName()
                        val diff = DifferenceCalculatorForClass(oldData, newData).difference()
                        if (diff.isClassAffected) {
                            collectSignature(oldData, diff.areSubclassesAffected)
                        }
                        collectChangedMembers(fqName, diff.changedMembersNames)
                    }
                    is PackagePartProtoData -> {
                        collectSignature(oldData, areSubclassesAffected = true)
                    }
                }
            }
            is PackagePartProtoData -> {
                when (newData) {
                    is ClassProtoData -> {
                        collectSignature(newData, areSubclassesAffected = false)
                    }
                    is PackagePartProtoData -> {
                        val diff = DifferenceCalculatorForPackageFacade(oldData, newData).difference()
                        collectChangedMembers(oldData.packageFqName, diff.changedMembersNames)
                    }
                }
            }
        }
    }

    private fun <T> T.getNonPrivateNames(nameResolver: NameResolver, vararg members: T.() -> List<MessageLite>): Set<String> =
            members.flatMap { this.it().filterNot { it.isPrivate }.names(nameResolver) }.toSet()

    private fun ProtoData.collectAll(isRemoved: Boolean) =
        when (this) {
            is PackagePartProtoData -> collectAllFromPackage(isRemoved)
            is ClassProtoData -> collectAllFromClass(isRemoved)
        }

    private fun PackagePartProtoData.collectAllFromPackage(isRemoved: Boolean) {
        val memberNames =
                proto.getNonPrivateNames(
                        nameResolver,
                        ProtoBuf.Package::getFunctionList,
                        ProtoBuf.Package::getPropertyList
                )

        if (isRemoved) {
            collectRemovedMembers(packageFqName, memberNames)
        }
        else {
            collectChangedMembers(packageFqName, memberNames)
        }
    }

    private fun ClassProtoData.collectAllFromClass(isRemoved: Boolean) {
        val classFqName = nameResolver.getClassId(proto.fqName).asSingleFqName()
        val kind = Flags.CLASS_KIND.get(proto.flags)

        if (kind == ProtoBuf.Class.Kind.COMPANION_OBJECT) {
            val memberNames =
                    proto.getNonPrivateNames(
                            nameResolver,
                            ProtoBuf.Class::getConstructorList,
                            ProtoBuf.Class::getFunctionList,
                            ProtoBuf.Class::getPropertyList
                    ) + proto.enumEntryList.map { nameResolver.getString(it.name) }

            val collectMember = if (isRemoved) this@ChangesCollector::collectRemovedMember else this@ChangesCollector::collectChangedMember
            collectMember(classFqName.parent(), classFqName.shortName().asString())
            memberNames.forEach { collectMember(classFqName, it) }
        }
        else {
            collectSignature(classFqName, areSubclassesAffected = true)
        }
    }

    fun collectMemberIfValueWasChanged(scope: FqName, name: String, oldValue: Any?, newValue: Any?) {
        if (oldValue == null && newValue == null) {
            throw IllegalStateException("Old and new value are null for $scope#$name")
        }

        if (oldValue != null && newValue == null) {
            collectRemovedMember(scope, name)
        }
        else if (oldValue != newValue) {
            collectChangedMember(scope, name)
        }
    }

    private fun collectSignature(classData: ClassProtoData, areSubclassesAffected: Boolean) {
        val fqName = classData.nameResolver.getClassId(classData.proto.fqName).asSingleFqName()
        collectSignature(fqName, areSubclassesAffected)
    }

    fun collectSignature(fqName: FqName, areSubclassesAffected: Boolean) {
        val prevValue = this.areSubclassesAffected[fqName] ?: false
        this.areSubclassesAffected[fqName] = prevValue || areSubclassesAffected
    }
}