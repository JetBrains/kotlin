/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.AbstractTypeMapper
import org.jetbrains.kotlin.types.TypeMappingContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContextForTypeMapping
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Type

class FirJvmTypeMapper(val session: FirSession) : TypeMappingContext<JvmSignatureWriter>, FirSessionComponent {
    override val typeContext = ConeTypeSystemCommonBackendContextForTypeMapping(session.typeContext)

    fun mapType(type: ConeKotlinType, mode: TypeMappingMode = TypeMappingMode.DEFAULT, sw: JvmSignatureWriter? = null): Type {
        return AbstractTypeMapper.mapType(this, type, mode, sw)
    }

    override fun getClassInternalName(typeConstructor: TypeConstructorMarker): String {
        require(typeConstructor is ConeClassLikeLookupTag)
        return typeConstructor.classId.asString().replace(".", "$").replace("/", ".")
    }

    override fun JvmSignatureWriter.writeGenericType(type: SimpleTypeMarker, asmType: Type, mode: TypeMappingMode) {
        if (type !is ConeClassLikeType) return
        if (skipGenericSignature() || hasNothingInNonContravariantPosition(type) || type.typeArguments.isEmpty()) {
            writeAsmType(asmType)
            return
        }

        val possiblyInnerType = type.buildPossiblyInnerType() ?: error("possiblyInnerType with arguments should not be null")

        val innerTypesAsList = possiblyInnerType.segments()

        val indexOfParameterizedType = innerTypesAsList.indexOfFirst { innerPart -> innerPart.arguments.isNotEmpty() }
        if (indexOfParameterizedType < 0 || innerTypesAsList.size == 1) {
            writeClassBegin(asmType)
            writeGenericArguments(this, possiblyInnerType, mode)
        } else {
            val outerType = innerTypesAsList[indexOfParameterizedType]

            writeOuterClassBegin(asmType, mapType(outerType.classifier.fir.defaultType()).internalName)
            writeGenericArguments(this, outerType, mode)

            writeInnerParts(
                innerTypesAsList,
                this,
                mode,
                indexOfParameterizedType + 1
            ) // inner parts separated by `.`
        }

        writeClassEnd()
    }

    private fun hasNothingInNonContravariantPosition(type: ConeKotlinType): Boolean = with(KotlinTypeMapper) {
        typeContext.hasNothingInNonContravariantPosition(type)
    }

    private fun FirClassLikeSymbol<*>.toRegularClassSymbol(): FirRegularClassSymbol? = when (this) {
        is FirRegularClassSymbol -> this
        is FirTypeAliasSymbol -> {
            val expandedType = fir.expandedTypeRef.coneType.fullyExpandedType(session) as? ConeClassLikeType
            expandedType?.lookupTag?.toSymbol(session) as? FirRegularClassSymbol
        }
        else -> null
    }

    private fun ConeClassLikeType.buildPossiblyInnerType(): PossiblyInnerConeType? =
        buildPossiblyInnerType(lookupTag.toSymbol(session)?.toRegularClassSymbol(), 0)

    private fun ConeClassLikeType.parentClassOrNull(): FirRegularClassSymbol? {
        val parentClassId = classId?.outerClassId ?: return null
        return session.firSymbolProvider.getClassLikeSymbolByFqName(parentClassId) as? FirRegularClassSymbol?
    }

    private fun ConeClassLikeType.buildPossiblyInnerType(classifier: FirRegularClassSymbol?, index: Int): PossiblyInnerConeType? {
        if (classifier == null) return null

        val firClass = classifier.fir
        val toIndex = firClass.typeParameters.count { it is FirTypeParameter } + index
        if (!firClass.isInner) {
            assert(toIndex == typeArguments.size || firClass.isLocal) {
                "${typeArguments.size - toIndex} trailing arguments were found in this type: ${render()}"
            }
            return PossiblyInnerConeType(classifier, typeArguments.toList().subList(index, typeArguments.size), null)
        }

        val argumentsSubList = typeArguments.toList().subList(index, toIndex)
        return PossiblyInnerConeType(
            classifier, argumentsSubList,
            buildPossiblyInnerType(firClass.defaultType().parentClassOrNull(), toIndex)
        )
    }

    private class PossiblyInnerConeType(
        val classifier: FirRegularClassSymbol,
        val arguments: List<ConeTypeProjection>,
        private val outerType: PossiblyInnerConeType?
    ) {
        fun segments(): List<PossiblyInnerConeType> = outerType?.segments().orEmpty() + this
    }

    private fun writeGenericArguments(
        sw: JvmSignatureWriter,
        type: PossiblyInnerConeType,
        mode: TypeMappingMode
    ) {
        val classifier = type.classifier.fir
        val defaultType = classifier.defaultType()
        val parameters = classifier.typeParameters.map { it.symbol }
        val arguments = type.arguments

        if ((defaultType.isFunctionalType(session) && arguments.size > BuiltInFunctionArity.BIG_ARITY)
            || defaultType.isKFunctionType(session)
        ) {
            writeGenericArguments(sw, listOf(arguments.last()), listOf(parameters.last()), mode)
            return
        }

        writeGenericArguments(sw, arguments, parameters, mode)
    }

    private fun writeGenericArguments(
        sw: JvmSignatureWriter,
        arguments: List<ConeTypeProjection>,
        parameterSymbols: List<FirTypeParameterSymbol>,
        mode: TypeMappingMode
    ) {
        with(KotlinTypeMapper) {
            val parameters = parameterSymbols.map { ConeTypeParameterLookupTag(it) }
            typeContext.writeGenericArguments(sw, arguments, parameters, mode) { type, sw, mode ->
                mapType(type as ConeKotlinType, mode, sw)
            }
        }
    }

    private fun writeInnerParts(
        innerTypesAsList: List<PossiblyInnerConeType>,
        sw: JvmSignatureWriter,
        mode: TypeMappingMode,
        index: Int
    ) {
        for (innerPart in innerTypesAsList.subList(index, innerTypesAsList.size)) {
            sw.writeInnerClass(getJvmShortName(innerPart.classifier.fir))
            writeGenericArguments(sw, innerPart, mode)
        }
    }

    internal fun getJvmShortName(klass: FirRegularClass): String {
        val result = runIf(!klass.isLocal) {
            klass.classId.asSingleFqName().toUnsafe().let { JavaToKotlinClassMap.mapKotlinToJava(it)?.shortClassName?.asString() }
        }
        return result ?: SpecialNames.safeIdentifier(klass.name).identifier
    }
}

val FirSession.jvmTypeMapper: FirJvmTypeMapper by FirSession.sessionComponentAccessor()

class ConeTypeSystemCommonBackendContextForTypeMapping(
    val context: ConeTypeContext
) : TypeSystemCommonBackendContext by context, TypeSystemCommonBackendContextForTypeMapping {
    private val session = context.session
    private val symbolProvider = session.firSymbolProvider

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
        return isSuspendFunctionType(session)
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
            val classSymbol = it.safeAs<ConeClassLikeType>()?.fullyExpandedType(session)
                ?.lookupTag?.toSymbol(session) as? FirRegularClassSymbol ?: return@firstOrNull false
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
