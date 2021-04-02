/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.common.serialization.proto.PirBodyCarrier
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrBodyBase
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrElementBase
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.*
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.AnonymousInitializerCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.ClassCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.ConstructorCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.EnumEntryCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.ErrorDeclarationCarrier
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.impl.IrMemoryArrayWriter
import org.jetbrains.kotlin.library.impl.IrMemoryIntArrayWriter

class SerializedCarriers(
    val signatures: ByteArray,
    val declarationCarriers: ByteArray,
    val bodyCarriers: ByteArray,
    val removedOn: ByteArray,
)

// Declarations are references by signatures, bodies are referenced by index.
fun IrFileSerializer.serializeCarriers(declarations: Iterable<IrDeclaration>, bodies: List<IrBody>, signaturer: (IrDeclaration) -> IdSignature): SerializedCarriers {
    val serializer = CarrierSerializer(this, bodies, signaturer)

    declarations.forEach { serializer.serializeDeclarationCarrier(it) }
    bodies.forEach { serializer.serializeBodyCarriers(it) }

    return serializer.build()
}

private class CarrierSerializer(val fileSerializer: IrFileSerializer, bodies: List<IrBody>, val signaturer: (IrDeclaration) -> IdSignature) {

    private val bodyToIndex = mutableMapOf<IrBody, Int>().also { map ->
        bodies.forEachIndexed { index, irBody ->
            map[irBody] = index
        }
    }

    private val serializerImpl = IrCarrierSerializerImpl(fileSerializer) {
        if (it is IrSyntheticBody) {
            when (it.kind) {
                IrSyntheticBodyKind.ENUM_VALUEOF -> -1
                IrSyntheticBodyKind.ENUM_VALUES -> -2
            }
        } else bodyToIndex[it]
            ?: error("")
    }

    val signatures = mutableListOf<Int>()
    val declarationCarriers = mutableListOf<ByteArray>()
    val bodyCarriers = mutableListOf<ByteArray>()
    val removedOn = mutableListOf<Int>()

    fun build(): SerializedCarriers {
        return SerializedCarriers(
            IrMemoryIntArrayWriter(signatures).writeIntoMemory(),
            IrMemoryArrayWriter(declarationCarriers).writeIntoMemory(),
            IrMemoryArrayWriter(bodyCarriers).writeIntoMemory(),
            IrMemoryIntArrayWriter(removedOn).writeIntoMemory(),
        )
    }

    fun serializeDeclarationCarrier(declaration: IrDeclaration) {
        if (declaration is PersistentIrDeclarationBase<*>) {
            // TODO proper signature calculations?
            val signature = signaturer(declaration)
            signatures += fileSerializer.protoIdSignature(signature)
            declarationCarriers += serializeCarriers(declaration)
            removedOn += declaration.removedOn
        } // else -> TODO?
    }

    fun serializeBodyCarriers(body: IrBody) {
        if (body is PersistentIrBodyBase<*>) {
            bodyCarriers += serializeCarriers(body)
        }
    }

    private fun serializeCarriers(element: PersistentIrElementBase<*>): ByteArray {
        val carriers = with(serializerImpl) {
            // Serialize all state
            // TODO maybe skip state in the initial declarations?
            val values = ((element.values ?: arrayOf()) + element)
            values.map {
                when (it) {
                    is AnonymousInitializerCarrier -> serializeAnonymousInitializerCarrier(it)
                    is ClassCarrier -> serializeClassCarrier(it)
                    is ConstructorCarrier -> serializeConstructorCarrier(it)
                    is EnumEntryCarrier -> serializeEnumEntryCarrier(it)
                    is ErrorDeclarationCarrier -> serializeErrorDeclarationCarrier(it)
                    is FieldCarrier -> serializeFieldCarrier(it)
                    is FunctionCarrier -> serializeFunctionCarrier(it)
                    is LocalDelegatedPropertyCarrier -> serializeLocalDelegatedPropertyCarrier(it)
                    is PropertyCarrier -> serializePropertyCarrier(it)
                    is TypeAliasCarrier -> serializeTypeAliasCarrier(it)
                    is TypeParameterCarrier -> serializeTypeParameterCarrier(it)
                    is ValueParameterCarrier -> serializeValueParameterCarrier(it)
                    is BodyCarrier -> serializeBodyCarrier(it)
                    else -> error("unknown Carrier")
                }
            }
        }

        return IrMemoryArrayWriter(carriers).writeIntoMemory()
    }

    private fun serializeBodyCarrier(bodyCarrier: BodyCarrier): ByteArray {
        val proto = PirBodyCarrier.newBuilder()
        proto.lastModified = bodyCarrier.lastModified
        bodyCarrier.containerFieldSymbol?.let { proto.containerFieldSymbol = serializerImpl.fileSerializer.serializeIrSymbol(it) }
        return proto.build().toByteArray()
    }
}