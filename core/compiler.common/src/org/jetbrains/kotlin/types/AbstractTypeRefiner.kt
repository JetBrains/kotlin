/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.KotlinTypeMarker

/**
 * This annotation marks part of internal compiler API related to type refinement.
 *
 * Marking such API explicitly has two objectives:
 * - prevent unconscious abuse of invasive API (like DelegatingSimpleType.replaceDelegate),
 *   which shouldn't be needed by anything outside of type refinement
 * - improve readability of classes by separating API
 *
 * If you're using related API outside of MPP context, it's a nice idea to consider
 * either finding some other API or removing @TypeRefinement (and thus "publishing"
 * API for broader use)
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class TypeRefinement


abstract class AbstractTypeRefiner {
    @TypeRefinement
    abstract fun refineType(type: KotlinTypeMarker): KotlinTypeMarker

    object Default : AbstractTypeRefiner() {
        @TypeRefinement
        override fun refineType(type: KotlinTypeMarker): KotlinTypeMarker {
            return type
        }
    }
}
