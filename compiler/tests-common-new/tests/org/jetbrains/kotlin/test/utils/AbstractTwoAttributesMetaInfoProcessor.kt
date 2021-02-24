/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalMetaInfoProcessor
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractTwoAttributesMetaInfoProcessor(testServices: TestServices) : AdditionalMetaInfoProcessor(testServices) {
    protected abstract val firstAttribute: String
    protected abstract val secondAttribute: String

    protected abstract fun processorEnabled(module: TestModule): Boolean
    protected abstract fun firstAttributeEnabled(module: TestModule): Boolean

    override fun processMetaInfos(module: TestModule, file: TestFile) {
        /*
         * Rules for OI/NI attribute:
         * ┌──────────┬───────┬────────┬──────────┐
         * │          │ first │ second │ nothing  │ <- reported
         * ├──────────┼───────┼────────┼──────────┤
         * │  nothing │  both │  both  │ nothing  │
         * │   first  │ first │  both  │  first   │
         * │  second  │  both │ second │  second  │
         * │   both   │  both │  both  │ opposite │ <- first if second enabled in test and vice versa
         * └──────────┴───────┴────────┴──────────┘
         *       ^ existed
         */
        if (!processorEnabled(module)) return
        val (currentFlag, otherFlag) = when (firstAttributeEnabled(module)) {
            true -> firstAttribute to secondAttribute
            false -> secondAttribute to firstAttribute
        }
        val matchedExistedInfos = mutableSetOf<ParsedCodeMetaInfo>()
        val matchedReportedInfos = mutableSetOf<CodeMetaInfo>()
        val allReportedInfos = globalMetadataInfoHandler.getReportedMetaInfosForFile(file)
        for ((_, reportedInfos) in allReportedInfos.groupBy { Triple(it.start, it.end, it.tag) }) {
            val existedInfos = globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, reportedInfos.first())
            for ((reportedInfo, existedInfo) in reportedInfos.zip(existedInfos)) {
                matchedExistedInfos += existedInfo
                matchedReportedInfos += reportedInfo
                if (currentFlag !in reportedInfo.attributes) continue
                if (currentFlag in existedInfo.attributes) continue
                reportedInfo.attributes.remove(currentFlag)
            }
        }

        if (allReportedInfos.size != matchedReportedInfos.size) {
            for (info in allReportedInfos) {
                if (info !in matchedReportedInfos) {
                    info.attributes.remove(currentFlag)
                }
            }
        }

        val allExistedInfos = globalMetadataInfoHandler.getExistingMetaInfosForFile(file)
        if (allExistedInfos.size == matchedExistedInfos.size) return

        val newInfos = allExistedInfos.mapNotNull {
            if (it in matchedExistedInfos) return@mapNotNull null
            if (currentFlag in it.attributes) return@mapNotNull null
            it.copy().apply {
                if (otherFlag !in attributes) {
                    attributes += otherFlag
                }
            }
        }
        globalMetadataInfoHandler.addMetadataInfosForFile(file, newInfos)
    }
}
