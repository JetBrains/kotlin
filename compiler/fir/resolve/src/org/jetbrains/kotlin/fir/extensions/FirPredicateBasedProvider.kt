/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

abstract class FirPredicateBasedProvider : FirSessionComponent {
    companion object {
        fun create(session: FirSession): FirPredicateBasedProvider {
            return FirPredicateBasedProviderImpl(session)
        }
    }

    abstract fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirAnnotatedDeclaration>

    abstract fun registerAnnotatedDeclaration(declaration: FirAnnotatedDeclaration, owners: PersistentList<FirAnnotatedDeclaration>)
}

private class FirPredicateBasedProviderImpl(private val session: FirSession) : FirPredicateBasedProvider() {
    private val registeredPluginAnnotations = session.registeredPluginAnnotations
    private val cache = Cache()

    override fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirAnnotatedDeclaration> {
        val annotations = registeredPluginAnnotations.getAnnotationsForPredicate(predicate)
        if (annotations.isEmpty()) return emptyList()
        return annotations.flatMap { cache.declarationByAnnotation[it] + cache.declarationsUnderAnnotated[it] }.filter {
            predicate.match(it, cache.ownersForDeclaration.getValue(it))
        }
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

    private class Cache {
        val declarationByAnnotation: Multimap<AnnotationFqn, FirAnnotatedDeclaration> = ArrayListMultimap.create()
        val annotationsOfDeclaration: Multimap<FirAnnotatedDeclaration, AnnotationFqn> = ArrayListMultimap.create()

        val declarationsUnderAnnotated: Multimap<AnnotationFqn, FirAnnotatedDeclaration> = ArrayListMultimap.create()
        val parentAnnotationsOfDeclaration: Multimap<FirAnnotatedDeclaration, AnnotationFqn> = ArrayListMultimap.create()

        val ownersForDeclaration: MutableMap<FirAnnotatedDeclaration, PersistentList<FirAnnotatedDeclaration>> = mutableMapOf()
    }
}

val FirSession.predicateBasedProvider: FirPredicateBasedProvider by FirSession.sessionComponentAccessor()