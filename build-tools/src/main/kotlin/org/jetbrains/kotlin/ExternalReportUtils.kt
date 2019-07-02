package org.jetbrains.kotlin

import kotlinx.io.PrintWriter
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader


@Serializable
data class ExternalTestReport(val statistics: Statistics, val groups: List<KonanTestGroupReport>)

@ImplicitReflectionSerializer
fun saveReport(reportFileName: String, statistics: Statistics, groups:List<KonanTestGroupReport>){
    File(reportFileName).apply {
        parentFile.mkdirs()
        PrintWriter(this).use {
            it.append(Json.stringify(ExternalTestReport(statistics, groups)))
        }
    }
}

@ImplicitReflectionSerializer
fun loadReport(reportFileName: String) : ExternalTestReport = FileReader(reportFileName).use {
        return@use Json.parse<ExternalTestReport>(it.readText())
    }
