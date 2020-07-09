/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KotlinGradleCoroutineDebugProjectResolver : AbstractProjectResolverExtension() {
    val log = Logger.getInstance(this::class.java)

    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        try {
            val disableCoroutineAgent = KotlinDebuggerSettings.getInstance().debugDisableCoroutineAgent
            if (!disableCoroutineAgent)
                setupCoroutineAgentForJvmForkedTestTasks(initScriptConsumer)
        } catch (e: Exception) {
            log.error("Gradle: not possible to attach a coroutine debugger agent.", e)
        }
    }

    private fun setupCoroutineAgentForJvmForkedTestTasks(initScriptConsumer: Consumer<String>) {
        val script =
            //language=Gradle
            """
            gradle.taskGraph.beforeTask { Task task ->
                if (task instanceof Test) {
                    def kotlinxCoroutinesCoreJar = task.classpath.find { it.name.startsWith("kotlinx-coroutines-core") }
                    if (kotlinxCoroutinesCoreJar) {
                        def results = (kotlinxCoroutinesCoreJar.getName() =~ /kotlinx-coroutines-core\-([\d\.]+)\.jar${'$'}/).findAll()
                        if (results) {
                            def version = results.first()[1]
                            if (org.gradle.util.VersionNumber.parse( version ) >= org.gradle.util.VersionNumber.parse('1.3.8')) {
                                task.jvmArgs ("-javaagent:${'$'}{kotlinxCoroutinesCoreJar?.absolutePath}", "-ea")
                                return
                            }
                        }
                    }
                }
            }
            """.trimIndent()
        initScriptConsumer.consume(script)
    }
}