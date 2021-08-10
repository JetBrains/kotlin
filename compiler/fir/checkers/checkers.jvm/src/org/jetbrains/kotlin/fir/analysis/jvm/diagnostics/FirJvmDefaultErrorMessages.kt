/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.diagnostics

import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDefaultErrorMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.CONFLICTING_JVM_DECLARATIONS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JAVA_TYPE_MISMATCH

object FirJvmDefaultErrorMessages {
    fun installJvmErrorMessages() {
        FirDefaultErrorMessages.Companion.MAP.also { map ->
            map.put(CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash")
            map.put(JAVA_TYPE_MISMATCH, "Java type mismatch expected {0} but found {1}. Use explicit cast", RENDER_TYPE, RENDER_TYPE)
        }
    }
}