/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.serialization.metadata.buildKlibPackageFragment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.serialization.FirSerializerExtension
import org.jetbrains.kotlin.fir.serialization.TypeApproximatorForMetadataSerializer
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.serialization.SerializableStringTable

fun serializeSingleFirFile(file: FirFile, session: FirSession, scopeSession: ScopeSession, configuration: CompilerConfiguration): ProtoBuf.PackageFragment {
    val serializerExtension = FirKLibSerializerExtension(
        session,
        configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!,
        FirElementAwareSerializableStringTable()
    )
    val approximator = TypeApproximatorForMetadataSerializer(session)
    val packageSerializer = FirElementSerializer.createTopLevel(session, scopeSession, serializerExtension, approximator)

    // TODO: typealiases (see klib serializer)
    // TODO: split package fragment (see klib serializer)
    // TODO: handle incremental/monolothic (see klib serializer) - maybe externally

    val packageProto = packageSerializer.packagePartProto(file.packageFqName, file).build()

    // TODO: filter out expects
    val classifiers = file.declarations.filterIsInstance<FirClass>().sortedBy { it.classId.asFqNameString() }

    val classesProto = classifiers.map {
        val classSerializer = FirElementSerializer.create(session, scopeSession, it, serializerExtension, null, approximator)
        val index = classSerializer.stringTable.getFqNameIndex(it)
        classSerializer.classProto(it).build() to index
    }

    val hasTopLevelDeclarations = file.declarations.any {
        it is FirProperty || it is FirSimpleFunction || it is FirTypeAlias
    }

    return buildKlibPackageFragment(
        packageProto,
        classesProto,
        file.packageFqName,
        hasTopLevelDeclarations && classesProto.isEmpty(),
        serializerExtension.stringTable as SerializableStringTable
    )
}

class FirKLibSerializerExtension(
    override val session: FirSession,
    override val metadataVersion: BinaryVersion,
    override val stringTable: FirElementAwareSerializableStringTable
) : FirSerializerExtension()

class FirElementAwareSerializableStringTable() : FirElementAwareStringTable, SerializableStringTable()
