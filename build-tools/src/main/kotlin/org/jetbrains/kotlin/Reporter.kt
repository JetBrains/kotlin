/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import kotlinx.serialization.ImplicitReflectionSerializer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.FileInputStream
import java.util.*


internal object Tc {
    private val teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE")
    val enabled:Boolean = (teamcityConfig != null)
    private val buildConfig by lazy {
        teamcityConfig ?: return@lazy null
        val properties = Properties()
        properties.load(FileInputStream(teamcityConfig))
        properties
    }
    val buildId = buildConfig?.getProperty("teamcity.build.id")
    val buildTypeId = buildConfig?.getProperty("teamcity.buildType.id")
    val konanReporterToken = buildConfig?.getProperty("konan-reporter-token")
    val konanChannelName = buildConfig?.getProperty("konan-channel-name")

}

private fun buildLogUrlTab(buildId: String?, buildTypeId: String?): String = tabUrl(buildId, buildTypeId, "buildLog")

private fun tabUrl(buildId: String?, buildTypeId: String?, tab: String?): String =
        "http://buildserver.labs.intellij.net/viewLog.html?buildId=$buildId&buildTypeId=$buildTypeId&tab=$tab"

private fun testReportUrl(buildId: String?, buildTypeId: String?): String = tabUrl(buildId, buildTypeId, "testsInfo")

private fun sendTextToSlack(report: String) {
    with(SlackSessionFactory.createWebSocketSlackSession(Tc.konanReporterToken)) {
        connect()
        sendMessage(findChannelByName(Tc.konanChannelName),
                "Hello, аборигены Котлина!\n текущий статус:\n$report")
        disconnect()
    }
}

private fun reportEpilogue(): String {
    val logUrl = buildLogUrlTab(Tc.buildId, Tc.buildTypeId)
    val testReportUrl = testReportUrl(Tc.buildId, Tc.buildTypeId)
    return "\nlog url: $logUrl\ntest report url: $testReportUrl"
}

private val Statistics.report
        get() = "total: $total\npassed: $passed\nfailed: $failed\nerror: $error\nskipped: $skipped"

private val Statistics.oneLineReport
    get() = "(total: $total, passed: $passed, failed: $failed, error: $error, skipped: $skipped)"


open class Reporter : DefaultTask() {

    @Input
    lateinit var reportHome: String

    @ImplicitReflectionSerializer
    @TaskAction
    fun report() {

        val reportJson = loadReport("$reportHome/external/results.json")

        val report: String =
            "${reportJson.statistics.report}\n ${reportEpilogue()}"

        project.logger.info(report)
        sendTextToSlack(report)
    }


}

open class NightlyReporter: DefaultTask() {
    @Input
    lateinit var externalMacosReport:String
    @Input
    lateinit var externalLinuxReport:String
    @Input
    lateinit var externalWindowsReport:String

    @ImplicitReflectionSerializer
    @TaskAction
    fun report() {
        val externalMacosJsonReport = loadReport("${project.rootDir.absolutePath}/$externalMacosReport")
        val externalLinuxJsonReport = loadReport("${project.rootDir.absolutePath}/$externalLinuxReport")
        val externalWindowsJsonReport = loadReport("${project.rootDir.absolutePath}/$externalWindowsReport")
        val report = buildString {
            append("Mac OS ")
            appendln(externalMacosJsonReport.statistics.oneLineReport)
            append("Linux ")
            appendln(externalLinuxJsonReport.statistics.oneLineReport)
            append("Windows ")
            appendln(externalWindowsJsonReport.statistics.oneLineReport)
            appendln(reportEpilogue())
        }
        project.logger.info(report)
        sendTextToSlack(report)
    }
}