/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

@KaExperimentalApi
public interface KaSymbolInformationProvider {
    /**
     * Gets the deprecation status of the given symbol. Returns null if the symbol is not deprecated.
     */
    @KaExperimentalApi
    public val KaSymbol.deprecationStatus: DeprecationInfo?

    /**
     * Gets the deprecation status of the given symbol. Returns null if the symbol is not deprecated.
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
     * Gets the deprecation status of the getter of this property symbol. Returns null if the getter is not deprecated.
     */
    @KaExperimentalApi
    public val KaPropertySymbol.getterDeprecationStatus: DeprecationInfo?

    /**
     * Gets the deprecation status of the setter of this property symbol. Returns null if the setter it not deprecated or the property does
     * not have a setter.
     */
    @KaExperimentalApi
    public val KaPropertySymbol.setterDeprecationStatus: DeprecationInfo?

    /** Gets the set of applicable targets for an annotation class symbol. Returns `null` if the symbol is not an annotation class. */
    @KaExperimentalApi
    public val KaClassOrObjectSymbol.annotationApplicableTargets: Set<KotlinTarget>?
}