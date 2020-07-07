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
import com.jetbrains.kmm.ios.XcFileExtensions.isXcFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
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
            if (location.isXcFile()) return XcProjectFile(location)

            val candidates = location.walk()
                .maxDepth(1)
                .filter { it.isXcFile() }
                .toList()

            val file = candidates.firstOrNull { it.extension == XcFileExtensions.workspace } // workspaces are preferable
                ?: candidates.firstOrNull()

            return file?.let { XcProjectFile(it) }
        }
    }
}

internal object XcFileExtensions {
    const val project = "xcodeproj"
    const val workspace = "xcworkspace"

    internal fun File.isXcFile(): Boolean =
        extension == workspace || extension == project
}