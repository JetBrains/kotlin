/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature as ProtoCommonIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.CompositeSignature as ProtoCompositeSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature as ProtoFileSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.LocalSignature as ProtoLocalSignature
import org.jetbrains.kotlin.ir.util.IdSignature
import java.util.ArrayList

class IdSignatureSerializer(
    private val serializeString: (String) -> Int,
    private val serializeDebugInfo: (String) -> Int,
    private val protoIdSignatureMap: MutableMap<IdSignature, Int>,
    private val protoIdSignatureArray: ArrayList<ProtoIdSignature>
) {

    private fun serializeFqName(fqName: String): List<Int> = fqName.split(".").map { serializeString(it) }
    private fun serializePublicSignature(signature: IdSignature.CommonSignature): ProtoCommonIdSignature {
        val proto = ProtoCommonIdSignature.newBuilder()
        proto.addAllPackageFqName(serializeFqName(signature.packageFqName))
        proto.addAllDeclarationFqName(serializeFqName(signature.declarationFqName))

        signature.id?.let { proto.memberUniqId = it }
        if (signature.mask != 0L) {
            proto.flags = signature.mask
        }

        signature.description?.let { proto.debugInfo = serializeDebugInfo(it) }

        return proto.build()
    }

    private fun serializeAccessorSignature(signature: IdSignature.AccessorSignature): ProtoAccessorIdSignature {
        val proto = ProtoAccessorIdSignature.newBuilder()

        proto.propertySignature = protoIdSignature(signature.propertySignature)
        with(signature.accessorSignature) {
            proto.name = serializeString(shortName)
            proto.accessorHashId = id ?: error("Expected hash Id")
            if (mask != 0L) {
                proto.flags = mask
            }
            description?.let { proto.debugInfo = serializeDebugInfo(it) }
        }

        return proto.build()
    }

    private fun serializePrivateSignature(signature: IdSignature.FileLocalSignature): ProtoFileLocalIdSignature {
        val proto = ProtoFileLocalIdSignature.newBuilder()

        proto.container = protoIdSignature(signature.container)
        proto.localId = signature.id
        signature.description?.let { proto.debugInfo = serializeDebugInfo(it) }

        return proto.build()
    }

    private fun serializeScopeLocalSignature(signature: IdSignature.ScopeLocalDeclaration): Int = signature.id

    private fun serializeCompositeSignature(signature: IdSignature.CompositeSignature): ProtoCompositeSignature {
        val proto = ProtoCompositeSignature.newBuilder()

        proto.containerSig = protoIdSignature(signature.container)
        proto.innerSig = protoIdSignature(signature.inner)

        return proto.build()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun serializeFileSignature(signature: IdSignature.FileSignature): ProtoFileSignature = ProtoFileSignature.getDefaultInstance()

    private fun serializeLocalSignature(signature: IdSignature.LocalSignature): ProtoLocalSignature {
        val proto = ProtoLocalSignature.newBuilder()

        proto.addAllLocalFqName(serializeFqName(signature.localFqn))
        signature.hashSig?.let { proto.localHash = it }
        signature.description?.let { proto.debugInfo = serializeDebugInfo(it) }

        return proto.build()
    }

    private fun serializeIdSignature(idSignature: IdSignature): ProtoIdSignature {
        val proto = ProtoIdSignature.newBuilder()
        when (idSignature) {
            is IdSignature.CommonSignature -> proto.publicSig = serializePublicSignature(idSignature)
            is IdSignature.AccessorSignature -> proto.accessorSig = serializeAccessorSignature(idSignature)
            is IdSignature.FileLocalSignature -> proto.privateSig = serializePrivateSignature(idSignature)
            is IdSignature.ScopeLocalDeclaration -> proto.scopedLocalSig = serializeScopeLocalSignature(idSignature)
            is IdSignature.CompositeSignature -> proto.compositeSig = serializeCompositeSignature(idSignature)
            is IdSignature.LocalSignature -> proto.localSig = serializeLocalSignature(idSignature)
            is IdSignature.FileSignature -> proto.fileSig = serializeFileSignature(idSignature)
            is IdSignature.SpecialFakeOverrideSignature -> {}
            is IdSignature.LoweredDeclarationSignature -> error("LoweredDeclarationSignature is not expected here")
        }
        return proto.build()
    }

    fun protoIdSignature(idSig: IdSignature): Int {
        return protoIdSignatureMap.getOrPut(idSig) {
            protoIdSignatureArray.add(serializeIdSignature(idSig))
            protoIdSignatureArray.size - 1
        }
    }
}
