/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.utils.createTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeTypeVariableTypeIsNotInferred
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirErrorType(
    override val coneType: ConeErrorType,
    override val analysisSession: KaFirSession,
) : KaErrorType, KaFirType {

    @KaNonPublicApi
    override val errorMessage: String
        get() = entryPointImplementation { coneType.diagnostic.reason }

    @KaNonPublicApi
    override val presentableText: String?
        get() = entryPointImplementation {
            when (val diagnostic = coneType.diagnostic) {
                is ConeCannotInferTypeParameterType -> diagnostic.typeParameter.name.asString()
                is ConeTypeVariableTypeIsNotInferred -> diagnostic.typeVariableType.typeConstructor.debugName
                else -> null
            }
        }

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
    override fun createPointer(): KaTypePointer<KaErrorType> = entryPointImplementation {
        return createTypePointer(coneType, ::KaFirErrorType)
    }
}
