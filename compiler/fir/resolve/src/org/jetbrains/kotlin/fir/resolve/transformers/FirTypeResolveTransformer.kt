/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

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
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedFunctionTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

open class FirTypeResolveTransformer(
    private val traversedClassifiers: Set<FirMemberDeclaration> = setOf()
) : FirAbstractTreeTransformerWithSuperTypes(reversedScopePriority = true) {
    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val session = file.session
        return withScopeCleanup {
            towerScope.scopes += listOf(
                // from low priority to high priority
                FirDefaultStarImportingScope(session),
                FirExplicitStarImportingScope(file.imports, session),
                FirDefaultSimpleImportingScope(session),
                FirSelfImportingScope(file.packageFqName, session),
                // TODO: explicit simple importing scope should have highest priority (higher than inner scopes added in process)
                FirExplicitSimpleImportingScope(file.imports, session)
            )
            super.transformFile(file, data)
        }
    }

    private fun resolveSuperTypesAndExpansions(element: FirMemberDeclaration) {
        try {
            element.transformChildren(SuperTypeResolver(traversedClassifiers + listOfNotNull(element)), null)
        } catch (e: Exception) {
            class SuperTypeResolveException(cause: Exception) : Exception(element.render(), cause)
            throw SuperTypeResolveException(e)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        withScopeCleanup {
            regularClass.addTypeParametersScope()
            resolveSuperTypesAndExpansions(regularClass)
            regularClass.typeParameters.forEach {
                it.accept(this, data)
            }
        }
        return withScopeCleanup {
            val firProvider = FirProvider.getInstance(regularClass.session)
            val classId = regularClass.symbol.classId
            lookupSuperTypes(regularClass, lookupInterfaces = false, deep = true).asReversed().mapTo(towerScope.scopes) {
                FirNestedClassifierScope(it.symbol.classId, FirSymbolProvider.getInstance(regularClass.session))
            }
            val companionObjects = regularClass.declarations.filterIsInstance<FirRegularClass>().filter { it.isCompanion }
            for (companionObject in companionObjects) {
                towerScope.scopes += FirNestedClassifierScope(companionObject.symbol.classId, firProvider)
            }
            towerScope.scopes += FirNestedClassifierScope(classId, firProvider)
            regularClass.addTypeParametersScope()

            super.transformRegularClass(regularClass, data)
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            typeAlias.addTypeParametersScope()
            resolveSuperTypesAndExpansions(typeAlias)
            super.transformTypeAlias(typeAlias, data)
        }
    }


    private fun FirMemberDeclaration.addTypeParametersScope() {
        val scopes = towerScope.scopes
        if (typeParameters.isNotEmpty()) {
            scopes += FirMemberTypeParameterScope(this)
        }
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            property.addTypeParametersScope()
            super.transformProperty(property, data)
        }
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            namedFunction.addTypeParametersScope()
            super.transformNamedFunction(namedFunction, data)
        }
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        val typeResolver = FirTypeResolver.getInstance(typeRef.session)
        typeRef.transformChildren(this, null)
        return transformType(typeRef, typeResolver.resolveType(typeRef, towerScope, position = FirPosition.OTHER))
    }

    override fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        val typeResolver = FirTypeResolver.getInstance(functionTypeRef.session)
        functionTypeRef.transformChildren(this, data)
        return FirResolvedFunctionTypeRefImpl(
            functionTypeRef.psi,
            functionTypeRef.session,
            functionTypeRef.isMarkedNullable,
            functionTypeRef.annotations as MutableList<FirAnnotationCall>,
            functionTypeRef.receiverTypeRef,
            functionTypeRef.valueParameters as MutableList<FirValueParameter>,
            functionTypeRef.returnTypeRef,
            typeResolver.resolveType(functionTypeRef, towerScope, FirPosition.OTHER)
        ).compose()
    }

    private fun transformType(typeRef: FirTypeRef, resolvedType: ConeKotlinType): CompositeTransformResult<FirTypeRef> {
        return FirResolvedTypeRefImpl(
            typeRef.session,
            typeRef.psi,
            resolvedType,
            false,
            typeRef.annotations
        ).compose()
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return resolvedTypeRef.compose()
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

    private class SuperTypeResolveTransformer(
        val elementIterator: Iterator<FirElement>,
        traversedClassifiers: Set<FirMemberDeclaration>
    ) : FirTypeResolveTransformer(traversedClassifiers) {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            if (elementIterator.hasNext()) elementIterator.next().transformSingle(this, data)
            return element.compose()
        }
    }

    private inner class SuperTypeResolver(val traversedClassifiers: Set<FirMemberDeclaration>) : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }


        private fun walkSymbols(symbol: ConeSymbol) {
            if (symbol is ConeClassLikeSymbol) {
                if (symbol is FirBasedSymbol<*>) {
                    val classId = symbol.classId

                    if (symbol is ConeTypeAliasSymbol) {
                        val fir = symbol.fir as FirTypeAlias
                        if (fir.expandedTypeRef is FirResolvedTypeRef) return
                    } else if (symbol is ConeClassSymbol) {
                        val fir = symbol.fir as FirClass
                        if (fir.superTypeRefs.all { it is FirResolvedTypeRef }) return
                    }
                    val firProvider = FirProvider.getInstance(symbol.fir.session)
                    val classes = generateSequence(classId) { it.outerClassId }.toList().asReversed()

                    val file = firProvider.getFirClassifierContainerFile(classes.first())

                    val firElementsToVisit = classes.asSequence().map {
                        firProvider.getFirClassifierByFqName(it)!!
                    }

                    val transformer = SuperTypeResolveTransformer(
                        firElementsToVisit.iterator(), traversedClassifiers
                    )
                    file.transformSingle(transformer, null)

                } else {
                    if (symbol is FirTypeAliasSymbol) {
                        symbol.fir.expandedConeType?.let { if (it !is ConeClassErrorType) walkSymbols(it.symbol) }
                    } else if (symbol is FirClassSymbol) {
                        symbol.fir.superConeTypes.forEach { if (it !is ConeClassErrorType) walkSymbols(it.symbol) }
                    }
                }
            }
        }

        override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
            val typeResolver = FirTypeResolver.getInstance(typeRef.session)
            val symbol = typeResolver.resolveToSymbol(typeRef, towerScope, position = FirPosition.SUPER_TYPE_OR_EXPANSION)
            val myTransformer = this@FirTypeResolveTransformer

            if (symbol != null) {
                if (symbol is AbstractFirBasedSymbol<*> && symbol.fir in traversedClassifiers) {
                    return FirErrorTypeRefImpl(typeRef.session, typeRef.psi, "Recursion detected: ${typeRef.render()}").compose()
                } else {
                    walkSymbols(symbol)
                }
            }

            if (typeRef !is FirUserTypeRef) return typeRef.transform(myTransformer, data)


            typeRef.transformChildren(myTransformer, null)
            return myTransformer.transformType(typeRef, typeResolver.resolveUserType(typeRef, symbol, towerScope))
        }
    }
}