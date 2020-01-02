/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Consumer
import com.intellij.util.SystemProperties
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KotlinGradleCoroutineDebugProjectResolver : AbstractProjectResolverExtension() {
    val log = Logger.getInstance(this::class.java)

    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        try {
            if (coroutineDebuggerEnabled())
                setupCoroutineAgentForJvmForkedTestTasks(initScriptConsumer)
        } catch (e: Exception) {
            log.error("Gradle: not possible to attach coroutine debugger agent. Coroutine debugger disabled.", e)
        }
    }

    private fun setupCoroutineAgentForJvmForkedTestTasks(initScriptConsumer: Consumer<String>) {
        val lines = arrayOf(
            "gradle.taskGraph.beforeTask { Task task ->",
            "  if (task instanceof Test) {",
            "    def kotlinxCoroutinesDebugJar = task.classpath.find { it.name.contains(\"kotlinx-coroutines-debug\") }",
            "    if (kotlinxCoroutinesDebugJar)",
            "        task.jvmArgs (\"-javaagent:\${kotlinxCoroutinesDebugJar?.absolutePath}\", \"-ea\")",
            "  }",
            "}"
        )
        val script = StringUtil.join(lines, SystemProperties.getLineSeparator())
        initScriptConsumer.consume(script)
    }

    private fun coroutineDebuggerEnabled() = Registry.`is`("kotlin.debugger.coroutines")
}