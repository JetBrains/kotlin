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
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.persistent.*
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.BodyCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.BodyCarrierImpl
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.DeclarationCarrier
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.IrSyntheticBodyImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader
import org.jetbrains.kotlin.library.impl.IrIntArrayMemoryReader
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import kotlin.collections.set

class CarrierDeserializer(
    val declarationDeserializer: IrDeclarationDeserializer,
    val serializedCarriers: SerializedCarriers,
) {
    private val carrierDeserializerImpl =
        IrCarrierDeserializerImpl(declarationDeserializer, ::deserializeBody, ::deserializeExpressionBody)

    private val blockBodyCache = mutableMapOf<Int, IrBody>()

    private fun deserializeBody(index: Int): IrBody = blockBodyCache.getOrPut(index) {
        when (index) {
            -1 -> IrSyntheticBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrSyntheticBodyKind.ENUM_VALUEOF)
            -2 -> IrSyntheticBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrSyntheticBodyKind.ENUM_VALUES)
            else -> (declarationDeserializer.deserializeStatementBody(index) as IrBlockBody).also {
                injectCarriers(it, index)
            }
        }

    }

    private val expressionBodyCache = mutableMapOf<Int, IrExpressionBody>()

    private fun deserializeExpressionBody(index: Int): IrExpressionBody = expressionBodyCache.getOrPut(index) {
        declarationDeserializer.deserializeExpressionBody(index).also {
            injectCarriers(it, index)
        }
    }

    private val signatureToIndex = mutableMapOf<IdSignature, Int>().also { map ->
        IrIntArrayMemoryReader(serializedCarriers.signatures).array.forEachIndexed { i, index ->
            val idSig = declarationDeserializer.symbolDeserializer.deserializeIdSignature(index)
            map[idSig] = i
        }
    }

    private val declarationReader = IrArrayMemoryReader(serializedCarriers.declarationCarriers)
    private val bodyReader = IrArrayMemoryReader(serializedCarriers.bodyCarriers)
    private val removedOnReader = IrIntArrayMemoryReader(serializedCarriers.removedOn)

    fun injectCarriers(declaration: IrDeclaration, signature: IdSignature) {
        if (declaration is PersistentIrDeclarationBase<*>) {
            when (declaration) {
                is PersistentIrAnonymousInitializer -> declaration.load(signature, carrierDeserializerImpl::deserializeAnonymousInitializerCarrier)
                is PersistentIrClass -> declaration.load(signature, carrierDeserializerImpl::deserializeClassCarrier)
                is PersistentIrConstructor -> declaration.load(signature, carrierDeserializerImpl::deserializeConstructorCarrier)
                is PersistentIrEnumEntry -> declaration.load(signature, carrierDeserializerImpl::deserializeEnumEntryCarrier)
                is PersistentIrErrorDeclaration -> declaration.load(signature, carrierDeserializerImpl::deserializeErrorDeclarationCarrier)
                is PersistentIrField -> declaration.load(signature, carrierDeserializerImpl::deserializeFieldCarrier)
                is PersistentIrFunctionCommon -> declaration.load(signature, carrierDeserializerImpl::deserializeFunctionCarrier)
                is PersistentIrLocalDelegatedProperty -> declaration.load(signature, carrierDeserializerImpl::deserializeLocalDelegatedPropertyCarrier)
                is PersistentIrPropertyCommon -> declaration.load(signature, carrierDeserializerImpl::deserializePropertyCarrier)
                is PersistentIrTypeAlias -> declaration.load(signature, carrierDeserializerImpl::deserializeTypeAliasCarrier)
                is PersistentIrTypeParameter -> declaration.load(signature, carrierDeserializerImpl::deserializeTypeParameterCarrier)
                is PersistentIrValueParameter -> declaration.load(signature, carrierDeserializerImpl::deserializeValueParameterCarrier)
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

    private inline fun <reified C : DeclarationCarrier> PersistentIrDeclarationBase<C>.load(signature: IdSignature, fn: (ByteArray) -> C) {
        val index = signatureToIndex[signature] ?: return
//            ?: error("Not found: $signature")

        val bodyCarriers = declarationReader.tableItemBytes(index)

        load(IrArrayMemoryReader(bodyCarriers).mapToArray(fn))

        removedOn = removedOnReader.array[index]
    }

    private inline fun <reified C : Carrier> PersistentIrElementBase<C>.load(carriers: Array<C>) {
        if (carriers.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            this.values = carriers as Array<Carrier>
            // pretend to be lowered "all the way"
            // TODO: make less hacky?
            this.lastModified = 1000
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