/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.predicate.*
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

abstract class FirPredicateBasedProvider : FirSessionComponent {
    abstract fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirBasedSymbol<*>>
    abstract fun getOwnersOfDeclaration(declaration: FirDeclaration): List<FirBasedSymbol<*>>?

    /**
     * @return `true` iff file has a top-level annotation from the [FirRegisteredPluginAnnotations.annotations] list.
     * @see FirRegisteredPluginAnnotations.annotations
     */
    abstract fun fileHasPluginAnnotations(file: FirFile): Boolean
    abstract fun matches(predicate: DeclarationPredicate, declaration: FirDeclaration): Boolean

    open fun registerAnnotatedDeclaration(declaration: FirDeclaration, owners: PersistentList<FirDeclaration>) {
    }

    fun matches(predicates: List<DeclarationPredicate>, declaration: FirDeclaration): Boolean {
        return predicates.any { matches(it, declaration) }
    }
}

@NoMutableState
class FirPredicateBasedProviderImpl(private val session: FirSession) : FirPredicateBasedProvider() {
    private val registeredPluginAnnotations = session.registeredPluginAnnotations
    private val cache = Cache()

    override fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirBasedSymbol<*>> {
        val annotations = registeredPluginAnnotations.getAnnotationsForPredicate(predicate)
        if (annotations.isEmpty()) return emptyList()
        val declarations = annotations.flatMapTo(mutableSetOf()) {
            cache.declarationByAnnotation[it] + cache.declarationsUnderAnnotated[it]
        }
        return declarations.filter { matches(predicate, it) }.map { it.symbol }
    }

    override fun fileHasPluginAnnotations(file: FirFile): Boolean {
        return file in cache.filesWithPluginAnnotations
    }

    override fun registerAnnotatedDeclaration(declaration: FirDeclaration, owners: PersistentList<FirDeclaration>) {
        cache.ownersForDeclaration[declaration] = owners
        registerOwnersDeclarations(declaration, owners)

        if (declaration.annotations.isEmpty()) return
        val matchingAnnotations = declaration.annotations.mapNotNull { it.fqName(session) }
            .filter { it in registeredPluginAnnotations.annotations }
        if (matchingAnnotations.isEmpty()) return
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
        val annotationsFromPreviousOwners = cache.parentAnnotationsOfDeclaration[lastOwner]

        val allParentDeclarations = annotationsFromLastOwner + annotationsFromPreviousOwners
        allParentDeclarations.forEach { cache.declarationsUnderAnnotated.put(it, declaration) }
        cache.parentAnnotationsOfDeclaration.putAll(declaration, allParentDeclarations)
    }

    // ---------------------------------- Matching ----------------------------------

    override fun matches(predicate: DeclarationPredicate, declaration: FirDeclaration): Boolean {
        return predicate.accept(matcher, declaration)
    }

    private val matcher = Matcher()

    private inner class Matcher : DeclarationPredicateVisitor<Boolean, FirDeclaration>() {
        override fun visitPredicate(predicate: DeclarationPredicate, data: FirDeclaration): Boolean {
            throw IllegalStateException("Should not be there")
        }

        override fun visitAny(predicate: DeclarationPredicate.Any, data: FirDeclaration): Boolean {
            return true
        }

        override fun visitAnd(predicate: DeclarationPredicate.And, data: FirDeclaration): Boolean {
            return predicate.a.accept(this, data) && predicate.b.accept(this, data)
        }

        override fun visitOr(predicate: DeclarationPredicate.Or, data: FirDeclaration): Boolean {
            return predicate.a.accept(this, data) || predicate.b.accept(this, data)
        }

        override fun visitAnnotatedWith(predicate: AnnotatedWith, data: FirDeclaration): Boolean {
            return matchWith(data, predicate.annotations)
        }

        override fun visitUnderAnnotatedWith(predicate: UnderAnnotatedWith, data: FirDeclaration): Boolean {
            return matchUnder(data, predicate.annotations)
        }

        override fun visitAnnotatedWithMeta(predicate: AnnotatedWithMeta, data: FirDeclaration): Boolean {
            return matchWith(data, predicate.userDefinedAnnotations)
        }

        override fun visitUnderMetaAnnotated(predicate: UnderMetaAnnotated, data: FirDeclaration): Boolean {
            return matchUnder(data, predicate.userDefinedAnnotations)
        }

        private val MetaAnnotated.userDefinedAnnotations: Set<AnnotationFqn>
            get() = metaAnnotations.flatMapTo(mutableSetOf()) { registeredPluginAnnotations.getAnnotationsWithMetaAnnotation(it) }

        private fun matchWith(declaration: FirDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return cache.annotationsOfDeclaration[declaration].any { it in annotations }
        }

        private fun matchUnder(declaration: FirDeclaration, annotations: Set<AnnotationFqn>): Boolean {
            return cache.parentAnnotationsOfDeclaration[declaration].any { it in annotations }
        }
    }

    // ---------------------------------- Cache ----------------------------------

    private class Cache {
        val declarationByAnnotation: Multimap<AnnotationFqn, FirDeclaration> = LinkedHashMultimap.create()
        val annotationsOfDeclaration: LinkedHashMultimap<FirDeclaration, AnnotationFqn> = LinkedHashMultimap.create()

        val declarationsUnderAnnotated: Multimap<AnnotationFqn, FirDeclaration> = LinkedHashMultimap.create()
        val parentAnnotationsOfDeclaration: LinkedHashMultimap<FirDeclaration, AnnotationFqn> = LinkedHashMultimap.create()

        val ownersForDeclaration: MutableMap<FirDeclaration, PersistentList<FirDeclaration>> = mutableMapOf()

        val filesWithPluginAnnotations: MutableSet<FirFile> = mutableSetOf()
    }
}

val FirSession.predicateBasedProvider: FirPredicateBasedProvider by FirSession.sessionComponentAccessor()
