/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.AbstractPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.PredicateVisitor
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*

/**
 * PSI index based implementation of [FirPredicateBasedProvider].
 */
internal class LLFirIdePredicateBasedProvider(
    private val session: LLFirSession,
    private val annotationsResolver: KotlinAnnotationsResolver,
) : FirPredicateBasedProvider() {
    private val projectStructureProvider by lazy { KotlinProjectStructureProvider.getInstance(session.project) }

    private val registeredPluginAnnotations: FirRegisteredPluginAnnotations
        get() = session.registeredPluginAnnotations

    private val declarationOwnersCache: FirCache<FirFile, FirOwnersStorage, Nothing?> =
        session.firCachesFactory.createCache { firFile -> FirOwnersStorage.collectOwners(firFile) }

    override fun getSymbolsByPredicate(predicate: LookupPredicate): List<FirBasedSymbol<*>> {
        val annotations = predicate.annotations
        val annotatedDeclarations = annotations
            .asSequence()
            .flatMap { annotationsResolver.declarationsByAnnotation(ClassId.topLevel(it)) }
            .toSet()

        return annotatedDeclarations
            .asSequence()
            .mapNotNull { it.findFirDeclaration() }
            .filter { matches(predicate, it) }
            .map { it.symbol }
            .toList()
    }

    private fun KtElement.findFirDeclaration(): FirDeclaration? {
        if (this !is KtDeclaration) return null

        if (this !is KtClassLikeDeclaration &&
            this !is KtNamedFunction &&
            this !is KtConstructor<*> &&
            this !is KtProperty
        ) return null

        val moduleForFile = projectStructureProvider.getModule(this, session.ktModule)
        val resolutionFacadeForFile = moduleForFile.getResolutionFacade(project)
        return this.resolveToFirSymbol(resolutionFacadeForFile).fir
    }

    override fun getOwnersOfDeclaration(declaration: FirDeclaration): List<FirBasedSymbol<*>>? {
        val firFile = declaration.getContainingFile() ?: return null
        val declarationOwners = declarationOwnersCache.getValue(firFile)

        return declarationOwners.getOwners(declaration)
    }

    override fun fileHasPluginAnnotations(file: FirFile): Boolean {
        val targetKtFile = file.psi as? KtFile ?: return false
        val pluginAnnotations = registeredPluginAnnotations.annotations

        return pluginAnnotations.any {
            val annotationId = ClassId.topLevel(it)
            val markedDeclarations = annotationsResolver.declarationsByAnnotation(annotationId)

            targetKtFile in markedDeclarations
        }
    }

    override fun matches(predicate: AbstractPredicate<*>, declaration: FirDeclaration): Boolean {
        return when (predicate) {
            is DeclarationPredicate -> predicate.accept(declarationPredicateMatcher, declaration)
            is LookupPredicate -> predicate.accept(lookupPredicateMatcher, declaration)
        }
    }

    private val declarationPredicateMatcher = Matcher<DeclarationPredicate>()
    private val lookupPredicateMatcher = Matcher<LookupPredicate>()

    private inner class Matcher<P : AbstractPredicate<P>> : PredicateVisitor<P, Boolean, FirDeclaration>() {
        override fun visitPredicate(predicate: AbstractPredicate<P>, data: FirDeclaration): Boolean {
            error(
                "When overrides for all possible DeclarationPredicate subtypes are implemented, " +
                        "this method should never be called, but it was called with $predicate"
            )
        }

        override fun visitAnd(predicate: AbstractPredicate.And<P>, data: FirDeclaration): Boolean {
            return predicate.a.accept(this, data) && predicate.b.accept(this, data)
        }

        override fun visitOr(predicate: AbstractPredicate.Or<P>, data: FirDeclaration): Boolean {
            return predicate.a.accept(this, data) || predicate.b.accept(this, data)
        }

        override fun visitAnnotatedWith(predicate: AbstractPredicate.AnnotatedWith<P>, data: FirDeclaration): Boolean {
            return annotationsOnDeclaration(data).any { it in predicate.annotations }
        }

        override fun visitAncestorAnnotatedWith(predicate: AbstractPredicate.AncestorAnnotatedWith<P>, data: FirDeclaration): Boolean {
            return annotationsOnOuterDeclarations(data).any { it in predicate.annotations }
        }

        override fun visitMetaAnnotatedWith(predicate: AbstractPredicate.MetaAnnotatedWith<P>, data: FirDeclaration): Boolean {
            return data.annotations.any { annotation ->
                annotation.markedWithMetaAnnotation(session, data, predicate.metaAnnotations, predicate.includeItself)
            }
        }

        override fun visitParentAnnotatedWith(predicate: AbstractPredicate.ParentAnnotatedWith<P>, data: FirDeclaration): Boolean {
            val parent = data.directParentDeclaration ?: return false
            val parentPredicate = DeclarationPredicate.AnnotatedWith(predicate.annotations)

            return parentPredicate.accept(declarationPredicateMatcher, parent)
        }

        override fun visitHasAnnotatedWith(predicate: AbstractPredicate.HasAnnotatedWith<P>, data: FirDeclaration): Boolean {
            val childPredicate = DeclarationPredicate.AnnotatedWith(predicate.annotations)

            return data.anyDirectChildDeclarationMatches(childPredicate)
        }

        private val FirDeclaration.directParentDeclaration: FirDeclaration?
            get() = getOwnersOfDeclaration(this)?.lastOrNull()?.fir
    }

    private fun FirDeclaration.anyDirectChildDeclarationMatches(childPredicate: DeclarationPredicate): Boolean {
        var result = false

        this.forEachDirectChildDeclaration {
            result = result || childPredicate.accept(declarationPredicateMatcher, it)
        }

        return result
    }

    private fun annotationsOnDeclaration(declaration: FirDeclaration): Set<AnnotationFqn> {
        if (declaration.annotations.isEmpty()) return emptySet()

        val firResolvedAnnotations = declaration.annotations
            .asSequence()
            .mapNotNull { it.annotationTypeRef as? FirResolvedTypeRef }
            .mapNotNull { it.coneType.classId }
            .map { it.asSingleFqName() }
            .toSet()

        if (firResolvedAnnotations.isNotEmpty()) return firResolvedAnnotations

        val psiDeclaration = declaration.psi as? KtAnnotated ?: return emptySet()
        val psiAnnotations = annotationsResolver.annotationsOnDeclaration(psiDeclaration)

        return psiAnnotations.map { it.asSingleFqName() }.toSet()
    }

    private fun annotationsOnOuterDeclarations(declaration: FirDeclaration): Set<AnnotationFqn> {
        return getOwnersOfDeclaration(declaration)?.flatMap { annotationsOnDeclaration(it.fir) }.orEmpty().toSet()
    }
}

