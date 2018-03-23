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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

open class FirTypeResolveTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    lateinit var scope: FirCompositeScope
    lateinit var packageFqName: FqName
    private var classLikeName: FqName = FqName.ROOT

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        scope = FirCompositeScope(
            mutableListOf(
                // from high priority to low priority
                FirExplicitImportingScope(file.imports),
                FirSelfImportingScope(file.packageFqName, file.session),
                FirExplicitStarImportingScope(file.imports, file.session),
                FirDefaultStarImportingScope(file.session)
            )
        )
        packageFqName = file.packageFqName
        return super.transformFile(file, data)
    }

    private fun lookupSuperTypes(klass: FirClass): List<ClassId> {
        return mutableListOf<ConeClassLikeType>().also { klass.symbol.collectSuperTypes(it) }.map { it.symbol.classId }
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
        classLikeName = classLikeName.child(klass.name)
        val classId = ClassId(packageFqName, classLikeName, false)
        scope = FirCompositeScope(mutableListOf(scope))
        scope.scopes += FirClassLikeTypeParameterScope(klass)
        resolveSuperTypesAndExpansions(klass)

        scope.scopes += FirNestedClassifierScope(classId, klass.session)
        val superTypeScopes = lookupSuperTypes(klass).map { FirNestedClassifierScope(it, klass.session) }
        scope.scopes.addAll(superTypeScopes)
        val result = super.transformClass(klass, data)
        scope = scope.scopes[0] as FirCompositeScope
        classLikeName = classLikeName.parent()

        return result
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        classLikeName = classLikeName.child(typeAlias.name)
        if (typeAlias.typeParameters.isNotEmpty()) {
            scope = FirCompositeScope(mutableListOf(scope))
            scope.scopes += FirClassLikeTypeParameterScope(typeAlias)
        }

        resolveSuperTypesAndExpansions(typeAlias)

        if (typeAlias.typeParameters.isNotEmpty()) {
            scope = scope.scopes[0] as FirCompositeScope
        }
        classLikeName = classLikeName.parent()
        return super.transformTypeAlias(typeAlias, data)
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        if (namedFunction.typeParameters.isNotEmpty()) {
            scope = FirCompositeScope(mutableListOf(scope))
            scope.scopes += FirFunctionTypeParameterScope(namedFunction)
        }

        val result = super.transformNamedFunction(namedFunction, data)
        if (namedFunction.typeParameters.isNotEmpty()) {
            scope = scope.scopes[0] as FirCompositeScope
        }

        return result
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
                        symbol.expansionType?.let { walkSymbols(it.symbol) }
                    } else if (symbol is ConeClassSymbol) {
                        symbol.superTypes.forEach { walkSymbols(it.symbol) }
                    }
                }
            }
        }

        override fun transformType(type: FirType, data: Nothing?): CompositeTransformResult<FirType> {
            val typeResolver = FirTypeResolver.getInstance(type.session)
            val symbol = typeResolver.resolveToSymbol(type, scope, position = FirPosition.SUPER_TYPE_OR_EXPANSION)
            val myTransformer = this@FirTypeResolveTransformer

            if (symbol != null) walkSymbols(symbol)

            if (type !is FirUserType) return myTransformer.transformType(type, data)


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
                        .firstOrNull { (it?.symbol as? ConeClassSymbol)?.kind == ClassKind.CLASS } ?: return
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