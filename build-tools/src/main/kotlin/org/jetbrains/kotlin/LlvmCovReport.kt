package org.jetbrains.kotlin

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.Json


@Serializable
data class LlvmCovReportFunction(
        val name: String,
        val count: Int,
        val regions: List<List<Int>>,
        val filenames: List<String>
)

@Serializable
data class LlvmCovReportSummary(
        val lines: LlvmCovReportStatistics,
        val functions: LlvmCovReportStatistics,
        val instantiations: LlvmCovReportStatistics,
        val regions: LlvmCovReportStatistics

)

/**
 * TODO: Add support for `segments` field later.
 *  It's a bit complicated since every segment
 *  is encoded not as dictionary, but as array of ints and bools.
 */
@Serializable
data class LlvmCovReportFile(
        val filename: String,
        val summary: LlvmCovReportSummary
)

@Serializable
data class LlvmCovReportStatistics(
    val count: Int,
    val covered: Int,
    val percent: Double
)

@Serializable
data class LlvmCovReportData(
        val files: List<LlvmCovReportFile>,
        val functions: List<LlvmCovReportFunction>,
        val totals: LlvmCovReportSummary
)

@Serializable
data class LlvmCovReport(
        val version: String,
        val type: String,
        val data: List<LlvmCovReportData>
)

fun parseLlvmCovReport(llvmCovReport: String): LlvmCovReport =
        Json.nonstrict.parse(LlvmCovReport.serializer(), llvmCovReport)

val LlvmCovReport.isValid
    get() = type == "llvm.coverage.json.export"

