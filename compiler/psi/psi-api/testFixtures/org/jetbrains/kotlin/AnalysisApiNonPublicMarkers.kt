/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

/**
 * The object contains the names of the annotations that are used to mark API endpoints as non-public (implementation details, platform
 * interface, IDE API, and so on).
 *
 * These names are recognized by [AbstractAnalysisApiInternalApiTest] when deciding whether a public declaration in an Analysis API
 * implementation module is properly hidden from the public API surface.
 *
 * String literals are used because this file lives in `compiler/psi/psi-api/testFixtures` and therefore cannot reference annotation
 * classes from the `analysis-api` module without introducing a dependency cycle.
 */
object AnalysisApiNonPublicMarkers {
    const val KA_IMPLEMENTATION_DETAIL: String = "KaImplementationDetail"
    const val KA_IMPLEMENTATION_DETAIL_ANNOTATION: String = "@$KA_IMPLEMENTATION_DETAIL"

    const val KA_EXPERIMENTAL_API: String = "KaExperimentalApi"
    const val KA_PLATFORM_INTERFACE: String = "KaPlatformInterface"
    const val KA_NON_PUBLIC_API: String = "KaNonPublicApi"
    const val KA_IDE_API: String = "KaIdeApi"

    const val LL_FIR_INTERNALS: String = "LLFirInternals"
    const val LL_FIR_INTERNALS_ANNOTATION: String = "@$LL_FIR_INTERNALS"

    const val REQUIRES_OPT_IN: String = "RequiresOptIn"

    val INTERNAL_API_MARKER_ANNOTATIONS: Set<String> = setOf(
        KA_IMPLEMENTATION_DETAIL,
        KA_EXPERIMENTAL_API,
        KA_PLATFORM_INTERFACE,
        KA_NON_PUBLIC_API,
        KA_IDE_API,
        LL_FIR_INTERNALS,
    )
}
