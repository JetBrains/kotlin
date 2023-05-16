/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.components.ShortenCommand

internal object ShorteningResultsRenderer {
    fun StringBuilder.renderShorteningResults(shortening: ShortenCommand) {
        if (shortening.isEmpty) {
            appendLine("EMPTY_SHORTENINGS")
            return
        }

        shortening.typesToShorten.forEach { userType ->
            userType.element?.text?.let {
                appendLine("[type] $it")
            }
        }
        shortening.qualifiersToShorten.forEach { qualifier ->
            qualifier.element?.text?.let {
                appendLine("[qualifier] $it")
            }
        }
        shortening.kDocQualifiersToShorten.forEach { kdoc ->
            kdoc.element?.text?.let {
                appendLine("[kdoc] $it")
            }
        }
    }
}