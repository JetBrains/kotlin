/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
                        routing {
                            get("/") {
                                call.respondHtml {
                                    head {
                                        title("Hello from Ktor!")
                                    }
                                    body {
                                        +"${'$'}{hello()} from Ktor. Check me value: ${'$'}{Sample().checkMe()}"
                                        div {
                                            id = "js-response"
                                            +"Loading..."
                                        }
                                        script(src = "/static/$name.js") {}
                                    }
                                }
                            }
                            static("/static") {
                                resource("$name.js")
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
                
                fun main() {
                    document.addEventListener("DOMContentLoaded", {
                        helloWorld("Hi!")
                    })
                }                
            """.trimIndent()
            )

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
            listOf(commonMain, commonTest, jvmMain, jvmTest, jsMain, jsTest, logBack).forEach(BufferedWriter::close)
        }
    }

    override fun buildMultiPlatformPart(): String {
        return """
            def ktor_version = '1.1.3'
            def logback_version = '1.2.3'

            kotlin {
                jvm()
                js {
                    browser {
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

            jvmJar {
                dependsOn(jsBrowserWebpack)
                from(new File(jsBrowserWebpack.entry.name, jsBrowserWebpack.outputPath))
            }
            
            task run(type: JavaExec, dependsOn: [jvmJar]) {
                group = "application"
                main = "sample.SampleJvmKt"
                classpath(configurations.jvmRuntimeClasspath, jvmJar)
                args = []
            }
        """.trimIndent()
    }
}