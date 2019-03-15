/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import java.io.BufferedWriter

class KotlinGradleWebMultiplatformModuleBuilder : KotlinGradleAbstractMultiplatformModuleBuilder() {

    private val commonName: String = "common"
    private var jvmTargetName: String = "jvm"
    private var jsTargetName: String = "js"

    private val commonSourceName get() = "$commonName$productionSuffix"
    private val commonTestName get() = "$commonName$testSuffix"
    private val jvmSourceName get() = "$jvmTargetName$productionSuffix"
    private val jvmTestName get() = "$jvmTargetName$testSuffix"
    private val jsSourceName get() = "$jsTargetName$productionSuffix"
    private val jsTestName get() = "$jsTargetName$testSuffix"

    override fun getBuilderId() = "kotlin.gradle.multiplatform.web"

    override fun getPresentableName() = "JS Client and JVM Server | Gradle"

    override fun getDescription() =
        "Multiplatform Gradle project allowing reuse of the same Kotlin code between JS Client and JVM Server"

    override fun BuildScriptDataBuilder.setupAdditionalDependencies() {
        addBuildscriptRepositoriesDefinition("jcenter()")
        addRepositoriesDefinition("maven { url \"https://dl.bintray.com/kotlin/ktor\" }")
        addRepositoriesDefinition("jcenter()")
    }

