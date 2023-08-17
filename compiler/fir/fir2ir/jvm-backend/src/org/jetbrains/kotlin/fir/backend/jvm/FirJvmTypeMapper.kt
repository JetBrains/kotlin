/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedTypeQualifierError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeMapper
import org.jetbrains.kotlin.types.TypeMappingContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContextForTypeMapping
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.org.objectweb.asm.Type

class FirJvmTypeMapper(val session: FirSession) : FirSessionComponent {
    companion object {
        val NON_EXISTENT_ID = ClassId.topLevel(StandardNames.NON_EXISTENT_CLASS)
        private val typeForNonExistentClass = NON_EXISTENT_ID.toLookupTag().constructClassType(ConeTypeProjection.EMPTY_ARRAY, isNullable = false)
    }

    fun mapType(
        type: ConeKotlinType,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
        sw: JvmSignatureWriter? = null,
        unresolvedQualifierRemapper: ((String) -> String?)? = null
    ): Type {
        val context = if (unresolvedQualifierRemapper != null) {
            Context(unresolvedQualifierRemapper)
        } else {
            defaultContext
        }
        return AbstractTypeMapper.mapType(context, type, mode, sw)
    }

    fun isPrimitiveBacked(type: ConeKotlinType): Boolean =
        AbstractTypeMapper.isPrimitiveBacked(defaultContext, type)

    private val defaultContext = Context { null }
    val typeContext: TypeSystemCommonBackendContext
        get() = defaultContext.typeContext

