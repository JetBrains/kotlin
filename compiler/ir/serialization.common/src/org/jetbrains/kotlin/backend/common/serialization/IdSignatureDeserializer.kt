/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignature.FileSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature as ProtoCommonIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.CompositeSignature as ProtoCompositeSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature as ProtoFileSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.LocalSignature as ProtoLocalSignature

class InvalidIdSignatureException(val erroneousSignature: IdSignature, message: String) : Exception(message)

/**
 * @see getFileSignature
 */
fun interface KlibFileSignatureProvider {

    /**
     * Used to build a signature of a private declaration.
     * Returns a signature of a file from the current module with the provided [fqName] and [filePath].
     *
     * @param fqName A fully-qualified name of the package the file belongs to.
     * @param filePath The path to the file as oin the corresponding FileEntry.
     * @return A signature of a file from the current module.
     */
    fun getFileSignature(deserializer: IdSignatureDeserializer, fqName: FqName, filePath: String): FileSignature

    object Default : KlibFileSignatureProvider {
        override fun getFileSignature(deserializer: IdSignatureDeserializer, fqName: FqName, filePath: String) =
            FileSignature(id = fqName to filePath, fqName, filePath)
    }
}

class IdSignatureDeserializer(
    private val libraryFile: IrLibraryFile,
    private val currentFileSignature: FileSignature?,
    private val fileSignatureProvider: KlibFileSignatureProvider = KlibFileSignatureProvider.Default,
) {

    private fun loadSignatureProto(index: Int): ProtoIdSignature {
        return libraryFile.signature(index)
    }

    private val signatureCache = HashMap<Int, IdSignature>()

    private var silenceErrors = false

    /**
     * Used for better error reporting.
     */
    private val signatureDeserializationStack = mutableListOf<ProtoIdSignature>()

    fun deserializeIdSignature(index: Int): IdSignature {
        return signatureCache.getOrPut(index) {
            val sigData = loadSignatureProto(index)
            signatureDeserializationStack.add(sigData)
            try {
                deserializeSignatureData(sigData)
            } finally {
                signatureDeserializationStack.removeAt(signatureDeserializationStack.lastIndex)
            }
        }
    }

    /**
     * If [silenceErrors] is `true`, calls [fallback]. Otherwise, throws an [InvalidIdSignatureException] with the information about
     * which signature we were trying to deserialize when encountered the error.
     */
    private inline fun invalidIdSignature(message: String, fallback: () -> Nothing): Nothing {
        if (silenceErrors) {
            fallback()
        }
        val erroneousSignature = try {
            IdSignatureDeserializer(libraryFile, currentFileSignature).apply {
                silenceErrors = true
            }.deserializeSignatureData(signatureDeserializationStack[0])
        } catch (e: Throwable) {
            throw IllegalStateException("This should never throw any exceptions!", e)
        }
        throw InvalidIdSignatureException(erroneousSignature, message)
    }

    internal fun invalidIdSignature(message: String): Nothing {
        require(!silenceErrors)
        invalidIdSignature(message) {
            error("unreachable")
        }
    }

    private fun deserializePublicIdSignature(proto: ProtoCommonIdSignature): IdSignature.CommonSignature {
        val pkg = libraryFile.deserializeFqName(proto.packageFqNameList)
        val cls = libraryFile.deserializeFqName(proto.declarationFqNameList)
        val memberId = if (proto.hasMemberUniqId()) proto.memberUniqId else null

        return IdSignature.CommonSignature(pkg, cls, memberId, proto.flags)
    }

    private fun deserializeAccessorIdSignature(proto: ProtoAccessorIdSignature): IdSignature.AccessorSignature {
        val propertySignature = deserializeIdSignature(proto.propertySignature)
        val hash = proto.accessorHashId
        val mask = proto.flags

        fun buildAccessorSignature(packageFqName: String, declarationFqName: String): IdSignature.AccessorSignature {
            val name = libraryFile.string(proto.name)

            val accessorSignature =
                IdSignature.CommonSignature(packageFqName, "$declarationFqName.$name", hash, mask)

            return IdSignature.AccessorSignature(propertySignature, accessorSignature)
        }

        if (propertySignature !is IdSignature.CommonSignature) {
            invalidIdSignature("For public accessor corresponding property supposed to be public as well") {
                return buildAccessorSignature(propertySignature.packageFqName().asString(), "")
            }
        }

        return buildAccessorSignature(propertySignature.packageFqName, propertySignature.declarationFqName)
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
        val localFqn = libraryFile.deserializeFqName(proto.localFqNameList)
        val localHash = if (proto.hasLocalHash()) proto.localHash else null
        val description = if (proto.hasDebugInfo()) libraryFile.debugInfo(proto.debugInfo) else null
        return IdSignature.LocalSignature(localFqn, localHash, description)
    }

    private fun deserializeFileIdSignature(proto: ProtoFileSignature): FileSignature {
        return if (proto.hasFilePath()) {
            val fqName = libraryFile.deserializeFqName(proto.fqNameList)
            fileSignatureProvider.getFileSignature(this, FqName(fqName), proto.filePath)
        } else {
            // Fallback for older behavior when we serialized file signatures as empty structures
            currentFileSignature ?: invalidIdSignature("Provide file symbol") {
                return FileSignature("<unknown>", FqName(""), "<unknown>")
            }
        }
    }

    private fun deserializeSignatureData(proto: ProtoIdSignature): IdSignature {
        return when (proto.idSigCase) {
            ProtoIdSignature.IdSigCase.PUBLIC_SIG -> deserializePublicIdSignature(proto.publicSig)
            ProtoIdSignature.IdSigCase.ACCESSOR_SIG -> deserializeAccessorIdSignature(proto.accessorSig)
            ProtoIdSignature.IdSigCase.PRIVATE_SIG -> deserializeFileLocalIdSignature(proto.privateSig)
            ProtoIdSignature.IdSigCase.SCOPED_LOCAL_SIG -> deserializeScopeLocalIdSignature(proto.scopedLocalSig)
            ProtoIdSignature.IdSigCase.COMPOSITE_SIG -> deserializeCompositeIdSignature(proto.compositeSig)
            ProtoIdSignature.IdSigCase.LOCAL_SIG -> deserializeLocalIdSignature(proto.localSig)
            ProtoIdSignature.IdSigCase.FILE_SIG -> deserializeFileIdSignature(proto.fileSig)
            else -> invalidIdSignature("Unexpected IdSignature kind: ${proto.idSigCase}") {
                return IdSignature.CommonSignature("", "<unknown>", null, 0)
            }
        }
    }

    fun signatureToIndexMapping(): Map<IdSignature, Int> {
        return signatureCache.entries.associate { it.value to it.key }
    }
}