    override fun createProjectSkeleton(rootDir: VirtualFile) {
        val src = rootDir.createChildDirectory(this, "src")

        val commonMain = src.createKotlinSampleFileWriter(commonSourceName)
        val commonTest = src.createKotlinSampleFileWriter(commonTestName, fileName = "SampleTests.kt")
        val jvmMain = src.createKotlinSampleFileWriter(jvmSourceName, jvmTargetName)
        val jvmTest = src.createKotlinSampleFileWriter(jvmTestName, fileName = "SampleTestsJVM.kt")
        val jsMain = src.createKotlinSampleFileWriter(jsSourceName, jsTargetName)
        val jsTest = src.createKotlinSampleFileWriter(jsTestName, fileName = "SampleTestsJS.kt")

        val jvmRoot = src.findChild(jvmSourceName)!!
        val jvmResources = jvmRoot.createChildDirectory(this, "resources")
        val logBack = jvmResources.createChildData(this, "logback.xml").bufferedWriter()

        val jsRoot = src.findChild(jsSourceName)!!
        val jsResources = jsRoot.createChildDirectory(this, "resources")
        val requireMinJs = jsResources.createChildData(this, "require.min.js").bufferedWriter()

        try {
            commonMain.write(
                """
                package sample

                expect class Sample() {
                    fun checkMe(): Int
                }

                expect object Platform {
                    val name: String
                }

                fun hello(): String = "Hello from ${"$"}{Platform.name}"
            """.trimIndent()
            )

            jvmMain.write(
                """
                package sample

                import io.ktor.application.*
                import io.ktor.html.*
                import io.ktor.http.content.*
                import io.ktor.routing.*
                import io.ktor.server.engine.*
                import io.ktor.server.netty.*
                import kotlinx.html.*
                import java.io.*

                actual class Sample {
                    actual fun checkMe() = 42
                }

                actual object Platform {
                    actual val name: String = "JVM"
                }

                fun main() {
                    embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
                        val currentDir = File(".").absoluteFile
                        environment.log.info("Current directory: ${"$"}currentDir")

                        val webDir = listOf(
                            "web",
                            "../src/jsMain/web",
                            "src/jsMain/web"
                        ).map {
                            File(currentDir, it)
                        }.firstOrNull { it.isDirectory }?.absoluteFile ?: error("Can't find 'web' folder for this sample")

                        environment.log.info("Web directory: ${"$"}webDir")

                        routing {
                            get("/") {
                                call.respondHtml {
                                    head {
                                        title("Hello from Ktor!")
                                    }
                                    body {
                                        +"${"$"}{hello()} from Ktor. Check me value: ${"$"}{Sample().checkMe()}"
                                        div {
                                            id = "js-response"
                                            +"Loading..."
                                        }
                                        script(src = "/static/require.min.js") {
                                        }
                                        script {
                                            +"require.config({baseUrl: '/static'});\n"
                                            +"require(['/static/$name.js'], function(js) { js.sample.helloWorld('Hi'); });\n"
                                        }
                                    }
                                }
                            }
                            static("/static") {
                                files(webDir)
                            }
                        }
                    }.start(wait = true)
                }
            """.trimIndent()
            )

            logBack.write(
                """
                <configuration>
                    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                        <encoder>
                            <pattern>%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
                        </encoder>
                    </appender>

                    <root level="INFO">
                        <appender-ref ref="STDOUT"/>
                    </root>
                </configuration>
                """.trimIndent()
            )

            jsMain.write(
                """
                package sample

                import kotlin.browser.*

                actual class Sample {
                    actual fun checkMe() = 12
                }

                actual object Platform {
                    actual val name: String = "JS"
                }


                @Suppress("unused")
                @JsName("helloWorld")
                fun helloWorld(salutation: String) {
                    val message = "${"$"}salutation from Kotlin.JS ${"$"}{hello()}, check me value: ${"$"}{Sample().checkMe()}"
                    document.getElementById("js-response")?.textContent = message
                }
            """.trimIndent()
            )

            requireMinJs.write(requireMinJsContent)

            commonTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTests {
                    @Test
                    fun testMe() {
                        assertTrue(Sample().checkMe() > 0)
                    }
                }
            """.trimIndent()
            )

            jvmTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTestsJVM {
                    @Test
                    fun testHello() {
                        assertTrue("JVM" in hello())
                    }
                }
            """.trimIndent()
            )

            jsTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTestsJS {
                    @Test
                    fun testHello() {
                        assertTrue("JS" in hello())
                    }
                }
            """.trimIndent()
            )
        } finally {
            listOf(commonMain, commonTest, jvmMain, jvmTest, jsMain, jsTest, logBack, requireMinJs).forEach(BufferedWriter::close)
        }
    }

    override fun buildMultiPlatformPart(): String {
        return """
            def ktor_version = '1.1.3'
            def logback_version = '1.2.3'

            kotlin {
                jvm()
                js() {
                    compilations.all {
                        kotlinOptions {
                            languageVersion = "1.3"
                            moduleKind = "umd"
                            sourceMap = true
                            metaInfo = true
                        }
                    }
                }
                sourceSets {
                    $commonSourceName {
                        dependencies {
                            implementation kotlin('stdlib-common')
                        }
                    }
                    $commonTestName {
                        dependencies {
                            implementation kotlin('test-common')
                            implementation kotlin('test-annotations-common')
                        }
                    }
                    $jvmSourceName {
                        dependencies {
                            implementation kotlin('stdlib-jdk8')
                            implementation "io.ktor:ktor-server-netty:${"$"}ktor_version"
                            implementation "io.ktor:ktor-html-builder:${"$"}ktor_version"
                            implementation "ch.qos.logback:logback-classic:${"$"}logback_version"
                        }
                    }
                    $jvmTestName {
                        dependencies {
                            implementation kotlin('test')
                            implementation kotlin('test-junit')
                        }
                    }
                    $jsSourceName {
                        dependencies {
                            implementation kotlin('stdlib-js')
                        }
                    }
                    $jsTestName {
                        dependencies {
                            implementation kotlin('test-js')
                        }
                    }
                }
            }

            def webFolder = new File(project.buildDir, "../src/jsMain/web")
            def jsCompilations = kotlin.targets.js.compilations

            task populateWebFolder(dependsOn: [jsMainClasses]) {
                doLast {
                    copy {
                        from jsCompilations.main.output
                        from kotlin.sourceSets.jsMain.resources.srcDirs
                        jsCompilations.test.runtimeDependencyFiles.each {
                            if (it.exists() && !it.isDirectory()) {
                                from zipTree(it.absolutePath).matching { include '*.js' }
                            }
                        }
                        into webFolder
                    }
                }
            }

            jsJar.dependsOn(populateWebFolder)

            task run(type: JavaExec, dependsOn: [jvmMainClasses, jsJar]) {
                main = "sample.Sample${jvmTargetName.capitalize()}Kt"
                classpath { [
                        kotlin.targets.jvm.compilations.main.output.allOutputs.files,
                        configurations.jvmRuntimeClasspath,
                ] }
                args = []
            }
        """.trimIndent()
    }
}