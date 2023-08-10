/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * @see org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox.Companion
 */
abstract class FirAnnotationsPlatformSpecificSupportComponent : FirSessionComponent {
    abstract val requiredAnnotationsWithArguments: Set<ClassId>
    abstract val requiredAnnotations: Set<ClassId>
    abstract val volatileAnnotations: Set<ClassId>

    val requiredAnnotationsShortClassNames: Set<Name> by lazy {
        requiredAnnotations.mapTo(mutableSetOf()) { it.shortClassName }
    }

    /**
     * Maps deprecation annotation ClassIds to the flag
     * which is true iff the corresponding annotation must
     * be propagated to overrides.
     */
    abstract val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean>

    private val deprecationAnnotations: Set<ClassId> by lazy {
        deprecationAnnotationsWithOverridesPropagation.keys
    }

    val deprecationAnnotationsSimpleNames: Set<String> by lazy {
        deprecationAnnotations.mapTo(mutableSetOf()) { it.shortClassName.asString() }
    }

    abstract fun symbolContainsRepeatableAnnotation(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean

    abstract fun extractBackingFieldAnnotationsFromProperty(
        property: FirProperty,
        session: FirSession,
        propertyAnnotations: List<FirAnnotation> = property.annotations,
        backingFieldAnnotations: List<FirAnnotation> = property.backingField?.annotations.orEmpty(),
    ): AnnotationsPosition?

    object Default : FirAnnotationsPlatformSpecificSupportComponent() {
        override val requiredAnnotationsWithArguments = setOf(
            StandardClassIds.Annotations.Deprecated,
            StandardClassIds.Annotations.Target,
        )

        override val requiredAnnotations = requiredAnnotationsWithArguments + setOf(
            StandardClassIds.Annotations.DeprecatedSinceKotlin,
            StandardClassIds.Annotations.SinceKotlin,
            StandardClassIds.Annotations.WasExperimental,
        )

        override val volatileAnnotations = setOf(
            StandardClassIds.Annotations.Volatile,
        )

        override val deprecationAnnotationsWithOverridesPropagation = mapOf(
            StandardClassIds.Annotations.Deprecated to true,
            StandardClassIds.Annotations.SinceKotlin to true,
        )

        override fun symbolContainsRepeatableAnnotation(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
            return symbol.getAnnotationByClassId(StandardClassIds.Annotations.Repeatable, session) != null
        }

        override fun extractBackingFieldAnnotationsFromProperty(
            property: FirProperty,
            session: FirSession,
            propertyAnnotations: List<FirAnnotation>,
            backingFieldAnnotations: List<FirAnnotation>,
        ): AnnotationsPosition? {
            return null
        }
    }
}

val FirSession.annotationPlatformSupport by FirSession.sessionComponentAccessor<FirAnnotationsPlatformSpecificSupportComponent>()

class AnnotationsPosition(
    val backingFieldAnnotations: List<FirAnnotation>,
    val propertyAnnotations: List<FirAnnotation>,
)
