/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.extensions.predicate.AbstractPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate

abstract class FirRegisteredPluginAnnotations(protected val session: FirSession) : FirSessionComponent {
    /**
     * Contains all annotations that can be targeted by lookup predicates from plugins
     */
    abstract val annotations: Set<AnnotationFqn>

    val hasRegisteredAnnotations: Boolean
        get() = annotations.isNotEmpty()

    @PluginServicesInitialization
    abstract fun initialize()
}

/**
 * Collecting annotations directly from registered plugins works the same way for all implementations of
 * [FirRegisteredPluginAnnotations], so this abstract base class was introduced.
 *
 * It also has some common code in it.
 */
abstract class AbstractFirRegisteredPluginAnnotations(session: FirSession) : FirRegisteredPluginAnnotations(session) {
    @PluginServicesInitialization
    final override fun initialize() {
        val registrar = object : FirDeclarationPredicateRegistrar() {
            val predicates = mutableListOf<AbstractPredicate<*>>()

            override fun register(vararg predicates: AbstractPredicate<*>) {
                this.predicates += predicates
            }

            override fun register(predicates: Collection<AbstractPredicate<*>>) {
                this.predicates += predicates
            }
        }

        for (extension in session.extensionService.getAllExtensions()) {
            with(extension) {
                registrar.registerPredicates()
            }
        }

        for (predicate in registrar.predicates) {
            saveAnnotationsFromPlugin(predicate.annotations)
        }
    }

    protected abstract fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>)
}

@NoMutableState
class FirRegisteredPluginAnnotationsImpl(session: FirSession) : AbstractFirRegisteredPluginAnnotations(session) {
    override val annotations: MutableSet<AnnotationFqn> = mutableSetOf()

    override fun saveAnnotationsFromPlugin(annotations: Collection<AnnotationFqn>) {
        this.annotations += annotations
    }
}

val FirSession.registeredPluginAnnotations: FirRegisteredPluginAnnotations by FirSession.sessionComponentAccessor()
