/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.providers.KotlinAnnotationsResolver
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.extensions.AbstractFirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass

internal class LLFirIdeRegisteredPluginAnnotations(
    session: FirSession,
    private val annotationsResolver: KotlinAnnotationsResolver
) : AbstractFirRegisteredPluginAnnotations(session) {

    private val annotationsFromPlugins: MutableSet<AnnotationFqn> = mutableSetOf()

    override val annotations: Set<AnnotationFqn>
        get() = allAnnotationsCache.getValue()

    private val allAnnotationsCache: FirLazyValue<Set<AnnotationFqn>, Nothing?> = session.firCachesFactory.createLazyValue {
        // at this point, annotationsFromPlugins should be collected
        annotationsFromPlugins
    }

    // MetaAnnotation -> Annotations
    private val annotationsWithMetaAnnotationCache: FirCache<AnnotationFqn, Set<AnnotationFqn>, Nothing?> =
        session.firCachesFactory.createCache { metaAnnotation -> collectAnnotationsWithMetaAnnotation(metaAnnotation) }

    private fun collectAnnotationsWithMetaAnnotation(metaAnnotation: AnnotationFqn): Set<FqName> {
        val annotatedDeclarations = annotationsResolver.declarationsByAnnotation(ClassId.topLevel(metaAnnotation))

        return annotatedDeclarations
            .asSequence()
            .filterIsInstance<KtClass>()
            .filter { it.isAnnotation() && it.isTopLevel() }
            .mapNotNull { it.fqName }
            .toSet()
    }

    override fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>) {
        annotationsFromPlugins += annotations
    }
}