    private inner class Context(unresolvedQualifierRemapper: (String) -> String?) : TypeMappingContext<JvmSignatureWriter> {
        private fun mapType(type: ConeKotlinType, mode: TypeMappingMode = TypeMappingMode.DEFAULT, sw: JvmSignatureWriter? = null): Type {
            return AbstractTypeMapper.mapType(this, type, mode, sw)
        }

        override val typeContext = ConeTypeSystemCommonBackendContextForTypeMapping(session.typeContext, unresolvedQualifierRemapper)

        override fun getClassInternalName(typeConstructor: TypeConstructorMarker): String {
            require(typeConstructor is ConeClassLikeLookupTag)
            val classId = typeConstructor.classId
            val name = if (classId.isLocal) safeShortClassName(classId) else classId.asString()
            return name.replace(".", "$")
        }

        override fun getScriptInternalName(typeConstructor: TypeConstructorMarker): String =
            TODO("Not yet implemented")

        override fun JvmSignatureWriter.writeGenericType(type: KotlinTypeMarker, asmType: Type, mode: TypeMappingMode) {
            if (type !is ConeKotlinType) return
            if (skipGenericSignature() || hasNothingInNonContravariantPosition(type) || type.typeArguments.isEmpty()) {
                writeAsmType(asmType)
                return
            }

            val possiblyInnerType = type.buildPossiblyInnerType()

            val innerTypesAsList = possiblyInnerType.segments()

            val indexOfParameterizedType = innerTypesAsList.indexOfFirst { innerPart -> innerPart.arguments.isNotEmpty() }
            if (indexOfParameterizedType < 0 || innerTypesAsList.size == 1) {
                writeClassBegin(asmType)
                writeGenericArguments(this, possiblyInnerType, mode)
            } else {
                val outerType = innerTypesAsList[indexOfParameterizedType]

                writeOuterClassBegin(asmType, mapType(outerType.classifier?.fir?.defaultType() ?: typeForNonExistentClass).internalName)
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

        private fun ConeKotlinType.buildPossiblyInnerType(): PossiblyInnerConeType {
            fun createForError(): PossiblyInnerConeType {
                return PossiblyInnerConeType(classifier = null, typeArguments.toList(), outerType = null)
            }

            if (this !is ConeClassLikeType) return createForError()

            return when (val symbol = lookupTag.toSymbol(session)) {
                is FirRegularClassSymbol -> buildPossiblyInnerType(symbol, 0)
                is FirTypeAliasSymbol -> {
                    val expandedType = fullyExpandedType(session) as? ConeClassLikeType
                    val classSymbol = expandedType?.lookupTag?.toSymbol(session) as? FirRegularClassSymbol
                    classSymbol?.let { expandedType.buildPossiblyInnerType(it, 0) }
                }
                else -> null
            } ?: createForError()
        }

        private fun ConeClassLikeType.parentClassOrNull(): FirRegularClassSymbol? {
            val parentClassId = classId?.outerClassId ?: return null
            return session.symbolProvider.getClassLikeSymbolByClassId(parentClassId) as? FirRegularClassSymbol?
        }

        private fun ConeClassLikeType.buildPossiblyInnerType(classifier: FirRegularClassSymbol?, index: Int): PossiblyInnerConeType? {
            if (classifier == null) return null

            val firClass = classifier.fir
            val toIndex = firClass.typeParameters.count { it is FirTypeParameter } + index
            if (!firClass.isInner) {
                assert(toIndex == typeArguments.size || firClass.isLocal) {
                    "${typeArguments.size - toIndex} trailing arguments were found in this type: ${renderForDebugging()}"
                }
                return PossiblyInnerConeType(classifier, typeArguments.toList().subList(index, typeArguments.size), null)
            }

            val argumentsSubList = typeArguments.toList().subList(index, toIndex)
            return PossiblyInnerConeType(
                classifier, argumentsSubList,
                buildPossiblyInnerType(firClass.defaultType().parentClassOrNull(), toIndex)
            )
        }

        private fun writeGenericArguments(
            sw: JvmSignatureWriter,
            type: PossiblyInnerConeType,
            mode: TypeMappingMode
        ) {
            val classifier = type.classifier?.fir
            val defaultType = classifier?.defaultType() ?: typeForNonExistentClass
            val parameters = classifier?.typeParameters.orEmpty().map { it.symbol }
            val arguments = type.arguments

            if ((defaultType.functionTypeKind(session).let { it == FunctionTypeKind.Function || it == FunctionTypeKind.SuspendFunction } &&
                        (arguments.size > BuiltInFunctionArity.BIG_ARITY)) ||
                defaultType.isReflectFunctionType(session)
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
                sw.writeInnerClass(getJvmShortName(innerPart.classifier?.classId ?: NON_EXISTENT_ID))
                writeGenericArguments(sw, innerPart, mode)
            }
        }
    }

    private class PossiblyInnerConeType(
        val classifier: FirRegularClassSymbol?,
        val arguments: List<ConeTypeProjection>,
        private val outerType: PossiblyInnerConeType?
    ) {
        fun segments(): List<PossiblyInnerConeType> = outerType?.segments().orEmpty() + this
    }

    internal fun getJvmShortName(classId: ClassId): String {
        val result = runUnless(classId.isLocal) {
            classId.asSingleFqName().toUnsafe().let { JavaToKotlinClassMap.mapKotlinToJava(it)?.shortClassName?.asString() }
        }
        return result ?: safeShortClassName(classId)
    }

    private fun safeShortClassName(classId: ClassId): String {
        return SpecialNames.safeIdentifier(classId.shortClassName).identifier
    }
}

val FirSession.jvmTypeMapper: FirJvmTypeMapper by FirSession.sessionComponentAccessor()

class ConeTypeSystemCommonBackendContextForTypeMapping(
    val context: ConeTypeContext,
    val unresolvedQualifierRemapper: (String) -> String?
) : TypeSystemCommonBackendContext by context, TypeSystemCommonBackendContextForTypeMapping {
    private val session = context.session
    private val symbolProvider = session.symbolProvider

    override fun TypeConstructorMarker.isTypeParameter(): Boolean {
        return this is ConeTypeParameterLookupTag
    }

    override fun TypeConstructorMarker.asTypeParameter(): TypeParameterMarker {
        require(isTypeParameter())
        return this as ConeTypeParameterLookupTag
    }

    override fun TypeConstructorMarker.defaultType(): ConeSimpleKotlinType {
        require(this is ConeClassifierLookupTag)
        return when (this) {
            is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, isNullable = false)
            is ConeClassLikeLookupTag -> {
                val symbol = toSymbol(session) as? FirRegularClassSymbol
                    ?: return ConeErrorType(ConeUnresolvedSymbolError(classId))
                symbol.fir.defaultType()
            }
            else -> error("Unsupported type constructor: $this")
        }
    }

    override fun TypeConstructorMarker.isScript(): Boolean = false

    override fun SimpleTypeMarker.isSuspendFunction(): Boolean {
        require(this is ConeSimpleKotlinType)
        return isSuspendOrKSuspendFunctionType(session)
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
        val bounds = this.typeParameterSymbol.resolvedBounds.map { it.coneType }
        return bounds.firstOrNull {
            val classSymbol = (it as? ConeClassLikeType)
                ?.fullyExpandedType(session)
                ?.lookupTag
                ?.toSymbol(session) as? FirRegularClassSymbol
                ?: return@firstOrNull false
            val kind = classSymbol.fir.classKind
            kind != ClassKind.INTERFACE && kind != ClassKind.ANNOTATION_CLASS
        } ?: bounds.first()
    }

    override fun continuationTypeConstructor(): ConeClassLikeLookupTag {
        return possiblyErrorTypeConstructorByClassId(StandardClassIds.Continuation)
    }

    override fun functionNTypeConstructor(n: Int): TypeConstructorMarker {
        return symbolProvider.getClassLikeSymbolByClassId(StandardClassIds.FunctionN(n))?.toLookupTag()
            ?: error("Function$n class not found")
    }

    private fun possiblyErrorTypeConstructorByClassId(classId: ClassId): ConeClassLikeLookupTag {
        return symbolProvider.getClassLikeSymbolByClassId(classId)?.toLookupTag()
            ?: ConeClassLikeErrorLookupTag(classId)
    }

    override fun KotlinTypeMarker.getNameForErrorType(): String? {
        require(this is ConeErrorType)
        val result = when (val diagnostic = diagnostic) {
            is ConeUnresolvedTypeQualifierError -> diagnostic.qualifier
            else -> null
        }
        return result?.let { unresolvedQualifierRemapper(it) ?: it }
    }
}
