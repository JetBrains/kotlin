/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.predicate.*
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

abstract class FirPredicateBasedProvider : FirSessionComponent {
    companion object {
        fun create(session: FirSession): FirPredicateBasedProvider {
            return FirPredicateBasedProviderImpl(session)
        }
    }

    abstract fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirAnnotatedDeclaration>
    abstract fun getSymbolsWithOwnersByPredicate(
        predicate: DeclarationPredicate
    ): List<Pair<FirAnnotatedDeclaration, List<FirAnnotatedDeclaration>>>

    abstract fun registerAnnotatedDeclaration(declaration: FirAnnotatedDeclaration, owners: PersistentList<FirAnnotatedDeclaration>)

    abstract fun matches(predicate: DeclarationPredicate, declaration: FirAnnotatedDeclaration): Boolean
}

private class FirPredicateBasedProviderImpl(private val session: FirSession) : FirPredicateBasedProvider() {
    private val registeredPluginAnnotations = session.registeredPluginAnnotations
    private val cache = Cache()

    override fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirAnnotatedDeclaration> {
        val annotations = registeredPluginAnnotations.getAnnotationsForPredicate(predicate)
        if (annotations.isEmpty()) return emptyList()
        return annotations.flatMap { cache.declarationByAnnotation[it] + cache.declarationsUnderAnnotated[it] }.filter {
            matches(predicate, it)
        }
    }

    override fun getSymbolsWithOwnersByPredicate(predicate: DeclarationPredicate): List<Pair<FirAnnotatedDeclaration, List<FirAnnotatedDeclaration>>> {
        return getSymbolsByPredicate(predicate).map { it to cache.ownersForDeclaration.getValue(it) }
    }

    override fun registerAnnotatedDeclaration(declaration: FirAnnotatedDeclaration, owners: PersistentList<FirAnnotatedDeclaration>) {
        cache.ownersForDeclaration[declaration] = owners
        registerOwnersDeclarations(declaration, owners)

        if (declaration.annotations.isEmpty()) return
        val matchingAnnotations = declaration.annotations.mapNotNull { it.fqName(session) }
            .filter { it in registeredPluginAnnotations.annotations }
        if (matchingAnnotations.isEmpty()) return
        matchingAnnotations.forEach { cache.declarationByAnnotation.put(it, declaration) }
        cache.annotationsOfDeclaration.putAll(declaration, matchingAnnotations)
    }

    private fun registerOwnersDeclarations(declaration: FirAnnotatedDeclaration, owners: PersistentList<FirAnnotatedDeclaration>) {
        val lastOwner = owners.lastOrNull() ?: return
        val annotationsFromLastOwner = cache.annotationsOfDeclaration[lastOwner]
        val annotationsFromPreviousOwners = cache.parentAnnotationsOfDeclaration[lastOwner]

        val allParentDeclarations = annotationsFromLastOwner + annotationsFromPreviousOwners
        allParentDeclarations.forEach { cache.declarationsUnderAnnotated.put(it, declaration) }
        cache.parentAnnotationsOfDeclaration.putAll(declaration, allParentDeclarations)
    }

    // ---------------------------------- Matching ----------------------------------

    override fun matches(predicate: DeclarationPredicate, declaration: FirAnnotatedDeclaration): Boolean {
        return predicate.accept(matcher, declaration)
    }

    private val matcher = Matcher()

    private inner class Matcher : DeclarationPredicateVisitor<Boolean, FirAnnotatedDeclaration>() {
        override fun visitPredicate(predicate: DeclarationPredicate, data: FirAnnotatedDeclaration): Boolean {
            throw IllegalStateException("Should not be there")
        }

        override fun visitAny(predicate: DeclarationPredicate.Any, data: FirAnnotatedDeclaration): Boolean {
            return true
        }

        override fun visitAnd(predicate: DeclarationPredicate.And, data: FirAnnotatedDeclaration): Boolean {
            return predicate.a.accept(this, data) && predicate.b.accept(this, data)
        }

        override fun visitOr(predicate: DeclarationPredicate.Or, data: FirAnnotatedDeclaration): Boolean {
            return predicate.a.accept(this, data) || predicate.b.accept(this, data)
        }

        override fun visitAnnotatedWith(predicate: AnnotatedWith, data: FirAnnotatedDeclaration): Boolean {
            return matchWith(data, predicate.annotations)
        }

        override fun visitUnderAnnotatedWith(predicate: UnderAnnotatedWith, data: FirAnnotatedDeclaration): Boolean {
            return matchUnder(data, predicate.annotations)
        }

        override fun visitAnnotatedWithMeta(predicate: AnnotatedWithMeta, data: FirAnnotatedDeclaration): Boolean {
            return matchWith(data, predicate.userDefinedAnnotations)
        }

        override fun visitUnderMetaAnnotated(predicate: UnderMetaAnnotated, data: FirAnnotatedDeclaration): Boolean {
            return matchUnder(data, predicate.userDefinedAnnotations)
        }

        private val MetaAnnotated.userDefinedAnnotations: Set<AnnotationFqn>
            get() = metaAnnotations.flatMapTo(mutableSetOf()) { registeredPluginAnnotations.getAnnotationsWithMetaAnnotation(it) }

        private fun matchWith(declaration: FirAnnotatedDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return cache.annotationsOfDeclaration[declaration].any { it in annotations }
        }

        private fun matchUnder(declaration: FirAnnotatedDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return cache.parentAnnotationsOfDeclaration[declaration].any { it in annotations }
        }
    }

    // ---------------------------------- Cache ----------------------------------

    private class Cache {
        val declarationByAnnotation: Multimap<AnnotationFqn, FirAnnotatedDeclaration> = ArrayListMultimap.create()
        val annotationsOfDeclaration: LinkedHashMultimap<FirAnnotatedDeclaration, AnnotationFqn> = LinkedHashMultimap.create()

        val declarationsUnderAnnotated: Multimap<AnnotationFqn, FirAnnotatedDeclaration> = ArrayListMultimap.create()
        val parentAnnotationsOfDeclaration: LinkedHashMultimap<FirAnnotatedDeclaration, AnnotationFqn> = LinkedHashMultimap.create()

        val ownersForDeclaration: MutableMap<FirAnnotatedDeclaration, PersistentList<FirAnnotatedDeclaration>> = mutableMapOf()
    }
}

val FirSession.predicateBasedProvider: FirPredicateBasedProvider by FirSession.sessionComponentAccessor()