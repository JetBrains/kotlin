/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.serialization

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.proto.PirAnonymousInitializerCarrier
import org.jetbrains.kotlin.backend.common.serialization.proto.PirClassCarrier
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.AnonymousInitializerCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.AnonymousInitializerCarrierImpl
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.ClassCarrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.ClassCarrierImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoIrConstructorCall

internal abstract class IrCarrierDeserializer(
    val linker: KotlinIrLinker
) {
    abstract fun deserializeOrigin(origin: Int): IrDeclarationOrigin

    abstract fun deserializeParent(parentSymbol: Long): IrDeclarationParent?

    abstract fun deserializeBlockBody(body: Int): IrBlockBody?

    abstract fun deserializeConstructorCall(call: ProtoIrConstructorCall): IrConstructorCall

    fun deserializeAnonymousInitializerCarrier(proto: PirAnonymousInitializerCarrier): AnonymousInitializerCarrier {
        return AnonymousInitializerCarrierImpl(
            proto.lastModified,
            deserializeParent(proto.parentSymbol),
            deserializeOrigin(proto.origin),
            proto.annotationList.map { deserializeConstructorCall(it) },
            deserializeBlockBody(proto.body),
        )
    }

//    fun deserializeClassCarrier(proto: PirClassCarrier): ClassCarrier {
//        return ClassCarrierImpl(
//            proto.lastModified,
//            deserializeParent(proto.parentSymbol),
//            deserializeOrigin(proto.origin),
//
//        )
//    }
}