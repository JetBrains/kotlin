/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirTypeResolveTransformer(val superTypesOnly: Boolean = false) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return if (superTypesOnly) {
            element.compose()
        } else {
            (element.transformChildren(this, data) as E).compose()
        }
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
                FirStarImportingScope(file.imports)
            )
        )
        packageFqName = file.packageFqName
        return super.transformFile(file, data)
    }

    private fun lookupSuperTypes(klass: FirClass): List<ClassId> {
        val superTypesBuilder = SuperClassHierarchyBuilder()
        klass.superTypes.any { it.accept(superTypesBuilder, null) }
        return superTypesBuilder.classes
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

    private inner class SuperTypeResolver : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformType(type: FirType, data: Nothing?): CompositeTransformResult<FirType> {
            val typeResolver = FirTypeResolver.getInstance(type.session)
            val symbol = typeResolver.resolveToSymbol(type, scope, position = FirPosition.SUPER_TYPE_OR_EXPANSION)
            val myTransformer = this@FirTypeResolveTransformer

            if (type !is FirUserType) return myTransformer.transformType(type, data)
            val classId = (symbol as? ConeClassLikeSymbol)?.classId ?: return myTransformer.transformType(type, data)
            val firProvider = FirProvider.getInstance(type.session)

            val classes = generateSequence(classId) { it.outerClassId }.toList()

            val transformer = FirTypeResolveTransformer(superTypesOnly = true)

            val file = firProvider.getFirClassifierContainerFile(classes.last())

            file.transformSingle(transformer, null)

            classes.forEach {
                firProvider.getFirClassifierByFqName(it)!!.transformSingle(transformer, data)
            }


            type.transformChildren(myTransformer, null)
            return myTransformer.transformType(type, typeResolver.resolveUserType(type, symbol, scope))
        }
    }

    private inner class SuperClassHierarchyBuilder : FirVisitor<Boolean, Nothing?>() {

        override fun visitElement(element: FirElement, data: Nothing?): Boolean {
            return false
        }

        private tailrec fun ConeClassLikeType.computePartialExpansion(): ClassId {
            return when (this) {
                !is ConeAbbreviatedType -> this.symbol.classId
                else -> this.directExpansion.computePartialExpansion()
            }
        }

        val classes = mutableListOf<ClassId>()

        override fun visitResolvedType(resolvedType: FirResolvedType, data: Nothing?): Boolean {
            val provider = FirProvider.getInstance(resolvedType.session)
            val targetClassId = resolvedType.coneTypeSafe<ConeClassLikeType>()?.computePartialExpansion() ?: return false
            val classifier = provider.getFirClassifierByFqName(targetClassId)!!
            when (classifier) {
                is FirClass -> {
                    if (classifier.classKind == ClassKind.CLASS) {
                        classes += targetClassId
                        classifier.superTypes.any { it.accept(this, data) }
                    }
                }
                is FirTypeAlias -> classifier.expandedType.accept(this, data)
            }
            return true
        }

    }
}