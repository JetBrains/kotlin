/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.ClassMetadataSerializer
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class FirJvmMetadataSerializer(
    session: FirSession,
    private val irClass: IrClass,
    private val context: JvmBackendContext,
    private val localSerializationBindings: JvmSerializationBindings,
    parent: ClassMetadataSerializer?
) : ClassMetadataSerializer {
    private val serializerExtension =
        FirJvmSerializerExtension(session, localSerializationBindings, context.state, irClass, context.typeMapper)

    private val serializer: FirElementSerializer? =
        when (val metadata = irClass.metadata) {
            is FirMetadataSource.Class -> FirElementSerializer.create(
                metadata.klass, serializerExtension, (parent as? FirJvmMetadataSerializer)?.serializer
            )
            is FirMetadataSource.File -> FirElementSerializer.createTopLevel(session, serializerExtension)
            is FirMetadataSource.Function -> FirElementSerializer.createForLambda(session, serializerExtension)
            else -> null
        }

    private val approximator: AbstractTypeApproximator = (parent as? FirJvmMetadataSerializer)?.approximator
        ?: object : AbstractTypeApproximator(session.typeContext) {
            override fun createErrorType(message: String): SimpleTypeMarker = ConeKotlinErrorType(message)
        }

    private fun FirTypeRef.approximated(
        toSuper: Boolean,
        conf: TypeApproximatorConfiguration = TypeApproximatorConfiguration.PublicDeclaration
    ): FirTypeRef {
        val approximatedType = if (toSuper)
            approximator.approximateToSuperType(this.coneTypeUnsafe(), conf)
        else
            approximator.approximateToSubType(this.coneTypeUnsafe(), conf)
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

    // TODO fix `MetadataSource` class hierarchy (FIR class is not a class?) & remove this
    override val isSynthetic: Boolean
        get() = irClass.metadata !is FirMetadataSource.Class

    override val stringTable: JvmStringTable
        get() = serializerExtension.stringTable

    override fun generateMetadataProto(type: Type): MessageLite? =
        when (val metadata = irClass.metadata) {
            is FirMetadataSource.Function -> serializer!!.functionProto(metadata.function.copyToFreeAnonymousFunction())?.build()
            is FirMetadataSource.Class -> serializer!!.classProto(metadata.klass).build()
            is FirMetadataSource.File ->
                serializer!!.packagePartProto(irClass.getPackageFragment()!!.fqName, metadata.file).also {
                    serializerExtension.serializeJvmPackage(it, type)
                }.build()
            else -> null
        }

    override fun bindMethodMetadata(method: IrFunction, signature: Method) {
        when (val metadata = method.metadata) {
            is FirMetadataSource.Variable -> {
                // We can't check for JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS because for interface methods
                // moved to DefaultImpls, origin is changed to DEFAULT_IMPLS
                // TODO: fix origin somehow, because otherwise $annotations methods in interfaces also don't have ACC_SYNTHETIC
                assert(method.name.asString().endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)) { method.dump() }

                context.state.globalSerializationBindings.put(
                    FirJvmSerializerExtension.SYNTHETIC_METHOD_FOR_FIR_VARIABLE, metadata.variable, signature
                )
            }
            is FirMetadataSource.Function -> {
                localSerializationBindings.put(FirJvmSerializerExtension.METHOD_FOR_FIR_FUNCTION, metadata.function, signature)
            }
            null -> {
            }
            else -> {
                error("Incorrect metadata source $metadata for:\n${method.dump()}")
            }
        }
    }

    override fun bindFieldMetadata(field: IrField, fieldType: Type, fieldName: String) {
        val metadata = field.metadata
        if (metadata is FirMetadataSource.Property) {
            context.state.globalSerializationBindings.put(FirJvmSerializerExtension.FIELD_FOR_PROPERTY, metadata.property, fieldType to fieldName)
        }
    }
}