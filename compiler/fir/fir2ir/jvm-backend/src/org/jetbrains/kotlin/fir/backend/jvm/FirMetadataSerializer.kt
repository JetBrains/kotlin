/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.backend.jvm.metadata.MetadataSerializer
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.containingClassForLocal
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameterCopy
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

fun makeFirMetadataSerializerForIrClass(
    session: FirSession,
    context: JvmBackendContext,
    irClass: IrClass,
    serializationBindings: JvmSerializationBindings,
    components: Fir2IrComponents,
    parent: MetadataSerializer?
): FirMetadataSerializer {
    val approximator = TypeApproximatorForMetadataSerializer(session)
    val localDelegatedProperties = context.localDelegatedProperties[irClass.attributeOwnerId]?.map {
        (it.owner.metadata as FirMetadataSource.Property).fir.copyToFreeProperty(approximator)
    } ?: emptyList()
    val firSerializerExtension = FirJvmSerializerExtension(
        session, serializationBindings, context.state, irClass.metadata, localDelegatedProperties,
        approximator, context.typeMapper, components
    )
    return FirMetadataSerializer(
        context.state.globalSerializationBindings,
        serializationBindings,
        firSerializerExtension,
        approximator,
        makeElementSerializer(irClass.metadata, components.session, components.scopeSession, firSerializerExtension, approximator, parent)
    )
}

@OptIn(LookupTagInternals::class)
fun makeLocalFirMetadataSerializerForMetadataSource(
    metadata: MetadataSource?,
    session: FirSession,
    scopeSession: ScopeSession,
    globalSerializationBindings: JvmSerializationBindings,
    parent: MetadataSerializer?,
    targetId: TargetId,
    configuration: CompilerConfiguration
): FirMetadataSerializer {
    val serializationBindings = JvmSerializationBindings()
    val approximator = TypeApproximatorForMetadataSerializer(session)

    val stringTable = object : JvmStringTable(null), FirElementAwareStringTable {
        override fun getLocalClassIdReplacement(firClass: FirClass): ClassId =
            ((firClass as? FirRegularClass)?.containingClassForLocal()?.toFirRegularClass(session) ?: firClass)
                .symbol.classId
    }

    val firSerializerExtension = FirJvmSerializerExtension(
        session, serializationBindings, metadata, emptyList(), approximator, scopeSession,
        globalSerializationBindings,
        configuration.getBoolean(JVMConfigurationKeys.USE_TYPE_TABLE),
        targetId.name,
        ClassBuilderMode.ABI,
        configuration.getBoolean(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS),
        session.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4 &&
                !configuration.getBoolean(JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS),
        configuration.get(CommonConfigurationKeys.METADATA_VERSION)
            ?: GenerationState.LANGUAGE_TO_METADATA_VERSION.getValue(session.languageVersionSettings.languageVersion),
        session.languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode),
        stringTable
    )
    return FirMetadataSerializer(
        globalSerializationBindings,
        serializationBindings,
        firSerializerExtension,
        approximator,
        makeElementSerializer(metadata, session, scopeSession, firSerializerExtension, approximator, parent)
    )
}

class FirMetadataSerializer(
    private val globalSerializationBindings: JvmSerializationBindings,
    private val serializationBindings: JvmSerializationBindings,
    private val serializerExtension: FirJvmSerializerExtension,
    private val approximator: AbstractTypeApproximator,
    internal val serializer: FirElementSerializer?
) : MetadataSerializer {

    override fun serialize(metadata: MetadataSource): Pair<MessageLite, JvmStringTable>? {
        val message = when (metadata) {
            is FirMetadataSource.Class -> serializer!!.classProto(metadata.fir).build()
            is FirMetadataSource.File ->
                serializer!!.packagePartProto(metadata.fir.packageFqName, metadata.fir).apply {
                    serializerExtension.serializeJvmPackage(this)
                }.build()
            is FirMetadataSource.Function -> {
                val withTypeParameters = metadata.fir.copyToFreeAnonymousFunction(approximator)
                serializationBindings.get(FirJvmSerializerExtension.METHOD_FOR_FIR_FUNCTION, metadata.fir)?.let {
                    serializationBindings.put(FirJvmSerializerExtension.METHOD_FOR_FIR_FUNCTION, withTypeParameters, it)
                }
                serializer!!.functionProto(withTypeParameters)?.build()
            }
            else -> null
        } ?: return null
        return message to serializer!!.stringTable as JvmStringTable
    }

    override fun bindPropertyMetadata(metadata: MetadataSource.Property, signature: Method, origin: IrDeclarationOrigin) {
        val fir = (metadata as FirMetadataSource.Property).fir
        val slice = when (origin) {
            JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS ->
                FirJvmSerializerExtension.SYNTHETIC_METHOD_FOR_FIR_VARIABLE
            IrDeclarationOrigin.PROPERTY_DELEGATE ->
                FirJvmSerializerExtension.DELEGATE_METHOD_FOR_FIR_VARIABLE
            else -> throw IllegalStateException("invalid origin $origin for property-related method $signature")
        }
        globalSerializationBindings.put(slice, fir, signature)
    }

    override fun bindMethodMetadata(metadata: MetadataSource.Function, signature: Method) {
        val fir = (metadata as FirMetadataSource.Function).fir
        serializationBindings.put(FirJvmSerializerExtension.METHOD_FOR_FIR_FUNCTION, fir, signature)
    }

    override fun bindFieldMetadata(metadata: MetadataSource.Property, signature: Pair<Type, String>) {
        val fir = (metadata as FirMetadataSource.Property).fir
        globalSerializationBindings.put(FirJvmSerializerExtension.FIELD_FOR_PROPERTY, fir, signature)
    }
}

