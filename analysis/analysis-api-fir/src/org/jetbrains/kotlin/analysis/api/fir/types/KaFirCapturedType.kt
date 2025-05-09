/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.createTypePointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirCapturedType(
    override val coneType: ConeCapturedType,
    private val builder: KaSymbolByFirBuilder,
) : KaCapturedType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability get() = withValidityAssertion { KaTypeNullability.create(coneType.isMarkedNullable) }

    override val projection: KaTypeProjection
        get() = withValidityAssertion { builder.typeBuilder.buildTypeProjection(coneType.constructor.projection) }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForType.create(coneType, builder)
        }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaCapturedType> = withValidityAssertion {
        return createTypePointer(coneType, builder, ::KaFirCapturedType)
    }
}