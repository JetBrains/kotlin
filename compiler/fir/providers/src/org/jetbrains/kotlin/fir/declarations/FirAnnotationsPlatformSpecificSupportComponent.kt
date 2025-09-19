/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * @see org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox.Companion
 */
abstract class FirAnnotationsPlatformSpecificSupportComponent : FirComposableSessionComponent<FirAnnotationsPlatformSpecificSupportComponent> {
    abstract val requiredAnnotationsWithArguments: Set<ClassId>
    abstract val requiredAnnotations: Set<ClassId>
    abstract val volatileAnnotations: Set<ClassId>

    val requiredAnnotationsShortClassNames: Set<Name> by lazy {
        requiredAnnotations.mapTo(mutableSetOf()) { it.shortClassName }
    }

    val requiredAnnotationsWithArgumentsShortClassNames: Set<Name> by lazy {
        requiredAnnotationsWithArguments.mapTo(mutableSetOf()) { it.shortClassName }
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

    class Composed(
        override val components: List<FirAnnotationsPlatformSpecificSupportComponent>
    ) : FirAnnotationsPlatformSpecificSupportComponent(), FirComposableSessionComponent.Composed<FirAnnotationsPlatformSpecificSupportComponent> {
        override val requiredAnnotationsWithArguments: Set<ClassId> =
            components.flatMapTo(mutableSetOf()) { it.requiredAnnotationsWithArguments }
        override val requiredAnnotations: Set<ClassId> = components.flatMapTo(mutableSetOf()) { it.requiredAnnotations }
        override val volatileAnnotations: Set<ClassId> = components.flatMapTo(mutableSetOf()) { it.volatileAnnotations }
        override val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean> = buildMap {
            components.forEach { component ->
                putAll(component.deprecationAnnotationsWithOverridesPropagation)
            }
        }

        override fun symbolContainsRepeatableAnnotation(
            symbol: FirClassLikeSymbol<*>,
            session: FirSession,
        ): Boolean {
            return components.any { it.symbolContainsRepeatableAnnotation(symbol, session) }
        }

        override fun extractBackingFieldAnnotationsFromProperty(
            property: FirProperty,
            session: FirSession,
            propertyAnnotations: List<FirAnnotation>,
            backingFieldAnnotations: List<FirAnnotation>,
        ): AnnotationsPosition? {
            return components.firstNotNullOfOrNull {
                it.extractBackingFieldAnnotationsFromProperty(property, session, propertyAnnotations, backingFieldAnnotations)
            }
        }
    }

    @SessionConfiguration
    override fun createComposed(components: List<FirAnnotationsPlatformSpecificSupportComponent>): Composed {
        return Composed(components)
    }

    object Default : FirAnnotationsPlatformSpecificSupportComponent() {
        override val requiredAnnotationsWithArguments: Set<ClassId> = setOf(
            StandardClassIds.Annotations.Deprecated,
            StandardClassIds.Annotations.Target,
            StandardClassIds.Annotations.DeprecatedSinceKotlin,
            StandardClassIds.Annotations.SinceKotlin,
        )

        override val requiredAnnotations: Set<ClassId> = requiredAnnotationsWithArguments + setOf(
            StandardClassIds.Annotations.WasExperimental,
        )

        override val volatileAnnotations: Set<ClassId> = setOf(
            StandardClassIds.Annotations.Volatile,
        )

        override val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean> = mapOf(
            StandardClassIds.Annotations.Deprecated to true,
            StandardClassIds.Annotations.SinceKotlin to true,
        )

        override fun symbolContainsRepeatableAnnotation(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
            return symbol.hasAnnotationWithClassId(StandardClassIds.Annotations.Repeatable, session)
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

val FirSession.annotationPlatformSupport: FirAnnotationsPlatformSpecificSupportComponent by FirSession.sessionComponentAccessor<FirAnnotationsPlatformSpecificSupportComponent>()

class AnnotationsPosition(
    val backingFieldAnnotations: List<FirAnnotation>,
    val propertyAnnotations: List<FirAnnotation>,
)