internal fun makeElementSerializer(
    metadata: MetadataSource?,
    session: FirSession,
    scopeSession: ScopeSession,
    serializerExtension: FirJvmSerializerExtension,
    approximator: AbstractTypeApproximator,
    parent: MetadataSerializer?
): FirElementSerializer? =
    when (metadata) {
        is FirMetadataSource.Class -> FirElementSerializer.create(
            session, scopeSession,
            metadata.fir,
            serializerExtension,
            (parent as? FirMetadataSerializer)?.serializer,
            approximator
        )
        is FirMetadataSource.File -> FirElementSerializer.createTopLevel(
            session, scopeSession,
            serializerExtension,
            approximator
        )
        is FirMetadataSource.Function -> FirElementSerializer.createForLambda(
            session, scopeSession,
            serializerExtension,
            approximator
        )
        else -> null
    }

internal class TypeApproximatorForMetadataSerializer(session: FirSession) :
    AbstractTypeApproximator(session.typeContext, session.languageVersionSettings) {

    override fun createErrorType(debugName: String): SimpleTypeMarker {
        return ConeErrorType(ConeIntermediateDiagnostic(debugName))
    }
}

private fun FirFunction.copyToFreeAnonymousFunction(approximator: AbstractTypeApproximator): FirAnonymousFunction {
    val function = this
    return buildAnonymousFunction {
        val typeParameterSet = function.typeParameters.filterIsInstanceTo(mutableSetOf<FirTypeParameter>())
        moduleData = function.moduleData
        origin = FirDeclarationOrigin.Source
        symbol = FirAnonymousFunctionSymbol()
        returnTypeRef = function.returnTypeRef.approximated(approximator, typeParameterSet, toSuper = true)
        receiverTypeRef = function.receiverTypeRef?.approximated(approximator, typeParameterSet, toSuper = false)
        isLambda = (function as? FirAnonymousFunction)?.isLambda == true
        hasExplicitParameterList = (function as? FirAnonymousFunction)?.hasExplicitParameterList == true
        valueParameters.addAll(function.valueParameters.map {
            buildValueParameterCopy(it) {
                returnTypeRef = it.returnTypeRef.approximated(approximator, typeParameterSet, toSuper = false)
            }
        })
        typeParameters += typeParameterSet
    }
}

private fun FirPropertyAccessor.copyToFreeAccessor(approximator: AbstractTypeApproximator): FirPropertyAccessor {
    val accessor = this
    return buildPropertyAccessor {
        val typeParameterSet = accessor.typeParameters.toMutableSet()
        moduleData = accessor.moduleData
        origin = FirDeclarationOrigin.Source
        returnTypeRef = accessor.returnTypeRef.approximated(approximator, typeParameterSet, toSuper = true)
        symbol = FirPropertyAccessorSymbol()
        isGetter = accessor.isGetter
        status = accessor.status
        accessor.valueParameters.mapTo(valueParameters) {
            buildValueParameterCopy(it) {
                returnTypeRef = it.returnTypeRef.approximated(approximator, typeParameterSet, toSuper = false)
            }
        }
        annotations += accessor.annotations
        typeParameters += typeParameterSet
    }
}

internal fun FirProperty.copyToFreeProperty(approximator: AbstractTypeApproximator): FirProperty {
    val property = this
    return buildProperty {
        val typeParameterSet = property.typeParameters.toMutableSet()
        moduleData = property.moduleData
        origin = FirDeclarationOrigin.Source
        symbol = FirPropertySymbol(property.symbol.callableId)
        returnTypeRef = property.returnTypeRef.approximated(approximator, typeParameterSet, toSuper = true)
        receiverTypeRef = property.receiverTypeRef?.approximated(approximator, typeParameterSet, toSuper = false)
        name = property.name
        initializer = property.initializer
        delegate = property.delegate
        delegateFieldSymbol = property.delegateFieldSymbol?.let {
            FirDelegateFieldSymbol(it.callableId)
        }
        getter = property.getter?.copyToFreeAccessor(approximator)
        setter = property.setter?.copyToFreeAccessor(approximator)
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

internal fun FirTypeRef.approximated(
    approximator: AbstractTypeApproximator,
    typeParameterSet: MutableCollection<FirTypeParameter>,
    toSuper: Boolean
): FirTypeRef {
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
