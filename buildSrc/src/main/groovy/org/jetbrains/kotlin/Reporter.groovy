/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin

import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import groovy.io.FileVisitResult
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by minamoto on 2/7/17.
 */
class Reporter extends DefaultTask {

    private String buildLogUrlTab(String buildId, String buildTypeId) {
        return tabUrl(buildId, buildTypeId, "buildLog")
    }

    private String tabUrl(String buildId, String buildTypeId, tab) {
        return "http://buildserver.labs.intellij.net/viewLog.html?buildId=${buildId}&buildTypeId=${buildTypeId}&tab=${tab}"
    }

    private String testReportUrl(String buildId, String buildTypeId) {
        return tabUrl(buildId, buildTypeId,"testsInfo")
    }
    def reportHome
    @TaskAction
    public void report() {
        def stats = new RunExternalTestGroup.Statistics()
        def obj = new JsonSlurper().parse(new File(reportHome, "external/results.json"))
        stats.total   = obj.statistics.total
        stats.passed  = obj.statistics.passed
        stats.failed  = obj.statistics.failed
        stats.error   = obj.statistics.error
        stats.skipped = obj.statistics.skipped

        def teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE")
        if (teamcityConfig == null)
            throw new RuntimeException("Can't load teamcity config")

        def buildProperties = new Properties()
        buildProperties.load(new FileInputStream(teamcityConfig))
        def buildId = buildProperties.'teamcity.build.id'
        def buildTypeId = buildProperties.'teamcity.buildType.id'
        def logUrl = buildLogUrlTab(buildId, buildTypeId)
        def testReportUrl = testReportUrl(buildId, buildTypeId)
        def epilogue = "\nlog url: $logUrl\ntest report url: $testReportUrl"

        def report = "total: ${stats.total}\npassed: ${stats.passed}\nfailed: ${stats.failed}\nerror:${stats.error}\nskipped:${stats.skipped} ${epilogue}"
        println(report)
        def session = new SlackSessionFactory().createWebSocketSlackSession(buildProperties.'konan-reporter-token')
        session.connect()
        def channel = session.findChannelByName(buildProperties.'konan-channel-name')
        session.sendMessage(channel, "Hello, аборигены Котлина!\n текущий статус:\n${report}")
        session.disconnect()
    }

}
