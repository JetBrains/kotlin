/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.backend.common.serialization.metadata.buildKlibPackageFragment
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.SerializableStringTable

fun serializeSingleFirFile(
    file: FirFile, session: FirSession, scopeSession: ScopeSession,
    actualizedExpectDeclarations: Set<FirDeclaration>?,
    serializerExtension: FirKLibSerializerExtension,
    languageVersionSettings: LanguageVersionSettings,
): ProtoBuf.PackageFragment {
    val approximator = TypeApproximatorForMetadataSerializer(session)
    val packageSerializer = FirElementSerializer.createTopLevel(session, scopeSession, serializerExtension, approximator,
                                                                languageVersionSettings)

    // TODO: typealiases (see klib serializer)
    // TODO: split package fragment (see klib serializer)
    // TODO: handle incremental/monolothic (see klib serializer) - maybe externally

    val packageProto = packageSerializer.packagePartProto(file.packageFqName, listOf(file), actualizedExpectDeclarations).build()

    fun List<FirDeclaration>.makeClassesProtoWithNested(): List<Pair<ProtoBuf.Class, Int>> {
        return filterIsInstance<FirClass>().filter { it.shouldBeSerialized(actualizedExpectDeclarations) }
            .sortedBy { it.classId.asFqNameString() }
            .flatMap {
                val classSerializer = FirElementSerializer.create(
                    session, scopeSession, it, serializerExtension, null,
                    approximator, languageVersionSettings
                )
                val index = classSerializer.stringTable.getFqNameIndex(it)
                listOf(classSerializer.classProto(it).build() to index) + it.declarations.makeClassesProtoWithNested()
            }
    }

    val classesProto = file.declarations.makeClassesProtoWithNested()

    val hasTopLevelDeclarations = file.declarations.any {
        it is FirMemberDeclaration && it.shouldBeSerialized(actualizedExpectDeclarations) &&
                (it is FirProperty || it is FirSimpleFunction || it is FirTypeAlias)
    }

    return buildKlibPackageFragment(
        packageProto,
        classesProto,
        file.packageFqName,
        hasTopLevelDeclarations && classesProto.isEmpty(),
        serializerExtension.stringTable as SerializableStringTable
    )
}

class FirElementAwareSerializableStringTable : FirElementAwareStringTable, SerializableStringTable() {
    override fun getLocalClassIdReplacement(firClass: FirClass): ClassId = ClassId.topLevel(StandardNames.FqNames.any.toSafe())
}
