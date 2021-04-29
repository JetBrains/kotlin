/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.MetadataSerializer
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameterCopy
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class FirMetadataSerializer(
    private val session: FirSession,
    private val context: JvmBackendContext,
    private val irClass: IrClass,
    private val serializationBindings: JvmSerializationBindings,
    components: Fir2IrComponents,
    parent: MetadataSerializer?
) : MetadataSerializer {
    private val approximator = object : AbstractTypeApproximator(session.typeContext, session.languageVersionSettings) {
        override fun createErrorType(debugName: String): SimpleTypeMarker {
            return ConeKotlinErrorType(ConeIntermediateDiagnostic(debugName))
        }
    }

    private fun FirTypeRef.approximated(toSuper: Boolean, typeParameterSet: MutableCollection<FirTypeParameter>): FirTypeRef {
        val approximatedType = if (toSuper)
            approximator.approximateToSuperType(coneType, TypeApproximatorConfiguration.PublicDeclaration)
        else
            approximator.approximateToSubType(coneType, TypeApproximatorConfiguration.PublicDeclaration)
        return withReplacedConeType(approximatedType as? ConeKotlinType).apply { coneType.collectTypeParameters(typeParameterSet) }
    }

    private fun ConeKotlinType.collectTypeParameters(c: MutableCollection<FirTypeParameter>) {
        when (this) {
            is ConeFlexibleType -> {
                lowerBound.collectTypeParameters(c)
                upperBound.collectTypeParameters(c)
            }
            is ConeClassLikeType ->
                for (projection in type.typeArguments) {
                    if (projection is ConeKotlinTypeProjection) {
                        projection.type.collectTypeParameters(c)
                    }
                }
            is ConeTypeParameterType -> c.add(lookupTag.typeParameterSymbol.fir)
            else -> Unit
        }
    }

    private fun FirFunction<*>.copyToFreeAnonymousFunction(): FirAnonymousFunction {
        val function = this
        return buildAnonymousFunction {
            val typeParameterSet = function.typeParameters.filterIsInstanceTo(mutableSetOf<FirTypeParameter>())
            moduleData = function.moduleData
            origin = FirDeclarationOrigin.Source
            symbol = FirAnonymousFunctionSymbol()
            returnTypeRef = function.returnTypeRef.approximated(toSuper = true, typeParameterSet)
            receiverTypeRef = function.receiverTypeRef?.approximated(toSuper = false, typeParameterSet)
            isLambda = (function as? FirAnonymousFunction)?.isLambda == true
            valueParameters.addAll(function.valueParameters.map {
                buildValueParameterCopy(it) {
                    returnTypeRef = it.returnTypeRef.approximated(toSuper = false, typeParameterSet)
                }
            })
            typeParameters += typeParameterSet
        }
    }

    private fun FirPropertyAccessor.copyToFreeAccessor(): FirPropertyAccessor {
        val accessor = this
        return buildPropertyAccessor {
            val typeParameterSet = accessor.typeParameters.toMutableSet()
            moduleData = accessor.moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = accessor.returnTypeRef.approximated(toSuper = true, typeParameterSet)
            symbol = FirPropertyAccessorSymbol()
            isGetter = accessor.isGetter
            status = accessor.status
            accessor.valueParameters.mapTo(valueParameters) {
                buildValueParameterCopy(it) {
                    returnTypeRef = it.returnTypeRef.approximated(toSuper = false, typeParameterSet)
                }
            }
            annotations += accessor.annotations
            typeParameters += typeParameterSet
        }
    }

    private fun FirProperty.copyToFreeProperty(): FirProperty {
        val property = this
        return buildProperty {
            val typeParameterSet = property.typeParameters.toMutableSet()
            moduleData = property.moduleData
            origin = FirDeclarationOrigin.Source
            symbol = FirPropertySymbol(property.symbol.callableId)
            returnTypeRef = property.returnTypeRef.approximated(toSuper = true, typeParameterSet)
            receiverTypeRef = property.receiverTypeRef?.approximated(toSuper = false, typeParameterSet)
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
            dispatchReceiverType = property.dispatchReceiverType
            attributes = property.attributes.copy()
            annotations += property.annotations
            typeParameters += typeParameterSet
        }.apply {
            delegateFieldSymbol?.bind(this)
        }
    }

    private val localDelegatedProperties = context.localDelegatedProperties[irClass.attributeOwnerId]?.map {
        (it.owner.metadata as FirMetadataSource.Property).fir.copyToFreeProperty()
    } ?: emptyList()

    private val serializerExtension = FirJvmSerializerExtension(
        session, serializationBindings, context.state, irClass, localDelegatedProperties, approximator,
        context.typeMapper, components
    )

    private val serializer: FirElementSerializer? =
        when (val metadata = irClass.metadata) {
            is FirMetadataSource.Class -> FirElementSerializer.create(
                components.session,
                components.scopeSession,
                metadata.fir, serializerExtension, (parent as? FirMetadataSerializer)?.serializer, approximator
            )
            is FirMetadataSource.File -> FirElementSerializer.createTopLevel(
                components.session,
                components.scopeSession,
                serializerExtension,
                approximator
            )
            is FirMetadataSource.Function -> FirElementSerializer.createForLambda(
                components.session,
                components.scopeSession,
                serializerExtension,
                approximator
            )
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
