/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoRenderer
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule

class GlobalMetadataInfoHandler(
    private val testServices: TestServices,
    private val processors: List<AdditionalMetaInfoProcessor>
) : TestService {
    private lateinit var existingInfosPerFile: Map<TestFile, List<ParsedCodeMetaInfo>>

    private val infosPerFile: MutableMap<TestFile, MutableList<CodeMetaInfo>> =
        mutableMapOf<TestFile, MutableList<CodeMetaInfo>>().withDefault { mutableListOf() }

    private val existingInfosPerFilePerInfoCache = mutableMapOf<Pair<TestFile, CodeMetaInfo>, List<ParsedCodeMetaInfo>>()

    @OptIn(ExperimentalStdlibApi::class)
    fun parseExistingMetadataInfosFromAllSources() {
        existingInfosPerFile = buildMap {
            for (file in testServices.moduleStructure.modules.flatMap { it.files }) {
                put(file, CodeMetaInfoParser.getCodeMetaInfoFromText(file.originalContent))
            }
        }
    }

    fun getExistingMetaInfosForFile(file: TestFile): List<ParsedCodeMetaInfo> {
        return existingInfosPerFile.getValue(file)
    }

    fun getReportedMetaInfosForFile(file: TestFile): List<CodeMetaInfo> {
        return infosPerFile.getValue(file)
    }

    fun getExistingMetaInfosForActualMetadata(file: TestFile, metaInfo: CodeMetaInfo): List<ParsedCodeMetaInfo> {
        return existingInfosPerFilePerInfoCache.getOrPut(file to metaInfo) {
            getExistingMetaInfosForFile(file).filter { it == metaInfo }
        }
    }

    fun addMetadataInfosForFile(file: TestFile, codeMetaInfos: List<CodeMetaInfo>) {
        val infos = infosPerFile.getOrPut(file) { mutableListOf() }
        infos += codeMetaInfos
    }

    fun compareAllMetaDataInfos(expectedTransformer: ((String) -> String)?) {
        // TODO: adapt to multiple testdata files
        val moduleStructure = testServices.moduleStructure
        val builder = StringBuilder()
        for (module in moduleStructure.modules) {
            for (file in module.files) {
                if (file.isAdditional) continue
                processors.forEach { it.processMetaInfos(module, file) }
                val codeMetaInfos = infosPerFile.getValue(file)
                val fileBuilder = StringBuilder()
                CodeMetaInfoRenderer.renderTagsToText(
                    fileBuilder,
                    codeMetaInfos,
                    testServices.sourceFileProvider.getContentOfSourceFile(file)
                )
                builder.append(fileBuilder.stripAdditionalEmptyLines(file))
            }
        }
        val actualText = builder.toString()
        val expectedFile = moduleStructure.originalTestDataFiles.single()
        if (expectedTransformer != null) {
            val expectedContent = expectedFile.readText().let(expectedTransformer)
            val message = "Actual data differs from transformed content of file $expectedFile"
            testServices.assertions.assertEquals(expectedContent, actualText) { message }
        } else {
            testServices.assertions.assertEqualsToFile(expectedFile, actualText)
        }
    }

    private fun StringBuilder.stripAdditionalEmptyLines(file: TestFile): CharSequence {
        return if (file.startLineNumberInOriginalFile != 0) {
            this.removePrefix((1..file.startLineNumberInOriginalFile).joinToString(separator = "") { "\n" })
        } else {
            this.toString()
        }
    }
}

val TestServices.globalMetadataInfoHandler: GlobalMetadataInfoHandler by TestServices.testServiceAccessor()

abstract class AdditionalMetaInfoProcessor(protected val testServices: TestServices) {
    protected val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    abstract fun processMetaInfos(module: TestModule, file: TestFile)
}
