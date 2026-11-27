/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.utils.createTypePointer
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirFlexibleType(
    override val coneType: ConeFlexibleType,
    override val analysisSession: KaFirSession,
) : KaFlexibleType(), KaFirType {

    override val lowerBound: KaType get() = entryPointImplementation { firSymbolBuilder.typeBuilder.buildKtType(coneType.lowerBound) }
    override val upperBound: KaType get() = entryPointImplementation { firSymbolBuilder.typeBuilder.buildKtType(coneType.upperBound) }
    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            KaFirAnnotationListForType.create(coneType)
        }

    override val abbreviation: KaUsualClassType?
        get() = entryPointImplementation { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaFlexibleType> = entryPointImplementation {
        return createTypePointer(coneType, ::KaFirFlexibleType)
    }
}
