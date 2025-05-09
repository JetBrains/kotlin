/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.hasFlexibleMarkedNullability
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirFlexibleType(
    override val coneType: ConeFlexibleType,
    private val builder: KaSymbolByFirBuilder,
) : KaFlexibleType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    override val lowerBound: KaType get() = withValidityAssertion { builder.typeBuilder.buildKtType(coneType.lowerBound) }
    override val upperBound: KaType get() = withValidityAssertion { builder.typeBuilder.buildKtType(coneType.upperBound) }
    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForType.create(coneType, builder)
        }

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability
        get() = withValidityAssertion {
            if (coneType.hasFlexibleMarkedNullability) {
                KaTypeNullability.UNKNOWN
            } else {
                KaTypeNullability.create(coneType.lowerBound.isMarkedNullable)
            }
        }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaFlexibleType> = withValidityAssertion {
        return createTypePointer(coneType, builder, ::KaFirFlexibleType)
    }
}