private class FirOwnersStorage(private val declarationToOwner: Map<FirDeclaration, List<FirBasedSymbol<*>>>) {
    fun getOwners(declaration: FirDeclaration): List<FirBasedSymbol<*>>? = declarationToOwner[declaration]

    companion object {
        fun collectOwners(file: FirFile): FirOwnersStorage {
            val declarationToOwners = hashMapOf<FirDeclaration, List<FirBasedSymbol<*>>>()
            val psiToFir = hashMapOf<KtElement, FirDeclaration>()

            file.forEachElementWithContainers { element, owners ->
                if (element !is FirDeclaration) return@forEachElementWithContainers

                declarationToOwners[element] = owners

                val psiDeclaration = element.psi
                if (psiDeclaration is KtElement) {
                    // FIXME we actually have a problem with KtFakeSourceElement sources
                    psiToFir.putIfAbsent(psiDeclaration, element)
                }
            }

            return FirOwnersStorage(declarationToOwners)
        }
    }
}

/**
 * Walks over every [FirElement] in [this] file and invokes [saveDeclaration] on it, passing each element and the list of its containing
 * declarations (like file, classes, functions/properties and so on).
 */
private inline fun FirFile.forEachElementWithContainers(
    crossinline saveDeclaration: (element: FirElement, owners: List<FirBasedSymbol<*>>) -> Unit
) {
    val declarationsCollector = object : FirVisitor<Unit, PersistentList<FirBasedSymbol<*>>>() {
        override fun visitElement(element: FirElement, data: PersistentList<FirBasedSymbol<*>>) {
            if (element is FirDeclaration) {
                saveDeclaration(element, data)
            }

            element.acceptChildren(
                visitor = this,
                data = if (element is FirDeclaration) data.add(element.symbol) else data
            )
        }
    }

    accept(declarationsCollector, persistentListOf())
}

/**
 * Calls [action] on every direct child declaration of [this] declaration.
 */
private inline fun FirDeclaration.forEachDirectChildDeclaration(crossinline action: (child: FirDeclaration) -> Unit) {
    this.acceptChildren(object : FirDefaultVisitorVoid() {
        override fun visitElement(element: FirElement) {
            // we must visit only direct children
        }

        override fun visitFile(file: FirFile) {
            action(file)
        }

        override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration) {
            action(callableDeclaration)
        }

        override fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration) {
            action(classLikeDeclaration)
        }
    })
}
