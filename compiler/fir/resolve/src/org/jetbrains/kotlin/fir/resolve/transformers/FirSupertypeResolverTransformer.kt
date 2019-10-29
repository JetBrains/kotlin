/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.ClassId

class FirSupertypeResolverTransformer : FirAbstractTreeTransformer<Nothing?>(phase = FirResolvePhase.SUPER_TYPES) {
    override lateinit var session: FirSession
    private lateinit var file: FirFile

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        initFromFile(file)
        return transformDeclaration(file, data) as CompositeTransformResult<FirFile>
    }

    fun initFromFile(file: FirFile) {
        session = file.session
        this.file = file
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirStatement> {
        val transformedClass = resolveSupertypesOrExpansions(regularClass) as? FirRegularClass ?: regularClass

        // resolve supertypes for nested classes
        return transformDeclaration(transformedClass, data) as CompositeTransformResult<FirStatement>
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Nothing?): CompositeTransformResult<FirStatement> {
        return transformRegularClass(enumEntry, data)
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val result = resolveSupertypesOrExpansions(typeAlias)
        return result.compose()
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return anonymousInitializer.compose()
    }

    override fun transformConstructor(constructor: FirConstructor, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return constructor.compose()
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return simpleFunction.compose()
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return property.compose()
    }

    override fun <F : FirFunction<F>> transformFunction(function: FirFunction<F>, data: Nothing?): CompositeTransformResult<FirStatement> {
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

        override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Nothing?): CompositeTransformResult<FirStatement> {
            return transformRegularClass(enumEntry, data)
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirStatement> {
            val classId = regularClass.classId
            if (!isOuterClass(classId, requestedClassId)) return regularClass.compose()
            val transformedClass = withScopeCleanup {
                if (regularClass.areSupertypesComputed() || regularClass.areSupertypesComputing()) return@withScopeCleanup regularClass

                regularClass.addTypeParametersScope()

                val transformer = FirSpecificTypeResolverTransformer(towerScope, session)
                val resolvedTypesRefs = regularClass.superTypeRefs.map {
                    transformer.transformTypeRef(it, data).single
                }

                val resultingTypeRefs = resolveLoops(regularClass, classId, resolvedTypesRefs)
                regularClass.replaceSuperTypeRefs(resultingTypeRefs)
                regularClass
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

                val transformer = FirSpecificTypeResolverTransformer(towerScope, session)
                val resolvedTypesRef = transformer.transformTypeRef(typeAlias.expandedTypeRef, data).single
                val resultingTypeRef = resolveLoops(typeAlias, classId, listOf(resolvedTypesRef)).firstOrNull()

                typeAlias.replaceExpandedTypeRef(resultingTypeRef ?: resolvedTypesRef)
                resultingClass = typeAlias
                typeAlias
            }.compose()
        }

        private fun resolveLoops(
            firClassLikeDeclaration: FirClassLikeDeclaration<*>,
            classId: ClassId,
            resolvedTypesRefs: List<FirTypeRef>
        ): List<FirTypeRef> {
            firClassLikeDeclaration.replaceSupertypesComputationStatus(SupertypesComputationStatus.COMPUTING)

            val resultingTypeRefs = mutableListOf<FirTypeRef>()
            for (superTypeRef in resolvedTypesRefs) {
                val coneType = (superTypeRef as FirResolvedTypeRef).type
                if (coneType is ConeTypeParameterType) {
                    resultingTypeRefs.add(
                        FirErrorTypeRefImpl(superTypeRef.source, FirSimpleDiagnostic("Type parameter cannot be a super-type: ${coneType.render()}", DiagnosticKind.TypeParameterAsSupertype))
                    )
                    continue
                }
                val resolvedType = coneType as ConeClassLikeType
                val superTypeClassId = resolvedType.lookupTag.classId

                if (superTypeClassId.outerClasses().any { it.areSupertypesComputing() }) {
                    resultingTypeRefs.add(
                        FirErrorTypeRefImpl(superTypeRef.source, FirSimpleDiagnostic("Recursion detected: ${superTypeRef.render()}", DiagnosticKind.RecursionInSupertypes))
                    )

                    continue
                }

                val sessionForSupertype = session.firSymbolProvider.getSessionForClass(superTypeClassId) ?: continue

                val firClassForSupertype =
                    sessionForSupertype
                        .firSymbolProvider
                        .getClassLikeSymbolByFqName(superTypeClassId)
                        ?.fir

                // TODO: this if is a temporary hack for built-in types (because we can't load file for them)
                if (firClassForSupertype == null ||
                    (firClassForSupertype is FirClass &&
                            firClassForSupertype.superTypeRefs.any { it !is FirResolvedTypeRef })
                ) {
                    val provider = sessionForSupertype.firProvider
                    val firForSuperClassFile = provider.getFirClassifierContainerFile(superTypeClassId)

                    ResolveSuperTypesTask(
                        sessionForSupertype, superTypeClassId, firForSuperClassFile
                    ).transformFile(firForSuperClassFile, null)
                }

                resultingTypeRefs.add(superTypeRef)
            }

            firClassLikeDeclaration.replaceSupertypesComputationStatus(SupertypesComputationStatus.COMPUTED)
            return resultingTypeRefs
        }

        private fun ClassId.areSupertypesComputing(): Boolean {
            val fir = session.firSymbolProvider.getClassLikeSymbolByFqName(this)?.fir ?: return false
            return fir.areSupertypesComputing()
        }
    }
}

private fun isOuterClass(outerCandidate: ClassId, innerCandidate: ClassId) =
    innerCandidate.outerClasses().any { outerCandidate == it }

private fun ClassId.outerClasses() = generateSequence(this, ClassId::getOuterClassId)

private fun FirClassLikeDeclaration<*>.areSupertypesComputed() =
    supertypesComputationStatus == SupertypesComputationStatus.COMPUTED

private fun FirClassLikeDeclaration<*>.areSupertypesComputing() =
    supertypesComputationStatus == SupertypesComputationStatus.COMPUTING
