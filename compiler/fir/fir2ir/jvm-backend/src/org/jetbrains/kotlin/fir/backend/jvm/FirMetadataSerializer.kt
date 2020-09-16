/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.MetadataSerializer
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class FirMetadataSerializer(
    private val session: FirSession,
    private val context: JvmBackendContext,
    private val irClass: IrClass,
    private val type: Type,
    private val serializationBindings: JvmSerializationBindings,
    parent: MetadataSerializer?
) : MetadataSerializer {
    private val approximator = object : AbstractTypeApproximator(session.typeContext) {}

    private fun FirTypeRef.approximated(toSuper: Boolean): FirTypeRef {
        val approximatedType = if (toSuper)
            approximator.approximateToSuperType(coneType, TypeApproximatorConfiguration.PublicDeclaration)
        else
            approximator.approximateToSubType(coneType, TypeApproximatorConfiguration.PublicDeclaration)
        return withReplacedConeType(approximatedType as? ConeKotlinType)
    }

    private fun FirFunction<*>.copyToFreeAnonymousFunction(): FirAnonymousFunction {
        val function = this
        return buildAnonymousFunction {
            session = function.session
            origin = FirDeclarationOrigin.Source
            symbol = FirAnonymousFunctionSymbol()
            returnTypeRef = function.returnTypeRef.approximated(toSuper = true)
            receiverTypeRef = function.receiverTypeRef?.approximated(toSuper = false)
            isLambda = (function as? FirAnonymousFunction)?.isLambda == true
            valueParameters.addAll(function.valueParameters.map {
                buildValueParameterCopy(it) {
                    returnTypeRef = it.returnTypeRef.approximated(toSuper = false)
                }
            })
            typeParameters.addAll(function.typeParameters.filterIsInstance<FirTypeParameter>())
        }
    }

    private val serializerExtension =
        FirJvmSerializerExtension(session, serializationBindings, context.state, irClass, context.typeMapper)

    private val serializer: FirElementSerializer? =
        when (val metadata = irClass.metadata) {
            is FirMetadataSource.Class -> FirElementSerializer.create(
                metadata.fir, serializerExtension, (parent as? FirMetadataSerializer)?.serializer
            )
            is FirMetadataSource.File -> FirElementSerializer.createTopLevel(metadata.session, serializerExtension)
            is FirMetadataSource.Function -> FirElementSerializer.createForLambda(metadata.session, serializerExtension)
            else -> null
        }

    override fun serialize(metadata: MetadataSource): Pair<MessageLite, JvmStringTable>? {
        val message = when (metadata) {
            is FirMetadataSource.Class -> serializer!!.classProto(metadata.fir).build()
            is FirMetadataSource.File ->
                serializer!!.packagePartProto(irClass.getPackageFragment()!!.fqName, metadata.fir).apply {
                    serializerExtension.serializeJvmPackage(this, type)
                }.build()
            is FirMetadataSource.Function ->
                serializer!!.functionProto(metadata.fir.copyToFreeAnonymousFunction())?.build()
            else -> null
        } ?: return null
        return message to serializer!!.stringTable as JvmStringTable
    }

    override fun bindMethodMetadata(metadata: MetadataSource.Property, signature: Method) {
        val fir = (metadata as FirMetadataSource.Property).fir
        context.state.globalSerializationBindings.put(FirJvmSerializerExtension.SYNTHETIC_METHOD_FOR_FIR_VARIABLE, fir, signature)
    }

    override fun bindMethodMetadata(metadata: MetadataSource.Function, signature: Method) {
        val fir = (metadata as FirMetadataSource.Function).fir
        serializationBindings.put(FirJvmSerializerExtension.METHOD_FOR_FIR_FUNCTION, fir, signature)
    }

    override fun bindFieldMetadata(metadata: MetadataSource.Property, signature: Pair<Type, String>) {
        val fir = (metadata as FirMetadataSource.Property).fir
        context.state.globalSerializationBindings.put(FirJvmSerializerExtension.FIELD_FOR_PROPERTY, fir, signature)
    }
}
