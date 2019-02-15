/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.util.*

open class Reporter : DefaultTask() {
    private fun buildLogUrlTab(buildId: String, buildTypeId: String): String = tabUrl(buildId, buildTypeId, "buildLog")

    private fun tabUrl(buildId: String, buildTypeId: String, tab: String): String =
            "http://buildserver.labs.intellij.net/viewLog.html?buildId=$buildId&buildTypeId=$buildTypeId&tab=$tab"

    private fun testReportUrl(buildId: String, buildTypeId: String): String = tabUrl(buildId, buildTypeId, "testsInfo")

    @Input
    lateinit var reportHome: String

    @TaskAction
    fun report() {
        val teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE")
                ?: throw  RuntimeException("Can't load teamcity config")

        val buildProperties = Properties()
        buildProperties.load(FileInputStream(teamcityConfig))
        val buildId = buildProperties.getProperty("teamcity.build.id")
        val buildTypeId = buildProperties.getProperty("teamcity.buildType.id")
        val logUrl = buildLogUrlTab(buildId, buildTypeId)
        val testReportUrl = testReportUrl(buildId, buildTypeId)
        var epilogue = "\nlog url: $logUrl\ntest report url: $testReportUrl"

        val resultsFile = File(reportHome, "external/results.json")
        val report: String = if (resultsFile.exists()) {
            @Suppress("UNCHECKED_CAST")
            val obj = JsonSlurper().parse(resultsFile) as? AbstractMap<String, Any>
                    ?: throw IllegalStateException("Got incorrect JSON object: ${resultsFile.absolutePath}")
            @Suppress("UNCHECKED_CAST")
            val statsJSON = obj["statistics"] as? AbstractMap<String, Int>
                    ?: throw IllegalStateException("Got incorrect statistics object in JSON:" +
                            resultsFile.absolutePath)
            val stats = Statistics(
                    passed = statsJSON["passed"]!!,
                    failed = statsJSON["failed"]!!,
                    error = statsJSON["error"]!!,
                    skipped = statsJSON["skipped"]!!)
            val total = statsJSON["total"]!!
            if (total != stats.total) {
                val message = "WARNING: total amount of tests doesn't match with the sum of failed, passed, error & skipped"
                println(message)
                epilogue += message
            }
            "total: ${stats.total}\npassed: ${stats.passed}\nfailed: ${stats.failed}\n" +
                    "error:${stats.error}\nskipped:${stats.skipped} $epilogue"
        } else {
            "Unable to get results\nBuild has probably failed$epilogue"
        }
        println(report)
        with(SlackSessionFactory.createWebSocketSlackSession(buildProperties.getProperty("konan-reporter-token"))) {
            connect()
            sendMessage(findChannelByName(buildProperties.getProperty("konan-channel-name")),
                    "Hello, аборигены Котлина!\n текущий статус:\n$report")
            disconnect()
        }
    }
}