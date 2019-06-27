package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option


open class KotlinBuildPusher: DefaultTask() {

    @Input
    @Option(option = "token", description = "Teamcity Bear token")
    var token:String? = ""

    @Input
    @Option(option = "buildServer", description = "Teamcity server")
    var buildServer:String = ""

    @Input
    @Option(option = "kotlinVersion", description = "Kotlin Compiler Version")
    var kotlinVersion:String = ""

    @Input
    @Option(option = "konanVersion", description = "Kotlin Native Compiler Version")
    var konanVersion:String = ""

    @TaskAction
    fun run() {
        requireNotNull(token, {"Teamcity Bear token required"})
        val client = HttpClient()
        runBlocking {
            val buildId = client.get<String>(scheme = "https", host = buildServer, path = "/app/rest/builds/buildType:(id:Kotlin_dev_Compiler),number:$kotlinVersion/id" ) {
                header("Authorization", "Bearer $token")
            }
            project.logger.info("pusher: buildId: $buildId")
            /**
             * <build>
             * <buildType id="Kotlin_dev_DeployMavenArtifacts_OverrideNative"/>
             * <lastChanges>
             * <change locator="build:2330798"/>
             * </lastChanges>
             * <properties>
             * <property name="system.versions.kotlin-native" value="1.3.50-dev-10483"/>
             * </properties>
             * </build>
             */
            val content = buildString{
                build{
                    buildType("Kotlin_dev_DeployMavenArtifacts_OverrideNative")
                    lastChanges {
                        change(buildId)
                    }
                    properties {
                        property("system.versions.kotlin-native", konanVersion)
                    }
                }
            }
            project.logger.info("pusher: content: \"$content\"")
            val res = client.post<String>(scheme = "https", host = buildServer, path = "/app/rest/buildQueue") {
                header("Authorization", "Bearer $token")
                header("Origin", "https://$buildServer")
                body = TextContent(content, ContentType.Application.Xml)
            }
            project.logger.info("pusher: result: \"$res\"")
        }
        client.close()
    }
}

internal fun StringBuilder.paired(tag:String, body:StringBuilder.()->Unit) {
    appendln("<$tag>")
    body()
    appendln("</$tag>")
}

internal fun StringBuilder.build(body:StringBuilder.()->Unit) = paired("build", body)
internal fun StringBuilder.buildType(id:String) = appendln("<buildType id=\"$id\"/>")
internal fun StringBuilder.lastChanges(body:StringBuilder.()->Unit) = paired("lastChanges", body)
internal fun StringBuilder.change(build:String) = appendln("<change locator=\"build:$build\"/>")
internal fun StringBuilder.properties(body:StringBuilder.()->Unit) = paired("properties", body)
internal fun StringBuilder.property(key:String, value:String) = appendln("<property name=\"$key\" value=\"$value\"/>")

