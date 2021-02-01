/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrDeclarationDeserializer
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.PirBodyCarrier
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.persistent.*
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrClass
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.BodyCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.BodyCarrierImpl
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.DeclarationCarrier
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature

class CarrierDeserializer(
    val declarationDeserializer: IrDeclarationDeserializer,
    val serializedCarriers: SerializedCarriers,
) {
    private val carrierDeserializerImpl = IrCarrierDeserializerImpl(declarationDeserializer)

    private val signatureToIndex = mutableMapOf<IdSignature, Int>().also { map ->
        IrArrayMemoryReader(serializedCarriers.signatures).forEachIndexed { i, bytes ->
            val signatureProto = ProtoIdSignature.parseFrom(bytes, ExtensionRegistryLite.newInstance())
            val idSig = declarationDeserializer.symbolDeserializer.deserializeSignatureData(signatureProto)
            map[idSig] = i
        }
    }

    private val declarationReader = IrArrayMemoryReader(serializedCarriers.declarationCarriers)
    private val bodyReader = IrArrayMemoryReader(serializedCarriers.bodyCarriers)

    fun injectCarriers(declaration: IrDeclaration) {
        if (declaration is PersistentIrDeclarationBase<*>) {
            when (declaration) {
                is PersistentIrAnonymousInitializer -> declaration.load(carrierDeserializerImpl::deserializeAnonymousInitializerCarrier)
                is PersistentIrClass -> declaration.load(carrierDeserializerImpl::deserializeClassCarrier)
                is PersistentIrConstructor -> declaration.load(carrierDeserializerImpl::deserializeConstructorCarrier)
                is PersistentIrEnumEntry -> declaration.load(carrierDeserializerImpl::deserializeEnumEntryCarrier)
                is PersistentIrErrorDeclaration -> declaration.load(carrierDeserializerImpl::deserializeErrorDeclarationCarrier)
                is PersistentIrField -> declaration.load(carrierDeserializerImpl::deserializeFieldCarrier)
                is PersistentIrFunctionCommon -> declaration.load(carrierDeserializerImpl::deserializeFunctionCarrier)
                is PersistentIrLocalDelegatedProperty -> declaration.load(carrierDeserializerImpl::deserializeLocalDelegatedPropertyCarrier)
                is PersistentIrPropertyCommon -> declaration.load(carrierDeserializerImpl::deserializePropertyCarrier)
                is PersistentIrTypeAlias -> declaration.load(carrierDeserializerImpl::deserializeTypeAliasCarrier)
                is PersistentIrTypeParameter -> declaration.load(carrierDeserializerImpl::deserializeTypeParameterCarrier)
                is PersistentIrValueParameter -> declaration.load(carrierDeserializerImpl::deserializeValueParameterCarrier)
                else -> error("unknown declaration type: ${declaration::class.qualifiedName}")
            }
        }
    }

    fun injectCarriers(body: IrBody, index: Int) {
        if (body is PersistentIrBodyBase<*>) {
            val bodyCarriers = bodyReader.tableItemBytes(index)

            val carriers = IrArrayMemoryReader(bodyCarriers).mapToArray { bodyBytes ->
                val bodyProto = PirBodyCarrier.parseFrom(bodyBytes.codedInputStream, ExtensionRegistryLite.newInstance())
                deserializeBodyCarrier(bodyProto)
            }

            body.load(carriers)
        }
    }

    private inline fun <reified C : DeclarationCarrier> PersistentIrDeclarationBase<C>.load(fn: (ByteArray) -> C) {
        val index = signatureToIndex[(this as IrSymbolOwner).symbol.signature]!!

        val bodyCarriers = declarationReader.tableItemBytes(index)

        load(IrArrayMemoryReader(bodyCarriers).mapToArray(fn))
    }

    private inline fun <reified C : Carrier> PersistentIrElementBase<C>.load(carriers: Array<C>) {
        if (carriers.isNotEmpty()) {
            val lastState = carriers.last()
            this.values = carriers.dropLast(1).toTypedArray()
            this.setState(lastState)
        }
    }

    private fun deserializeBodyCarrier(proto: PirBodyCarrier): BodyCarrier {
        return BodyCarrierImpl(
            proto.lastModified,
            if (proto.hasContainerFieldSymbol()) declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto.containerFieldSymbol) else null,
        )
    }
}

private inline fun <reified T> IrArrayMemoryReader.mapToArray(fn: (ByteArray) -> T): Array<T> {
    return Array<T>(entryCount()) { i ->
        fn(tableItemBytes(i))
    }
}

private fun IrArrayMemoryReader.forEachIndexed(fn: (Int, ByteArray) -> Unit) {
    val size = entryCount()
    for (i in 0 until size) {
        fn(i, tableItemBytes(i))
    }
}