/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent

internal object AnalysisApiSurfaceNames {
    @Suppress("OPT_IN_USAGE") // Suppress instead of @OptIn because the annotation is not available in the test sources
    val KA_SESSION_COMPONENT: String = KaSessionComponent::class.simpleName!!

    val KA_SESSION: String = KaSession::class.simpleName!!

    val DEPRECATED: String = Deprecated::class.simpleName!!

    val CONTEXT_PARAMETER_BRIDGE: String = KaContextParameterApi::class.simpleName!!
    const val CUSTOM_CONTEXT_PARAMETER_BRIDGE: String = "KaCustomContextParameterBridge"
    const val NO_CONTEXT_PARAMETER_BRIDGE_REQUIRED: String = "KaNoContextParameterBridgeRequired"

    const val KA_SPI: String = "KaSpi"
    val SUBCLASS_OPT_IN_REQUIRED: String = SubclassOptInRequired::class.simpleName!!
    val SUBCLASS_OPT_IN_REQUIRED_ANNOTATION: String = "@$SUBCLASS_OPT_IN_REQUIRED(KaImplementationDetail::class)"

    const val IMPLEMENTATION_DETAIL: String = "KaSessionComponentImplementationDetail"
    const val IMPLEMENTATION_DETAIL_ANNOTATION: String = "@$IMPLEMENTATION_DETAIL"
    val IMPLEMENTATION_DETAIL_SUBCLASS_ANNOTATION: String =
        "@${SubclassOptInRequired::class.simpleName}($IMPLEMENTATION_DETAIL::class)"
}
