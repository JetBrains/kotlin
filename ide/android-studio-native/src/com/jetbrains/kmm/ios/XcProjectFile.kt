/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class XcProjectFile(
    private val file: File
) {
    val absolutePath: String get() = file.absolutePath
    val projectName: String get() = file.nameWithoutExtension
    val selector: String
        get() = if (absolutePath.endsWith(XcFileExtensions.workspace))
            "-workspace"
        else
            "-project"

    var schemesStatus = "Xcode schemes not initialized"
        private set
    private val schemesLock = ReentrantLock()
    var schemes: List<String> = emptyList()
        get() = schemesLock.withLock {
            return field
        }
        private set(value) = schemesLock.withLock {
            field = value
        }

    init {
        loadSchemes(DISPATCHER)
    }

    private fun loadSchemes(dispatcher: CoroutineDispatcher) {
        GlobalScope.launch(dispatcher) {
            val schemeGrabCommand = GeneralCommandLine().apply {
                workDirectory = file.parentFile
                exePath = "/usr/bin/xcodebuild"
                addParameters(selector, absolutePath)
                addParameter("-list")
            }
            val process = CapturingProcessHandler(schemeGrabCommand)
            val schemeCollector = SchemeCollector()

            process.addProcessListener(schemeCollector)

            if (process.runProcess().exitCode != 0 || schemeCollector.schemes.isEmpty()) {
                schemesStatus = "can't grab Xcode schemes with " + schemeGrabCommand.commandLineString
            } else {
                schemesStatus = "Xcode schemes were successful loaded"
                schemes = schemeCollector.schemes
            }
        }
    }

    private class SchemeCollector : ProcessAdapter() {
        enum class State { BEFORE, ACCEPTING, AFTER }

        private var state = State.BEFORE

        val schemes = mutableListOf<String>()

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            val line = event.text.trim()

            when (state) {
                State.BEFORE -> {
                    if (line == "Schemes:") {
                        state = State.ACCEPTING
                    }
                }

                State.ACCEPTING -> {
                    if (line.isNotEmpty()) {
                        schemes.add(line)
                    } else {
                        state = State.AFTER
                    }
                }
            }
        }
    }

    companion object {
        private val DISPATCHER = AppExecutorUtil.createBoundedApplicationPoolExecutor(javaClass.simpleName, 1).asCoroutineDispatcher()

        fun findXcProjectFile(location: File): XcProjectFile? {
            val candidates = mutableListOf<File>()

            if (location.isFile) {
                candidates.add(location)
            } else if (location.isDirectory) {
                for (file in (location.listFiles() ?: emptyArray())) {
                    when (file.extension) {
                        XcFileExtensions.workspace -> candidates.add(0, file) // workspaces are preferable
                        XcFileExtensions.project -> candidates.add(file)
                    }
                }
            }

            return candidates.firstOrNull()?.let { XcProjectFile(it) }
        }
    }
}

internal object XcFileExtensions {
    const val project = "xcodeproj"
    const val workspace = "xcworkspace"
}