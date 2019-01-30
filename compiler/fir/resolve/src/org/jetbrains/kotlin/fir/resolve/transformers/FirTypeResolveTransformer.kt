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
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedFunctionTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeImpl
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
            lookupSuperTypes(regularClass, lookupInterfaces = false).asReversed().mapTo(towerScope.scopes) {
                val symbol = it.symbol
                if (symbol is FirBasedSymbol<*>) {
                    FirNestedClassifierScope(symbol.classId, FirProvider.getInstance(symbol.fir.session))
                } else {
                    FirNestedClassifierScope(symbol.classId, FirSymbolProvider.getInstance(regularClass.session))
                }
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

    override fun transformType(type: FirType, data: Nothing?): CompositeTransformResult<FirType> {
        val typeResolver = FirTypeResolver.getInstance(type.session)
        type.transformChildren(this, null)
        return transformType(type, typeResolver.resolveType(type, towerScope, position = FirPosition.OTHER))
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
            typeResolver.resolveType(functionType, towerScope, FirPosition.OTHER)
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
            val symbol = typeResolver.resolveToSymbol(type, towerScope, position = FirPosition.SUPER_TYPE_OR_EXPANSION)
            val myTransformer = this@FirTypeResolveTransformer

            if (symbol != null) {
                if (symbol is AbstractFirBasedSymbol<*> && symbol.fir in traversedClassifiers) {
                    return FirErrorTypeImpl(type.session, type.psi, "Recursion detected: ${type.render()}").compose()
                } else {
                    walkSymbols(symbol)
                }
            }

            if (type !is FirUserType) return type.transform(myTransformer, data)


            type.transformChildren(myTransformer, null)
            return myTransformer.transformType(type, typeResolver.resolveUserType(type, symbol, towerScope))
        }
    }
}