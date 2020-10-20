/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.ChangesCollector.Companion.getNonPrivateMemberNames
import org.jetbrains.kotlin.metadata.ProtoBuf.Visibility.PRIVATE
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME

//TODO(valtman) Should be in gradle daemon.
class AbiSnapshotDiffService() {

    companion object {
        //Store list of changed lookups
        private val diffCache: MutableMap<Pair<AbiSnapshot, AbiSnapshot>, DirtyData> = mutableMapOf()

        //TODO(valtman) move out from Kotlin daemon
        fun compareJarsInternal(
            oldSnapshot: AbiSnapshot, newSnapshot: AbiSnapshot,
            caches: IncrementalCacheCommon
        ) = diffCache.computeIfAbsent(Pair(oldSnapshot, newSnapshot)) { (snapshot, actual) -> doCompute(snapshot, actual, caches, emptyList()) }

        fun inScope(fqName: FqName, scopes: Collection<String>) = scopes.any { scope -> fqName.toString().startsWith(scope) }

        fun doCompute(snapshot: AbiSnapshot, actual: AbiSnapshot, caches: IncrementalCacheCommon, scopes: Collection<String>): DirtyData {

            val dirtyFqNames = mutableListOf<FqName>()
            val dirtyLookupSymbols = mutableListOf<LookupSymbol>()

            for ((fqName, protoData) in snapshot.protos) {
                if (!inScope(fqName, scopes)) continue
                val newProtoData = actual.protos[fqName]
                if (newProtoData == null) {
                    val (fqNames, symbols) = addProtoInfo(protoData, fqName)
                    dirtyFqNames.addAll(fqNames)
                    dirtyLookupSymbols.addAll(symbols)
                } else {
                    if (protoData is ClassProtoData && newProtoData is ClassProtoData) {
                        ProtoCompareGenerated(
                            protoData.nameResolver, newProtoData.nameResolver,
                            protoData.proto.typeTable, newProtoData.proto.typeTable
                        )
                        val diff = DifferenceCalculatorForClass(protoData, newProtoData).difference()

                        if (diff.isClassAffected) {
                            //TODO(valtman) get cache to mark dirty all subtypes if subclass affected
//                            val fqNames = if (!diff.areSubclassesAffected) listOf(fqName) else withSubtypes(fqName, caches)
                            dirtyFqNames.add(fqName)
                            assert(!fqName.isRoot) { "$fqName is root" }

                            val scope = fqName.parent().asString()
                            val name = fqName.shortName().identifier
                            dirtyLookupSymbols.add(LookupSymbol(name, scope))
                        }
                        for (member in diff.changedMembersNames) {
                            //TODO(valtman) mark dirty symbols for subclasses
                            val subtypeFqNames = withSubtypes(fqName, listOf(caches))
                            dirtyFqNames.addAll(subtypeFqNames)

                            for (subtypeFqName in subtypeFqNames) {
                                dirtyLookupSymbols.add(LookupSymbol(member, subtypeFqName.asString()))
                                dirtyLookupSymbols.add(LookupSymbol(SAM_LOOKUP_NAME.asString(), subtypeFqName.asString()))
                            }
                        }

                    } else if (protoData is PackagePartProtoData && newProtoData is PackagePartProtoData) {
                        val diff = DifferenceCalculatorForPackageFacade(protoData, newProtoData).difference()
                        for (member in diff.changedMembersNames) {
                            dirtyLookupSymbols.add(LookupSymbol(member, fqName.asString()))
                        }
                    } else {
                        //TODO(valtman) is it a valid case
                        throw IllegalStateException("package proto and class proto have the same fqName: $fqName")
                    }
                }
            }

//                fqNames.addAll(snapshot.protos.keys.removeAll(actual.protos.keys))
            DirtyData(dirtyLookupSymbols, dirtyFqNames)
            // .removeAll(actual.protos.keys)
            val oldFqNames = snapshot.protos.keys
            dirtyFqNames.addAll(actual.protos.keys.filter { !oldFqNames.contains(it) })
            return DirtyData(dirtyLookupSymbols, dirtyFqNames)

        }

        //TODO(valtman) change to return type
        private fun addProtoInfo(
            protoData: ProtoData,
            fqName: FqName,
        ) : Pair<List<FqName>, List<LookupSymbol>>{
            val fqNames = ArrayList<FqName>()
            val symbols = ArrayList<LookupSymbol>()
            when (protoData) {
                is ClassProtoData -> {
                    fqNames.add(fqName)
                    symbols.addAll(protoData.getNonPrivateMemberNames().map { LookupSymbol(it, fqName.asString()) })
                }
                is PackagePartProtoData -> {
                    symbols.addAll(
                        protoData.proto.functionOrBuilderList.filterNot { Flags.VISIBILITY.get(it.flags) == PRIVATE }
                            .map { LookupSymbol(protoData.nameResolver.getString(it.name), fqName.asString()) }.toSet()
                    )

                }
            }
            return Pair(fqNames, symbols)
        }

    }
}