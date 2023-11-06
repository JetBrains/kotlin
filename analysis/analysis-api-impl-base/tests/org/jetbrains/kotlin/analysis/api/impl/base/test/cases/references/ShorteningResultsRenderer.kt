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

        shortening.listOfTypeToShortenInfo.forEach { (userType, shortenedRef) ->
            userType.element?.text?.let {
                appendLine("[type] $it${shortenedRef?.let { ref -> " -> $ref" } ?: ""}")
            }
        }
        shortening.listOfQualifierToShortenInfo.forEach { (qualifier, shortenedRef) ->
            qualifier.element?.text?.let {
                appendLine("[qualifier] $it${shortenedRef?.let { ref -> " -> $ref" } ?: ""}")
            }
        }
        shortening.thisLabelsToShorten.forEach { thisLabel ->
            thisLabel.labelToShorten.element?.text?.let {
                appendLine("[thisLabel] $it")
            }
        }
        shortening.kDocQualifiersToShorten.forEach { kdoc ->
            kdoc.element?.text?.let {
                appendLine("[kdoc] $it")
            }
        }
    }
}