/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformUpperBoundsProvider
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.java.enhancement.EnhancedForWarningConeSubstitutor
import org.jetbrains.kotlin.fir.java.enhancement.enhancedTypeForWarning
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeContext

class FirJavaNullabilityWarningUpperBoundsProvider(session: FirSession) : FirPlatformUpperBoundsProvider {
    private val substitutor: EnhancedForWarningConeSubstitutor = EnhancedForWarningConeSubstitutor(session.typeContext)

    override val diagnostic: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType>
        get() = FirJvmErrors.UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS

    override val diagnosticForTypeAlias: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType>
        get() = FirJvmErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS

    override fun getAdditionalUpperBound(coneKotlinType: ConeKotlinType): ConeKotlinType? {
        return substitutor.substituteOrNull(coneKotlinType)
    }
}