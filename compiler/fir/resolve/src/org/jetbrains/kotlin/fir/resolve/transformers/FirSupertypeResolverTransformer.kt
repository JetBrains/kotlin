/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.ClassId

class FirSupertypeResolverTransformer : FirAbstractTreeTransformer() {
    private lateinit var firSession: FirSession
    private val currentlyComputing: MutableSet<ClassId> = mutableSetOf()
    private val fullyComputed: MutableSet<ClassId> = mutableSetOf()

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        firSession = file.fileSession
        return super.transformFile(file, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val transformedClass = resolveSupertypesOrExpansions(regularClass) as? FirRegularClass ?: regularClass

        // resolve supertypes for nested classes
        return super.transformRegularClass(transformedClass, data)
    }


    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return resolveSupertypesOrExpansions(typeAlias).compose()
    }

    // This and transformProperty functions are required to forbid supertype resolving for local classes
    override fun transformDeclarationWithBody(
        declarationWithBody: FirDeclarationWithBody,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return declarationWithBody.compose()
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return property.compose()
    }

    private fun resolveSupertypesOrExpansions(classLikeDeclaration: FirClassLikeDeclaration): FirDeclaration {
        val classId = classLikeDeclaration.symbol.classId

        if (classId in fullyComputed) return classLikeDeclaration

        val firFile = firSession.getService(FirProvider::class).getFirClassifierContainerFile(classId)

        val visitor = ResolveSuperTypesTask(firSession, classId, firFile, currentlyComputing, fullyComputed)
        firFile.accept(visitor, null).single

        return visitor.resultingClass
    }

    private class ResolveSuperTypesTask(
        private val session: FirSession,
        private val requestedClassId: ClassId,
        file: FirFile,
        private val currentlyComputing: MutableSet<ClassId>,
        private val fullyComputed: MutableSet<ClassId>
    ) : FirAbstractTreeTransformerWithSuperTypes(reversedScopePriority = true) {

        lateinit var resultingClass: FirDeclaration

        init {
            towerScope.addImportingScopes(file, session)
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
            val classId = regularClass.classId
            if (!isOuterClass(classId, requestedClassId)) return regularClass.compose()
            val transformedClass = withScopeCleanup {
                if (classId in fullyComputed) return@withScopeCleanup regularClass

                regularClass.addTypeParametersScope()

                val transformer = FirSpecificTypeResolverTransformer(towerScope, FirPosition.SUPER_TYPE_OR_EXPANSION, session)
                val resolvedTypesRefs = regularClass.superTypeRefs.map { transformer.transformTypeRef(it, data).single }

                val resultingTypeRefs = resolveLoops(classId, resolvedTypesRefs)
                regularClass.replaceSupertypes(resultingTypeRefs)
            }

            if (classId == requestedClassId) {
                resultingClass = transformedClass
                return transformedClass.compose()
            }

            return resolveNestedClassesSupertypes(transformedClass, data)
        }

        override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
            val classId = typeAlias.symbol.classId
            // nested type aliases
            if (requestedClassId != classId || classId in fullyComputed) return typeAlias.compose()

            return withScopeCleanup {
                typeAlias.addTypeParametersScope()

                val transformer = FirSpecificTypeResolverTransformer(towerScope, FirPosition.SUPER_TYPE_OR_EXPANSION, session)
                val resolvedTypesRef = transformer.transformTypeRef(typeAlias.expandedTypeRef, data).single
                val resultingTypeRef = resolveLoops(classId, listOf(resolvedTypesRef)).firstOrNull()

                typeAlias.replaceExpandTypeRef(resultingTypeRef ?: resolvedTypesRef).also {
                    resultingClass = it
                }
            }.compose()
        }

        private fun resolveLoops(
            classId: ClassId,
            resolvedTypesRefs: List<FirTypeRef>
        ): List<FirTypeRef> {
            currentlyComputing.add(classId)

            val resultingTypeRefs = mutableListOf<FirTypeRef>()
            for (superTypeRef in resolvedTypesRefs) {
                val resolvedType = superTypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
                val superTypeClassId = resolvedType.lookupTag.classId

                if (superTypeClassId.outerClasses().any(currentlyComputing::contains)) {
                    resultingTypeRefs.add(
                        FirErrorTypeRefImpl(session, superTypeRef.psi, "Recursion detected: ${superTypeRef.render()}")
                    )

                    continue
                }

                val sessionForSupertype = session.getService(FirSymbolProvider::class).getSessionForClass(superTypeClassId) ?: continue
                val provider = sessionForSupertype.getService(FirProvider::class)

                val firClassForSupertype =
                    sessionForSupertype
                        .getService(FirSymbolProvider::class)
                        .getClassLikeSymbolByFqName(superTypeClassId)
                        ?.toFirClassLike() as? FirClass

                // TODO: this if is a temporary hack for built-in types (because we can't load file for them)
                if (firClassForSupertype == null || firClassForSupertype.superTypeRefs.any { it !is FirResolvedTypeRef }) {
                    val firForSuperClassFile = provider.getFirClassifierContainerFile(superTypeClassId)

                    ResolveSuperTypesTask(
                        sessionForSupertype, superTypeClassId, firForSuperClassFile,
                        currentlyComputing, fullyComputed
                    ).transformFile(firForSuperClassFile, null)
                }

                resultingTypeRefs.add(superTypeRef)
            }

            fullyComputed.add(classId)
            currentlyComputing.remove(classId)
            return resultingTypeRefs
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

                super.transformRegularClass(regularClass, data)
            }
        }
    }
}

private fun isOuterClass(outerCandidate: ClassId, innerCandidate: ClassId) =
    innerCandidate.outerClasses().any { outerCandidate == it }

private fun ClassId.outerClasses() = generateSequence(this, ClassId::getOuterClassId)
