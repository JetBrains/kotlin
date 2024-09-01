/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

@KaExperimentalApi
public interface KaSymbolInformationProvider {
    /**
     * The deprecation status of the given symbol, or `null` if the declaration is not deprecated.
     */
    @KaExperimentalApi
    public val KaSymbol.deprecationStatus: DeprecationInfo?

    /**
     * The deprecation status of the given symbol, or `null` if the declaration is not deprecated.
     */
    @KaExperimentalApi
    public fun KaSymbol.deprecationStatus(annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo?

    @Deprecated(
        "Use 'deprecationStatus' instead.",
        replaceWith = ReplaceWith("deprecationStatus(annotationUseSiteTarget)")
    )
    @KaExperimentalApi
    public fun KaSymbol.getDeprecationStatus(annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? =
        deprecationStatus(annotationUseSiteTarget)

    /**
     * Deprecation status of the given property getter, or `null` if the getter is not deprecated.
     */
    @KaExperimentalApi
    public val KaPropertySymbol.getterDeprecationStatus: DeprecationInfo?

    /**
     * Deprecation status of the given property setter, or `null` if the setter is not deprecated or the property does not have a setter.
     */
    @KaExperimentalApi
    public val KaPropertySymbol.setterDeprecationStatus: DeprecationInfo?

    /** A set of applicable targets for an annotation class symbol, or `null` if the symbol is not an annotation class. */
    @KaExperimentalApi
    public val KaClassSymbol.annotationApplicableTargets: Set<KotlinTarget>?
}