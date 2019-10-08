/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirNestedClassifierScope
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.ClassId

class FirSupertypeResolverTransformer : FirAbstractTreeTransformer(phase = FirResolvePhase.SUPER_TYPES) {
    override lateinit var session: FirSession
    private lateinit var file: FirFile

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        initFromFile(file)
        return transformElement(file, data)
    }

    fun initFromFile(file: FirFile) {
        session = file.fileSession
        this.file = file
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val transformedClass = resolveSupertypesOrExpansions(regularClass) as? FirRegularClass ?: regularClass

        // resolve supertypes for nested classes
        return transformElement(transformedClass, data)
    }


    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val result = resolveSupertypesOrExpansions(typeAlias)
        return result.compose()
    }

    // This and transformProperty functions are required to forbid supertype resolving for local classes
//    override fun transformDeclarationWithBody(
//        declarationWithBody: FirDeclarationWithBody,
//        data: Nothing?
//    ): CompositeTransformResult<FirDeclaration> {
//        return declarationWithBody.compose()
//    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return anonymousInitializer.compose()
    }

    override fun transformConstructor(constructor: FirConstructor, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return constructor.compose()
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return namedFunction.compose()
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return property.compose()
    }

    override fun <F : FirFunction<F>> transformFunction(function: FirFunction<F>, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return function.compose()
    }

    private fun resolveSupertypesOrExpansions(classLikeDeclaration: FirClassLikeDeclaration<*>): FirDeclaration {
        if (classLikeDeclaration.areSupertypesComputed() || classLikeDeclaration.areSupertypesComputing()) {
            return classLikeDeclaration
        }

        val classId = classLikeDeclaration.symbol.classId

        val visitor = ResolveSuperTypesTask(session, classId, file, classLikeDeclaration)
        file.accept(visitor, null).single

        return visitor.resultingClass
    }

    private class ResolveSuperTypesTask(
        override val session: FirSession,
        private val requestedClassId: ClassId,
        file: FirFile,
        private val knownFirClassLikeDeclaration: FirClassLikeDeclaration<*>? = null
    ) : FirAbstractTreeTransformerWithSuperTypes(phase = FirResolvePhase.SUPER_TYPES, reversedScopePriority = true) {

        lateinit var resultingClass: FirDeclaration

        init {
            val scopeSession = ScopeSession()
            towerScope.addImportingScopes(file, session, scopeSession)
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
            val classId = regularClass.classId
            if (!isOuterClass(classId, requestedClassId)) return regularClass.compose()
            val transformedClass = withScopeCleanup {
                if (regularClass.areSupertypesComputed() || regularClass.areSupertypesComputing()) return@withScopeCleanup regularClass

                regularClass.addTypeParametersScope()

                val transformer = FirSpecificTypeResolverTransformer(towerScope, FirPosition.SUPER_TYPE_OR_EXPANSION, session)
                val resolvedTypesRefs = regularClass.superTypeRefs.map { transformer.transformTypeRef(it, data).single }

                val resultingTypeRefs = resolveLoops(regularClass, classId, resolvedTypesRefs)
                regularClass.replaceSupertypes(resultingTypeRefs)
            }

            if (regularClass.matchesRequestedDeclaration()) {
                resultingClass = transformedClass
                return transformedClass.compose()
            }

            return resolveNestedClassesSupertypes(transformedClass, data)
        }

        private fun FirClassLikeDeclaration<*>.matchesRequestedDeclaration(): Boolean {
            if (knownFirClassLikeDeclaration != null) return knownFirClassLikeDeclaration == this
            return symbol.classId == requestedClassId
        }

        override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
            val classId = typeAlias.symbol.classId
            // nested type aliases
            if (typeAlias.areSupertypesComputed() || !typeAlias.matchesRequestedDeclaration()) return typeAlias.compose()

            return withScopeCleanup {
                typeAlias.addTypeParametersScope()

                val transformer = FirSpecificTypeResolverTransformer(towerScope, FirPosition.SUPER_TYPE_OR_EXPANSION, session)
                val resolvedTypesRef = transformer.transformTypeRef(typeAlias.expandedTypeRef, data).single
                val resultingTypeRef = resolveLoops(typeAlias, classId, listOf(resolvedTypesRef)).firstOrNull()

                typeAlias.replaceExpandTypeRef(resultingTypeRef ?: resolvedTypesRef).also {
                    resultingClass = it
                }
            }.compose()
        }

        private fun resolveLoops(
            firClassLikeDeclaration: FirClassLikeDeclaration<*>,
            classId: ClassId,
            resolvedTypesRefs: List<FirTypeRef>
        ): List<FirTypeRef> {
            firClassLikeDeclaration.supertypesComputationStatus = FirClassLikeDeclaration.SupertypesComputationStatus.COMPUTING

            val resultingTypeRefs = mutableListOf<FirTypeRef>()
            for (superTypeRef in resolvedTypesRefs) {
                val resolvedType = superTypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
                val superTypeClassId = resolvedType.lookupTag.classId

                if (superTypeClassId.outerClasses().any { it.areSupertypesComputing() }) {
                    resultingTypeRefs.add(
                        FirErrorTypeRefImpl(superTypeRef.psi, "Recursion detected: ${superTypeRef.render()}")
                    )

                    continue
                }

                val sessionForSupertype = session.getService(FirSymbolProvider::class).getSessionForClass(superTypeClassId) ?: continue

                val firClassForSupertype =
                    sessionForSupertype
                        .getService(FirSymbolProvider::class)
                        .getClassLikeSymbolByFqName(superTypeClassId)
                        ?.fir

                // TODO: this if is a temporary hack for built-in types (because we can't load file for them)
                if (firClassForSupertype == null ||
                    (firClassForSupertype is FirClass &&
                            firClassForSupertype.superTypeRefs.any { it !is FirResolvedTypeRef })
                ) {
                    val provider = sessionForSupertype.getService(FirProvider::class)
                    val firForSuperClassFile = provider.getFirClassifierContainerFile(superTypeClassId)

                    ResolveSuperTypesTask(
                        sessionForSupertype, superTypeClassId, firForSuperClassFile
                    ).transformFile(firForSuperClassFile, null)
                }

                resultingTypeRefs.add(superTypeRef)
            }

            firClassLikeDeclaration.supertypesComputationStatus = FirClassLikeDeclaration.SupertypesComputationStatus.COMPUTED
            return resultingTypeRefs
        }

        private fun ClassId.areSupertypesComputing(): Boolean {
            val fir = session.firSymbolProvider.getClassLikeSymbolByFqName(this)?.fir ?: return false
            return fir.areSupertypesComputing()
        }

        private fun resolveNestedClassesSupertypes(
            regularClass: FirRegularClass,
            data: Nothing?
        ): CompositeTransformResult<FirDeclaration> {
            return withScopeCleanup {
                // ? Is it Ok to use original file session here ?
                val firProvider = FirProvider.getInstance(session)
                val classId = regularClass.symbol.classId
                lookupSuperTypes(regularClass, lookupInterfaces = false, deep = true, useSiteSession = session)
                    .asReversed().mapTo(towerScope.scopes) {
                        FirNestedClassifierScope(it.lookupTag.classId, FirSymbolProvider.getInstance(session))
                    }
                val companionObjects = regularClass.declarations.filterIsInstance<FirRegularClass>().filter { it.isCompanion }
                for (companionObject in companionObjects) {
                    towerScope.scopes += FirNestedClassifierScope(companionObject.symbol.classId, firProvider)
                }
                towerScope.scopes += FirNestedClassifierScope(classId, firProvider)
                regularClass.addTypeParametersScope()

                transformElement(regularClass, data)
            }
        }
    }
}

private fun isOuterClass(outerCandidate: ClassId, innerCandidate: ClassId) =
    innerCandidate.outerClasses().any { outerCandidate == it }

private fun ClassId.outerClasses() = generateSequence(this, ClassId::getOuterClassId)

private fun FirClassLikeDeclaration<*>.areSupertypesComputed() =
    supertypesComputationStatus == FirClassLikeDeclaration.SupertypesComputationStatus.COMPUTED
private fun FirClassLikeDeclaration<*>.areSupertypesComputing() =
    supertypesComputationStatus == FirClassLikeDeclaration.SupertypesComputationStatus.COMPUTING
