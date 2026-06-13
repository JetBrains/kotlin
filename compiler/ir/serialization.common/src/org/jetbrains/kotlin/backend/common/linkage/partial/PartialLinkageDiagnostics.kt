/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

object PartialLinkageDiagnostics : KtDiagnosticsContainer() {
    val MINOR_PARTIAL_LINKAGE_ISSUE = KtSourcelessDiagnosticFactory("MINOR_PARTIAL_LINKAGE_ISSUE", Severity.INFO, getRendererFactory())
    val MAJOR_PARTIAL_LINKAGE_ISSUE = KtSourcelessDiagnosticFactory("MAJOR_PARTIAL_LINKAGE_ISSUE", Severity.WARNING, getRendererFactory())

    val IR_LINKER_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDefaultPartialLinkageErrorMessages
}

enum class PartialLinkageIssueSignificance {
    MINOR, MAJOR;

    fun toDiagnosticFactory() = when (this) {
        MINOR -> PartialLinkageDiagnostics.MINOR_PARTIAL_LINKAGE_ISSUE
        MAJOR -> PartialLinkageDiagnostics.MAJOR_PARTIAL_LINKAGE_ISSUE
    }

    companion object {
        fun minorIf(condition: () -> Boolean): PartialLinkageIssueSignificance =
            if (condition()) MINOR else MAJOR
    }
}

private object KtDefaultPartialLinkageErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(PartialLinkageDiagnostics.MINOR_PARTIAL_LINKAGE_ISSUE, "{0}")
        map.put(PartialLinkageDiagnostics.MAJOR_PARTIAL_LINKAGE_ISSUE, "{0}")
        map.put(PartialLinkageDiagnostics.IR_LINKER_ERROR, "{0}")
    }
}
