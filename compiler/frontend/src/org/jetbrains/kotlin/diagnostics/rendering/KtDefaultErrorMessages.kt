/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtErrors.NON_LOCAL_RETURN_IN_DISABLED_INLINE
import org.jetbrains.kotlin.diagnostics.KtErrors.SUSPENSION_POINT_INSIDE_MONITOR
import org.jetbrains.kotlin.diagnostics.KtErrors.TYPEOF_ANNOTATED_TYPE
import org.jetbrains.kotlin.diagnostics.KtErrors.TYPEOF_EXTENSION_FUNCTION_TYPE
import org.jetbrains.kotlin.diagnostics.KtErrors.TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND
import org.jetbrains.kotlin.diagnostics.KtErrors.TYPEOF_SUSPEND_TYPE

class KtDefaultErrorMessages {
    companion object {

        val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
            map.put(NON_LOCAL_RETURN_IN_DISABLED_INLINE, "Non-local returns are not allowed with inlining disabled")
            map.put(TYPEOF_SUSPEND_TYPE, "Suspend functional types are not supported in typeOf")
            map.put(TYPEOF_EXTENSION_FUNCTION_TYPE, "Extension function types are not supported in typeOf")
            map.put(TYPEOF_ANNOTATED_TYPE, "Annotated types are not supported in typeOf")
            map.put(TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND, "Non-reified type parameters with recursive bounds are not supported yet: {0}", STRING)

            map.put(SUSPENSION_POINT_INSIDE_MONITOR, "A suspension point at {0} is inside a critical section", STRING)
        }
    }
}