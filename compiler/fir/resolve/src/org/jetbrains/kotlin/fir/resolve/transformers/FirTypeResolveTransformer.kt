/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedFunctionTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

open class FirTypeResolveTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    lateinit var scope: FirCompositeScope

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val session = file.session
        scope = FirCompositeScope(
            mutableListOf(
                // from high priority to low priority
                FirExplicitSimpleImportingScope(file.imports, session),
                FirSelfImportingScope(file.packageFqName, session),
                FirDefaultSimpleImportingScope(session),
                FirExplicitStarImportingScope(file.imports, session),
                FirDefaultStarImportingScope(session)
            )
        )
        return super.transformFile(file, data)
    }

    private fun lookupSuperTypes(klass: FirClass): List<ConeClassLikeType> {
        return mutableListOf<ConeClassLikeType>().also { klass.symbol.collectSuperTypes(it) }
    }

    private fun resolveSuperTypesAndExpansions(element: FirMemberDeclaration) {
        try {
            element.transformChildren(SuperTypeResolver(), null)
        } catch (e: Exception) {
            class SuperTypeResolveException(cause: Exception) : Exception(element.render(), cause)
            throw SuperTypeResolveException(e)
        }
    }

    override fun transformClass(klass: FirClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            klass.withTypeParametersScope {
                resolveSuperTypesAndExpansions(klass)

                val firProvider = FirProvider.getInstance(klass.session)
                val classId = klass.symbol.classId
                scope.scopes += FirNestedClassifierScope(classId, firProvider)
                val companionObjects = klass.declarations.filterIsInstance<FirClass>().filter { it.isCompanion }
                for (companionObject in companionObjects) {
                    scope.scopes += FirNestedClassifierScope(companionObject.symbol.classId, firProvider)
                }

                lookupSuperTypes(klass).mapTo(scope.scopes) {
                    val symbol = it.symbol
                    if (symbol is FirBasedSymbol<*>) {
                        FirNestedClassifierScope(symbol.classId, FirProvider.getInstance(symbol.fir.session))
                    } else {
                        FirNestedClassifierScope(symbol.classId, FirSymbolProvider.getInstance(klass.session))
                    }
                }
                super.transformClass(klass, data)
            }
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        // TODO: Remove comment when KT-23742 fixed
        // Warning: boxing inline class here ()
        return typeAlias.withTypeParametersScope {
            resolveSuperTypesAndExpansions(typeAlias)
            super.transformTypeAlias(typeAlias, data)
        }
    }


    private inline fun <T> FirMemberDeclaration.withTypeParametersScope(crossinline l: () -> T): T {
        val scopes = scope.scopes
        if (typeParameters.isNotEmpty()) {
            scopes += FirMemberTypeParameterScope(this)
        }
        val result = l()
        if (typeParameters.isNotEmpty()) {
            scopes.removeAt(scopes.lastIndex)
        }
        return result
    }

    private inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val scopeBefore = scope
        val scopes = scope.scopes
        val sizeBefore = scopes.size
        val result = l()
        scope = scopeBefore
        assert(scopes.size >= sizeBefore)
        scopes.subList(sizeBefore + 1, scopes.size).clear()
        return result
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return property.withTypeParametersScope {
            super.transformProperty(property, data)
        }
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return namedFunction.withTypeParametersScope {
            super.transformNamedFunction(namedFunction, data)
        }
    }

    override fun transformType(type: FirType, data: Nothing?): CompositeTransformResult<FirType> {
        val typeResolver = FirTypeResolver.getInstance(type.session)
        type.transformChildren(this, null)
        return transformType(type, typeResolver.resolveType(type, scope, position = FirPosition.OTHER))
    }

    override fun transformFunctionType(functionType: FirFunctionType, data: Nothing?): CompositeTransformResult<FirType> {
        val typeResolver = FirTypeResolver.getInstance(functionType.session)
        functionType.transformChildren(this, data)
        return FirResolvedFunctionTypeImpl(
            functionType.psi,
            functionType.session,
            functionType.isNullable,
            functionType.annotations as MutableList<FirAnnotationCall>,
            functionType.receiverType,
            functionType.valueParameters as MutableList<FirValueParameter>,
            functionType.returnType,
            typeResolver.resolveType(functionType, scope, FirPosition.OTHER)
        ).compose()
    }

    private fun transformType(type: FirType, resolvedType: ConeKotlinType): CompositeTransformResult<FirType> {
        return FirResolvedTypeImpl(
            type.session,
            type.psi,
            resolvedType,
            false,
            type.annotations
        ).compose()
    }

    override fun transformResolvedType(resolvedType: FirResolvedType, data: Nothing?): CompositeTransformResult<FirType> {
        return resolvedType.compose()
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return valueParameter.also { it.transformChildren(this, data) }.compose()
    }


    override fun transformTypeProjectionWithVariance(
        typeProjectionWithVariance: FirTypeProjectionWithVariance,
        data: Nothing?
    ): CompositeTransformResult<FirTypeProjection> {
        typeProjectionWithVariance.transformChildren(this, data)
        return typeProjectionWithVariance.compose()
    }

    private class SuperTypeResolveTransformer(val elementIterator: Iterator<FirElement>) : FirTypeResolveTransformer() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            if (elementIterator.hasNext()) elementIterator.next().transformSingle(this, data)
            return element.compose()
        }
    }

    private inner class SuperTypeResolver : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }


        private fun walkSymbols(symbol: ConeSymbol) {
            if (symbol is ConeClassLikeSymbol) {
                if (symbol is FirBasedSymbol<*>) {
                    val classId = symbol.classId
                    val firProvider = FirProvider.getInstance(symbol.fir.session)

                    val classes = generateSequence(classId) { it.outerClassId }.toList().asReversed()


                    val file = firProvider.getFirClassifierContainerFile(classes.first())

                    val firElementsToVisit = classes.asSequence().map {
                        firProvider.getFirClassifierByFqName(it)!!
                    }

                    val transformer = SuperTypeResolveTransformer(
                        firElementsToVisit.iterator()
                    )
                    file.transformSingle(transformer, null)

                } else {
                    if (symbol is ConeTypeAliasSymbol) {
                        symbol.expansionType?.let { if (it !is ConeClassErrorType) walkSymbols(it.symbol) }
                    } else if (symbol is ConeClassSymbol) {
                        symbol.superTypes.forEach { if (it !is ConeClassErrorType) walkSymbols(it.symbol) }
                    }
                }
            }
        }

        override fun transformType(type: FirType, data: Nothing?): CompositeTransformResult<FirType> {
            val typeResolver = FirTypeResolver.getInstance(type.session)
            val symbol = typeResolver.resolveToSymbol(type, scope, position = FirPosition.SUPER_TYPE_OR_EXPANSION)
            val myTransformer = this@FirTypeResolveTransformer

            if (symbol != null) walkSymbols(symbol)

            if (type !is FirUserType) return type.transform(myTransformer, data)


            type.transformChildren(myTransformer, null)
            return myTransformer.transformType(type, typeResolver.resolveUserType(type, symbol, scope))
        }
    }

    private tailrec fun ConeClassLikeType.computePartialExpansion(): ConeClassLikeType? {
        return when (this) {
            is ConeAbbreviatedType -> directExpansion.takeIf { it !is ConeClassErrorType }?.computePartialExpansion()
            else -> return this
        }
    }

    private tailrec fun ConeClassLikeSymbol.collectSuperTypes(list: MutableList<ConeClassLikeType>) {
        return when (this) {
            is ConeClassSymbol -> {
                val superClassType =
                    this.superTypes
                        .map { it.computePartialExpansion() }
                        .firstOrNull {
                            it !is ConeClassErrorType && (it?.symbol as? ConeClassSymbol)?.kind == ClassKind.CLASS
                        } ?: return
                list += superClassType
                superClassType.symbol.collectSuperTypes(list)
            }
            is ConeTypeAliasSymbol -> {
                val expansion = expansionType?.computePartialExpansion() ?: return
                expansion.symbol.collectSuperTypes(list)
            }
            else -> error("?!id:1")
        }
    }

}