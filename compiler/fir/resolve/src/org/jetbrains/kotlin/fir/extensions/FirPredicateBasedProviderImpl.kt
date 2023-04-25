/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.predicate.AbstractPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.PredicateVisitor
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

@NoMutableState
class FirPredicateBasedProviderImpl(private val session: FirSession) : FirPredicateBasedProvider() {
    private val registeredPluginAnnotations = session.registeredPluginAnnotations
    private val cache = Cache()

    override fun getSymbolsByPredicate(predicate: LookupPredicate): List<FirBasedSymbol<*>> {
        val annotations = predicate.annotations
        if (annotations.isEmpty()) return emptyList()
        val declarations = annotations.flatMapTo(mutableSetOf()) {
            cache.declarationByAnnotation[it] + cache.declarationsUnderAnnotated[it]
        }
        return declarations.filter { matches(predicate, it) }.map { it.symbol }
    }

    override fun fileHasPluginAnnotations(file: FirFile): Boolean {
        return file in cache.filesWithPluginAnnotations
    }

    @FirExtensionApiInternals
    override fun registerAnnotatedDeclaration(declaration: FirDeclaration, owners: PersistentList<FirDeclaration>) {
        cache.ownersForDeclaration[declaration] = owners
        registerOwnersDeclarations(declaration, owners)

        if (declaration.annotations.isEmpty()) return
        val matchingAnnotations = declaration.annotations
            .mapNotNull { it.fqName(session) }
            .filter { it in registeredPluginAnnotations.annotations }
            .takeIf { it.isNotEmpty() }
            ?: return

        owners.lastOrNull()?.let { owner ->
            matchingAnnotations.forEach { cache.declarationsHasAnnotated.put(it, owner) }
            cache.annotationsOfHasAnnotated.putAll(owner, matchingAnnotations)
        }

        matchingAnnotations.forEach { cache.declarationByAnnotation.put(it, declaration) }
        cache.annotationsOfDeclaration.putAll(declaration, matchingAnnotations)

        val file = owners.first() as FirFile
        cache.filesWithPluginAnnotations += file
    }

    override fun getOwnersOfDeclaration(declaration: FirDeclaration): List<FirBasedSymbol<*>>? {
        return cache.ownersForDeclaration[declaration]?.map { it.symbol }
    }

    private fun registerOwnersDeclarations(declaration: FirDeclaration, owners: PersistentList<FirDeclaration>) {
        val lastOwner = owners.lastOrNull() ?: return
        val annotationsFromLastOwner = cache.annotationsOfDeclaration[lastOwner]
        val annotationsFromPreviousOwners = cache.annotationsOfUnderAnnotated[lastOwner]

        annotationsFromLastOwner.forEach { cache.declarationsParentAnnotated.put(it, declaration) }
        cache.annotationsOfParentAnnotated.putAll(declaration, annotationsFromLastOwner)

        val allParentDeclarations = annotationsFromLastOwner + annotationsFromPreviousOwners
        allParentDeclarations.forEach { cache.declarationsUnderAnnotated.put(it, declaration) }
        cache.annotationsOfUnderAnnotated.putAll(declaration, allParentDeclarations)
    }

    // ---------------------------------- Matching ----------------------------------

    override fun matches(predicate: AbstractPredicate<*>, declaration: FirDeclaration): Boolean {
        /*
         * If declaration came from the other source session we should delegate to provider from
         *   that session, because it stores all caches about its own declarations
         */
        val declarationSession = declaration.moduleData.session
        if (declarationSession.kind == FirSession.Kind.Source && declarationSession !== session) {
            return declarationSession.predicateBasedProvider.matches(predicate, declaration)
        }
        return when (predicate) {
            is DeclarationPredicate -> predicate.accept(declarationPredicateMatcher, declaration)
            is LookupPredicate -> predicate.accept(lookupPredicateMatcher, declaration)
        }
    }

    private val declarationPredicateMatcher = Matcher<DeclarationPredicate>()
    private val lookupPredicateMatcher = Matcher<LookupPredicate>()

