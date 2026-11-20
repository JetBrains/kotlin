/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoRenderer
import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCodeMetaRenderConfiguration.Companion.METADATA_TAG
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalMetaInfoProcessor
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Lets LL tests share the diagnostics of the metadata compilation with the compiler's KMP test data.
 *
 * The compiler's diagnostic tests render the diagnostics of the simulated metadata compilation with [METADATA_TAG]. LL tests analyze common
 * modules in metadata sessions, so they report the same diagnostics, but without the tag.
 *
 * The processor takes the tag from the expected test data: a reported diagnostic gets [METADATA_TAG] if the test data has it only from the
 * metadata compilation. If the test data also has it without the tag, the reported diagnostic matches that one, and the tagged one is added.
 *
 * Diagnostics of the metadata compilation that LL tests don't report still fail the test.
 *
 * @see withoutMetadataCompilationTags
 */
internal class LLKmpMetaInfoProcessor(testServices: TestServices) : AdditionalMetaInfoProcessor(testServices) {
    override fun processMetaInfos(module: TestModule, file: TestFile) {
        val existingInfos = globalMetadataInfoHandler.getExistingMetaInfosForFile(file)
        val reportedInfos = globalMetadataInfoHandler.getReportedMetaInfosForFile(file)
        val newInfos = mutableListOf<CodeMetaInfo>()

        for (metadataInfo in existingInfos.filter { it.isFromMetadataCompilation }) {
            val matchingReportedInfos = reportedInfos.filter { metadataInfo == it }
            if (matchingReportedInfos.isEmpty()) continue

            if (existingInfos.any { metadataInfo == it && !it.isFromMetadataCompilation }) {
                newInfos += metadataInfo.copy()
            } else {
                matchingReportedInfos.forEach { it.attributes += METADATA_TAG }
            }
        }

        globalMetadataInfoHandler.addMetadataInfosForFile(file, newInfos)
    }
}

/**
 * Renders the test data with the diagnostics of the metadata compilation untagged, as [LLKmpMetaInfoProcessor] expects LL tests to report
 * them.
 */
internal fun String.withoutMetadataCompilationTags(): String {
    val infos = CodeMetaInfoParser.getCodeMetaInfoFromText(this)
    if (infos.none { it.isFromMetadataCompilation }) return this

    val llInfos = infos
        .map { info -> info.copy().apply { attributes.remove(METADATA_TAG) } }
        .distinctBy { listOf(it.start, it.end, it.tag, it.attributes, it.description) }

    return CodeMetaInfoRenderer.renderTagsToText(llInfos, clearTextFromDiagnosticMarkup(this)).toString()
}

private val ParsedCodeMetaInfo.isFromMetadataCompilation: Boolean
    get() = METADATA_TAG in attributes
