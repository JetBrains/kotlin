package org.jetbrains.kotlin

import com.google.gson.*
import com.google.gson.annotations.*


data class LlvmCovReportFunction(
        val name: String,
        val count: Int,
        val regions: List<List<Int>>,
        val filenames: List<String>
)

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
data class LlvmCovReportFile(
        @Expose val filename: String,
        @Expose val summary: LlvmCovReportSummary
)

data class LlvmCovReportStatistics(
    @Expose val count: Int,
    @Expose val covered: Int,
    @Expose val percent: Double
)

data class LlvmCovReportData(
        @Expose val files: List<LlvmCovReportFile>,
        @Expose val functions: List<LlvmCovReportFunction>,
        @Expose val totals: LlvmCovReportSummary
)

data class LlvmCovReport(
        @Expose val version: String,
        @Expose val type: String,
        @Expose val data: List<LlvmCovReportData>
)

fun parseLlvmCovReport(llvmCovReport: String): LlvmCovReport = gson.fromJson(llvmCovReport, LlvmCovReport::class.java)

val LlvmCovReport.isValid
    get() = type == "llvm.coverage.json.export"

