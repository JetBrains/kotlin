/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollectorWithSuppress

object DiagnosticReporterFactory {
    // kept only to keep compatibility with Compose tests
    fun createReporter(messageCollector: MessageCollector): BaseDiagnosticsCollector {
        return createReporter()
    }

    fun createReporter(disableSuppress: Boolean = false): BaseDiagnosticsCollector {
        return if (disableSuppress) {
            SimpleDiagnosticsCollector()
        } else {
            SimpleDiagnosticsCollectorWithSuppress()
        }
    }

    fun createPendingReporter(): PendingDiagnosticsCollectorWithSuppress {
        return PendingDiagnosticsCollectorWithSuppress()
    }
}
