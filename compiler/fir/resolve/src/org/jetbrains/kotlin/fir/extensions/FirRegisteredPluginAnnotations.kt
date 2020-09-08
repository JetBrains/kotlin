/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

abstract class FirRegisteredPluginAnnotations(val session: FirSession) : FirSessionComponent {
    companion object {
        fun create(session: FirSession): FirRegisteredPluginAnnotations {
            return FirRegisteredPluginAnnotationsImpl(session)
        }
    }

    abstract val annotations: Set<AnnotationFqn>
    abstract val metaAnnotations: Set<AnnotationFqn>
    abstract fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn>

    abstract fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<FirRegularClass>)

    abstract fun getAnnotationsForPredicate(predicate: DeclarationPredicate): Set<AnnotationFqn>

    @PluginServicesInitialization
    abstract fun initialize()
}

@NoMutableState
private class FirRegisteredPluginAnnotationsImpl(session: FirSession) : FirRegisteredPluginAnnotations(session) {
    override val annotations: MutableSet<AnnotationFqn> = mutableSetOf()
    override val metaAnnotations: MutableSet<AnnotationFqn> = mutableSetOf()

    // MetaAnnotation -> Annotations
    private val userDefinedAnnotations: Multimap<AnnotationFqn, AnnotationFqn> = LinkedHashMultimap.create()

    private val annotationsForPredicateCache: MutableMap<DeclarationPredicate, Set<AnnotationFqn>> = mutableMapOf()

    override fun getAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Collection<AnnotationFqn> {
        return userDefinedAnnotations[metaAnnotation]
    }

    override fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotationClasses: Collection<FirRegularClass>) {
        require(annotationClasses.all { it.classKind == ClassKind.ANNOTATION_CLASS })
        val annotations = annotationClasses.map { it.symbol.classId.asSingleFqName() }
        this.annotations += annotations
        userDefinedAnnotations.putAll(metaAnnotation, annotations)
    }

    override fun getAnnotationsForPredicate(predicate: DeclarationPredicate): Set<AnnotationFqn> {
        return annotationsForPredicateCache.computeIfAbsent(predicate, ::collectAnnotations)
    }

    private fun collectAnnotations(predicate: DeclarationPredicate): Set<AnnotationFqn> {
        if (predicate.metaAnnotations.isEmpty()) return predicate.annotations
        val result = predicate.metaAnnotations.flatMapTo(mutableSetOf()) { getAnnotationsWithMetaAnnotation(it) }
        if (result.isEmpty()) return predicate.annotations
        result += predicate.annotations
        return result
    }

    @PluginServicesInitialization
    override fun initialize() {
        for (extension in session.extensionService.getAllExtensions()) {
            if (extension !is FirPredicateBasedExtension) continue
            val predicate = extension.predicate
            annotations += predicate.annotations
            metaAnnotations += predicate.metaAnnotations
        }
    }
}

val FirSession.registeredPluginAnnotations: FirRegisteredPluginAnnotations by FirSession.sessionComponentAccessor()
