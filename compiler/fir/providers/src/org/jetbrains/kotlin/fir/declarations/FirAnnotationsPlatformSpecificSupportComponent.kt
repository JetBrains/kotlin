/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * @see org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox.Companion
 */
abstract class FirAnnotationsPlatformSpecificSupportComponent :
    FirComposableSessionComponent<FirAnnotationsPlatformSpecificSupportComponent> {
    abstract val requiredAnnotationsWithArguments: CompilerRequiredParametersMap
    abstract val requiredAnnotations: Set<ClassId>
    abstract val volatileAnnotations: Set<ClassId>
    protected abstract val repeatableAnnotations: Set<ClassId>
    abstract val jvmInlineAnnotationClassId: ClassId?

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

    fun symbolContainsRepeatableAnnotation(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
        return symbol.resolvedAnnotationsWithClassIds.getAnnotationByClassIds(repeatableAnnotations, session) != null
    }

    abstract fun extractBackingFieldAnnotationsFromProperty(
        property: FirProperty,
        session: FirSession,
        propertyAnnotations: List<FirAnnotation> = property.annotations,
        backingFieldAnnotations: List<FirAnnotation> = property.backingField?.annotations.orEmpty(),
    ): AnnotationsPosition?

    class Composed(
        override val components: List<FirAnnotationsPlatformSpecificSupportComponent>,
    ) : FirAnnotationsPlatformSpecificSupportComponent(),
        FirComposableSessionComponent.Composed<FirAnnotationsPlatformSpecificSupportComponent> {
        override val requiredAnnotationsWithArguments: CompilerRequiredParametersMap = buildMap {
            components.forEach {
                it.requiredAnnotationsWithArguments.forEach { [key, value] ->
                    require(!this.containsKey(key))
                    this[key] = value
                }
            }
        }

        override val requiredAnnotations: Set<ClassId> = components.flatMapTo(mutableSetOf()) { it.requiredAnnotations }
        override val volatileAnnotations: Set<ClassId> = components.flatMapTo(mutableSetOf()) { it.volatileAnnotations }
        override val repeatableAnnotations: Set<ClassId> = components.flatMapTo(mutableSetOf()) { it.repeatableAnnotations }
        override val jvmInlineAnnotationClassId: ClassId? = components.firstNotNullOfOrNull { it.jvmInlineAnnotationClassId }
        override val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean> = buildMap {
            components.forEach { component ->
                putAll(component.deprecationAnnotationsWithOverridesPropagation)
            }
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
        override val requiredAnnotationsWithArguments: CompilerRequiredParametersMap = buildMap {
            this[StandardClassIds.Annotations.Deprecated] = [
                FirCompilerRequiredParameterDescription(
                    name = StandardClassIds.Annotations.ParameterNames.deprecatedLevel,
                    kind = FirCraParameterKind.EnumParameter(StandardClassIds.DeprecationLevel, isVararg = false),
                    position = 2,
                )
            ]

            this[StandardClassIds.Annotations.Target] = [
                FirCompilerRequiredParameterDescription(
                    name = StandardClassIds.Annotations.ParameterNames.targetAllowedTargets,
                    kind = FirCraParameterKind.EnumParameter(StandardClassIds.AnnotationTarget, isVararg = true),
                    position = null,
                )
            ]

            this[StandardClassIds.Annotations.DeprecatedSinceKotlin] = [
                FirCompilerRequiredParameterDescription(
                    name = StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinWarningSince,
                    kind = FirCraParameterKind.LiteralParameter(ConstantValueKind.String),
                    position = 0,
                ),
                FirCompilerRequiredParameterDescription(
                    name = StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinErrorSince,
                    kind = FirCraParameterKind.LiteralParameter(ConstantValueKind.String),
                    position = 1,
                ),
                FirCompilerRequiredParameterDescription(
                    name = StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinHiddenSince,
                    kind = FirCraParameterKind.LiteralParameter(ConstantValueKind.String),
                    position = 2,
                ),
            ]

            this[StandardClassIds.Annotations.SinceKotlin] = [
                FirCompilerRequiredParameterDescription(
                    name = StandardClassIds.Annotations.ParameterNames.sinceKotlinVersion,
                    kind = FirCraParameterKind.LiteralParameter(ConstantValueKind.String),
                    position = 0,
                )
            ]
        }

        override val requiredAnnotations: Set<ClassId> = requiredAnnotationsWithArguments.keys + setOf(
            StandardClassIds.Annotations.WasExperimental,
            StandardClassIds.Annotations.EqualityBound,
        )

        override val volatileAnnotations: Set<ClassId> = setOf(
            StandardClassIds.Annotations.Volatile,
        )

        override val repeatableAnnotations: Set<ClassId> = setOf(
            StandardClassIds.Annotations.Repeatable,
        )

        override val jvmInlineAnnotationClassId: ClassId?
            get() = null

        override val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean> = mapOf(
            StandardClassIds.Annotations.Deprecated to true,
            StandardClassIds.Annotations.SinceKotlin to true,
        )

        override fun extractBackingFieldAnnotationsFromProperty(
            property: FirProperty,
            session: FirSession,
            propertyAnnotations: List<FirAnnotation>,
            backingFieldAnnotations: List<FirAnnotation>,
        ): AnnotationsPosition? {
            return null
        }
    }

    protected typealias CompilerRequiredParametersMap = Map<ClassId, List<FirCompilerRequiredParameterDescription>>
}

val FirSession.annotationPlatformSupport: FirAnnotationsPlatformSpecificSupportComponent by FirSession.sessionComponentAccessor<FirAnnotationsPlatformSpecificSupportComponent>()

class AnnotationsPosition(
    val backingFieldAnnotations: List<FirAnnotation>,
    val propertyAnnotations: List<FirAnnotation>,
)
