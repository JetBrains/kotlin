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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
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
            // TODO need containers' type parameters too
            function.typeParameters.filterIsInstanceTo(typeParameters)
        }
    }

    private fun FirPropertyAccessor.copyToFreeAccessor(): FirPropertyAccessor {
        val accessor = this
        return buildPropertyAccessor {
            session = accessor.session
            origin = FirDeclarationOrigin.Source
            returnTypeRef = accessor.returnTypeRef.approximated(toSuper = true)
            symbol = FirPropertyAccessorSymbol()
            isGetter = accessor.isGetter
            status = accessor.status
            accessor.valueParameters.mapTo(valueParameters) {
                buildValueParameterCopy(it) {
                    returnTypeRef = it.returnTypeRef.approximated(toSuper = false)
                }
            }
            annotations += accessor.annotations
            // TODO need containers' type parameters too
            typeParameters += accessor.typeParameters
        }
    }

    private fun FirProperty.copyToFreeProperty(): FirProperty {
        val property = this
        return buildProperty {
            session = property.session
            origin = FirDeclarationOrigin.Source
            symbol = FirPropertySymbol(property.symbol.callableId)
            returnTypeRef = property.returnTypeRef.approximated(toSuper = true)
            receiverTypeRef = property.receiverTypeRef?.approximated(toSuper = false)
            name = property.name
            initializer = property.initializer
            delegate = property.delegate
            delegateFieldSymbol = property.delegateFieldSymbol?.let {
                FirDelegateFieldSymbol(it.callableId)
            }
            getter = property.getter?.copyToFreeAccessor()
            setter = property.setter?.copyToFreeAccessor()
            isVar = property.isVar
            isLocal = property.isLocal
            status = property.status
            annotations += property.annotations
            // TODO need containers' type parameters too
            typeParameters += property.typeParameters
        }.apply {
            delegateFieldSymbol?.fir = this
        }
    }

    private val localDelegatedProperties = context.localDelegatedProperties[irClass.attributeOwnerId]?.map {
        (it.owner.metadata as FirMetadataSource.Property).fir.copyToFreeProperty()
    } ?: emptyList()

    private val serializerExtension =
        FirJvmSerializerExtension(session, serializationBindings, context.state, irClass, localDelegatedProperties)

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
                    serializerExtension.serializeJvmPackage(this)
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