    private inner class Matcher<P : AbstractPredicate<P>> : PredicateVisitor<P, Boolean, FirDeclaration>() {
        override fun visitPredicate(predicate: AbstractPredicate<P>, data: FirDeclaration): Boolean {
            throw IllegalStateException("Should not be there")
        }

        override fun visitAnd(predicate: AbstractPredicate.And<P>, data: FirDeclaration): Boolean {
            return predicate.a.accept(this, data) && predicate.b.accept(this, data)
        }

        override fun visitOr(predicate: AbstractPredicate.Or<P>, data: FirDeclaration): Boolean {
            return predicate.a.accept(this, data) || predicate.b.accept(this, data)
        }

        // ------------------------------------ Annotated ------------------------------------

        override fun visitAnnotatedWith(predicate: AbstractPredicate.AnnotatedWith<P>, data: FirDeclaration): Boolean {
            return matchWith(data, predicate.annotations)
        }

        override fun visitAncestorAnnotatedWith(
            predicate: AbstractPredicate.AncestorAnnotatedWith<P>,
            data: FirDeclaration
        ): Boolean {
            return matchUnder(data, predicate.annotations)
        }

        override fun visitParentAnnotatedWith(
            predicate: AbstractPredicate.ParentAnnotatedWith<P>,
            data: FirDeclaration
        ): Boolean {
            return matchParentWith(data, predicate.annotations)
        }

        override fun visitHasAnnotatedWith(predicate: AbstractPredicate.HasAnnotatedWith<P>, data: FirDeclaration): Boolean {
            return matchHasAnnotatedWith(data, predicate.annotations)
        }

        // ------------------------------------ Meta-annotated ------------------------------------

        override fun visitMetaAnnotatedWith(predicate: AbstractPredicate.MetaAnnotatedWith<P>, data: FirDeclaration): Boolean {
            return data.annotations.any { annotation ->
                annotation.markedWithMetaAnnotation(session, data, predicate.metaAnnotations, predicate.includeItself)
            }
        }

        // ------------------------------------ Utilities ------------------------------------

        private fun matchWith(declaration: FirDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return when (declaration.origin) {
                FirDeclarationOrigin.Library, is FirDeclarationOrigin.Java -> matchNonIndexedDeclaration(declaration, annotations)
                else -> when (declaration is FirClass && declaration.isLocal) {
                    true -> matchNonIndexedDeclaration(declaration, annotations)
                    false -> cache.annotationsOfDeclaration[declaration].any { it in annotations }
                }
            }
        }

        private fun matchNonIndexedDeclaration(declaration: FirDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return declaration.annotations.any { it.fqName(session) in annotations }
        }

        private fun matchUnder(declaration: FirDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return cache.annotationsOfUnderAnnotated[declaration].any { it in annotations }
        }

        private fun matchParentWith(declaration: FirDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return cache.annotationsOfParentAnnotated[declaration].any { it in annotations }
        }

        private fun matchHasAnnotatedWith(declaration: FirDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return cache.annotationsOfHasAnnotated[declaration].any { it in annotations }
        }
    }

    // ---------------------------------- Cache ----------------------------------

    private class Cache {
        val declarationByAnnotation: Multimap<AnnotationFqn, FirDeclaration> = LinkedHashMultimap.create()
        val annotationsOfDeclaration: LinkedHashMultimap<FirDeclaration, AnnotationFqn> = LinkedHashMultimap.create()

        val declarationsUnderAnnotated: Multimap<AnnotationFqn, FirDeclaration> = LinkedHashMultimap.create()
        val annotationsOfUnderAnnotated: LinkedHashMultimap<FirDeclaration, AnnotationFqn> = LinkedHashMultimap.create()

        val declarationsParentAnnotated: Multimap<AnnotationFqn, FirDeclaration> = LinkedHashMultimap.create()
        val annotationsOfParentAnnotated: Multimap<FirDeclaration, AnnotationFqn> = LinkedHashMultimap.create()

        val declarationsHasAnnotated: Multimap<AnnotationFqn, FirDeclaration> = LinkedHashMultimap.create()
        val annotationsOfHasAnnotated: Multimap<FirDeclaration, AnnotationFqn> = LinkedHashMultimap.create()

        val ownersForDeclaration: MutableMap<FirDeclaration, PersistentList<FirDeclaration>> = mutableMapOf()

        val filesWithPluginAnnotations: MutableSet<FirFile> = mutableSetOf()
    }
}

fun FirAnnotation.markedWithMetaAnnotation(
    session: FirSession,
    containingDeclaration: FirDeclaration,
    metaAnnotations: Set<AnnotationFqn>,
    includeItself: Boolean
): Boolean {
    containingDeclaration.symbol.lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
    return annotationTypeRef.coneTypeSafe<ConeKotlinType>()
        ?.toRegularClassSymbol(session)
        .markedWithMetaAnnotationImpl(session, metaAnnotations, includeItself, mutableSetOf())
}

fun FirRegularClassSymbol?.markedWithMetaAnnotationImpl(
    session: FirSession,
    metaAnnotations: Set<AnnotationFqn>,
    includeItself: Boolean,
    visited: MutableSet<FirRegularClassSymbol>
): Boolean {
    if (this == null) return false
    if (!visited.add(this)) return false
    if (this.classId.asSingleFqName() in metaAnnotations) return includeItself
    return this.resolvedCompilerAnnotationsWithClassIds
        .mapNotNull { it.annotationTypeRef.coneTypeSafe<ConeKotlinType>()?.toRegularClassSymbol(session) }
        .any { it.markedWithMetaAnnotationImpl(session, metaAnnotations, includeItself = true, visited) }
}
