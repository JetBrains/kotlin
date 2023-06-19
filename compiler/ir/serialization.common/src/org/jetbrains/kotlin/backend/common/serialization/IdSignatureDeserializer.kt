/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignature.FileSignature
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize
import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature as ProtoCommonIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.CompositeSignature as ProtoCompositeSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature as ProtoFileSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.LocalSignature as ProtoLocalSignature

class IdSignatureDeserializer(
    private val libraryFile: IrLibraryFile,
    private val fileSignature: FileSignature?,
    private val internationService: IrInterningService
) {

    private fun loadSignatureProto(index: Int): ProtoIdSignature {
        return libraryFile.signature(index)
    }

    private val signatureCache = HashMap<Int, IdSignature>()

    fun deserializeIdSignature(index: Int): IdSignature {
        return signatureCache.getOrPut(index) {
            val sigData = loadSignatureProto(index)
            deserializeSignatureData(sigData)
        }
    }

    private fun deserializePublicIdSignature(proto: ProtoCommonIdSignature): IdSignature.CommonSignature {
        val pkg = internationService.string(libraryFile.deserializeFqName(proto.packageFqNameList))
        val cls = internationService.string(libraryFile.deserializeFqName(proto.declarationFqNameList))
        val memberId = if (proto.hasMemberUniqId()) proto.memberUniqId else null
        val description = if (proto.hasDebugInfo()) libraryFile.debugInfo(proto.debugInfo)?.let(internationService::string) else null

        return IdSignature.CommonSignature(
            packageFqName = pkg,
            declarationFqName = cls,
            id = memberId,
            mask = proto.flags,
            description = description,
        )
    }

    private fun deserializeAccessorIdSignature(proto: ProtoAccessorIdSignature): IdSignature.AccessorSignature {
        val propertySignature = deserializeIdSignature(proto.propertySignature)
        require(propertySignature is IdSignature.CommonSignature) { "For public accessor corresponding property supposed to be public as well" }
        val name = libraryFile.string(proto.name)
        val hash = proto.accessorHashId
        val mask = proto.flags
        val description = if (proto.hasDebugInfo()) libraryFile.debugInfo(proto.debugInfo)?.let(internationService::string) else null

        val declarationFqName = internationService.string("${propertySignature.declarationFqName}.$name")
        val accessorSignature =
            IdSignature.CommonSignature(
                packageFqName = propertySignature.packageFqName,
                declarationFqName = declarationFqName,
                id = hash,
                mask = mask,
                description = description,
            )

        return IdSignature.AccessorSignature(propertySignature, accessorSignature)
    }

    private fun deserializeFileLocalIdSignature(proto: ProtoFileLocalIdSignature): IdSignature {
        return IdSignature.FileLocalSignature(deserializeIdSignature(proto.container), proto.localId)
    }

    private fun deserializeScopeLocalIdSignature(proto: Int): IdSignature {
        return IdSignature.ScopeLocalDeclaration(proto)
    }

    private fun deserializeCompositeIdSignature(proto: ProtoCompositeSignature): IdSignature.CompositeSignature {
        val containerSig = deserializeIdSignature(proto.containerSig)
        val innerSig = deserializeIdSignature(proto.innerSig)
        return IdSignature.CompositeSignature(containerSig, innerSig)
    }

    private fun deserializeLocalIdSignature(proto: ProtoLocalSignature): IdSignature.LocalSignature {
        val localFqn = internationService.string(libraryFile.deserializeFqName(proto.localFqNameList))
        val localHash = if (proto.hasLocalHash()) proto.localHash else null
        val description = if (proto.hasDebugInfo()) libraryFile.debugInfo(proto.debugInfo) else null
        return IdSignature.LocalSignature(localFqn, localHash, description)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun deserializeFileIdSignature(proto: ProtoFileSignature): FileSignature = fileSignature ?: error("Provide file symbol")

    private fun deserializeSignatureData(proto: ProtoIdSignature): IdSignature {
        return when (proto.idSigCase) {
            ProtoIdSignature.IdSigCase.PUBLIC_SIG -> deserializePublicIdSignature(proto.publicSig)
            ProtoIdSignature.IdSigCase.ACCESSOR_SIG -> deserializeAccessorIdSignature(proto.accessorSig)
            ProtoIdSignature.IdSigCase.PRIVATE_SIG -> deserializeFileLocalIdSignature(proto.privateSig)
            ProtoIdSignature.IdSigCase.SCOPED_LOCAL_SIG -> deserializeScopeLocalIdSignature(proto.scopedLocalSig)
            ProtoIdSignature.IdSigCase.COMPOSITE_SIG -> deserializeCompositeIdSignature(proto.compositeSig)
            ProtoIdSignature.IdSigCase.LOCAL_SIG -> deserializeLocalIdSignature(proto.localSig)
            ProtoIdSignature.IdSigCase.FILE_SIG -> deserializeFileIdSignature(proto.fileSig)
            else -> error("Unexpected IdSignature kind: ${proto.idSigCase}")
        }
    }

    fun signatureToIndexMapping(): Map<IdSignature, Int> {
        if (signatureCache.isEmpty()) return emptyMap()
        return signatureCache.entries.associateTo(newHashMapWithExpectedSize(signatureCache.size)) { it.value to it.key }
    }
}