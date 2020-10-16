/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.inference.isKClassType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.load.kotlin.JvmDescriptorTypeWriter
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeMapper
import org.jetbrains.kotlin.types.TypeMappingContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContextForTypeMapping
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Type

class FirJvmTypeMapper(val session: FirSession) : TypeMappingContext<JvmDescriptorTypeWriter<Type>>, FirSessionComponent {
    override val typeContext = ConeTypeSystemCommonBackendContextForTypeMapping(session.typeContext)

    fun mapType(type: ConeKotlinType, mode: TypeMappingMode = TypeMappingMode.DEFAULT, sw: JvmDescriptorTypeWriter<Type>? = null): Type {
        return AbstractTypeMapper.mapType(this, type, mode, sw)
    }

    override fun getClassInternalName(typeConstructor: TypeConstructorMarker): String {
        require(typeConstructor is ConeClassLikeLookupTag)
        return typeConstructor.classId.asString().replace(".", "$").replace("/", ".")
    }

    override fun JvmDescriptorTypeWriter<Type>.writeGenericType(type: SimpleTypeMarker, asmType: Type, mode: TypeMappingMode) {
        //TODO Make correct implementation
        (this as JvmSignatureWriter).writeAsmType(asmType)
    }
}

val FirSession.jvmTypeMapper: FirJvmTypeMapper by FirSession.sessionComponentAccessor()

class ConeTypeSystemCommonBackendContextForTypeMapping(
    val context: ConeTypeContext
) : TypeSystemCommonBackendContext by context, TypeSystemCommonBackendContextForTypeMapping {
    private val symbolProvider = context.session.firSymbolProvider

    override fun TypeConstructorMarker.isTypeParameter(): Boolean {
        return this is ConeTypeParameterLookupTag
    }

    override fun TypeConstructorMarker.defaultType(): ConeSimpleKotlinType {
        require(this is ConeClassifierLookupTag)
        return when (this) {
            is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, isNullable = false)
            is ConeClassLikeLookupTag -> {
                val symbol = symbolProvider.getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol
                    ?: error("Class for $this not found")
                symbol.fir.defaultType()
            }
            else -> error("Unsupported type constructor: $this")
        }
    }

    override fun SimpleTypeMarker.isSuspendFunction(): Boolean {
        require(this is ConeSimpleKotlinType)
        return isSuspendFunctionType(context.session)
    }

    override fun SimpleTypeMarker.isKClass(): Boolean {
        require(this is ConeSimpleKotlinType)
        return isKClassType()
    }

    override fun KotlinTypeMarker.isRawType(): Boolean {
        return this is ConeRawType
    }

    override fun TypeConstructorMarker.typeWithArguments(arguments: List<KotlinTypeMarker>): ConeSimpleKotlinType {
        arguments.forEach {
            require(it is ConeKotlinType)
        }
        @Suppress("UNCHECKED_CAST")
        return defaultType().withArguments((arguments as List<ConeKotlinType>).toTypedArray())
    }

    override fun TypeParameterMarker.representativeUpperBound(): ConeKotlinType {
        require(this is ConeTypeParameterLookupTag)
        val bounds = this.typeParameterSymbol.fir.bounds.map { it.coneType }
        return bounds.firstOrNull {
            val classId = it.classId ?: return@firstOrNull false
            val classSymbol = symbolProvider.getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol ?: return@firstOrNull false
            val kind = classSymbol.fir.classKind
            kind != ClassKind.INTERFACE && kind != ClassKind.ANNOTATION_CLASS
        } ?: bounds.first()
    }

    override fun continuationTypeConstructor(): ConeClassLikeLookupTag {
        return symbolProvider.getClassLikeSymbolByFqName(StandardClassIds.Continuation)?.toLookupTag()
            ?: error("Continuation class not found")
    }

    override fun functionNTypeConstructor(n: Int): TypeConstructorMarker {
        return symbolProvider.getClassLikeSymbolByFqName(StandardClassIds.FunctionN(n))?.toLookupTag()
            ?: error("Function$n class not found")
    }
}
