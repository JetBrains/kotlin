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
        def report = "total: ${stats.total}\npassed: ${stats.passed}\nfailed: ${stats.failed}\nerror:${stats.error}\nskipped:${stats.skipped}"
        println(report)
        def session = new SlackSessionFactory().createWebSocketSlackSession("xoxb-137371102001-DaYxLJEmbhOZQiR4XFRLZuHG")
        session.connect()
        def channel = session.findChannelByName("kotlin-native-team")
        session.sendMessage(channel, "Hello, аборигены Котлина!\n текущий статус:\n${report}")
        session.disconnect()
    }

}
