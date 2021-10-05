/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.BackendErrors

object KtDefaultErrorMessages : BaseDiagnosticRendererFactory() {

    override val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
        map.put(BackendErrors.NON_LOCAL_RETURN_IN_DISABLED_INLINE, "Non-local returns are not allowed with inlining disabled")
    }

}
