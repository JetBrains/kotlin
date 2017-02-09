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
        reportHome.traverse {
            println("$it $it.name")
            if (it.name == 'results.json') {
                def obj = new JsonSlurper().parse(it)
                stats.total   += obj.statistics.total
                stats.passed  += obj.statistics.passed
                stats.failed  += obj.statistics.failed
                stats.error   += obj.statistics.error
                stats.skipped += obj.statistics.skipped
            }
            FileVisitResult.CONTINUE
        }

        def epilog = ""
        def teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE")
        if (teamcityConfig != null) {
            def buildProperties = new Properties()
            buildProperties.load(new FileInputStream(teamcityConfig))
            def logUrl = buildLogUrlTab(buildProperties.'teamcity.build.id', buildProperties.'teamcity.buildType.id')
            def testReportUrl = testReportUrl(buildProperties.'teamcity.build.id', buildProperties.'teamcity.buildType.id')
            epilog = "\nlog url: $logUrl\ntest report url: $testReportUrl"
        }

        def report = "total: ${stats.total}\npassed: ${stats.passed}\nfailed: ${stats.failed}\nerror:${stats.error}\nskipped:${stats.skipped} ${epilog}"
        println(report)
        def session = new SlackSessionFactory().createWebSocketSlackSession("xoxb-137371102001-DaYxLJEmbhOZQiR4XFRLZuHG")
        session.connect()
        def channel = session.findChannelByName("kotlin-native-team")
        session.sendMessage(channel, "Hello, аборигены Котлина!\n текущий статус:\n${report}")
        session.disconnect()
    }

}
