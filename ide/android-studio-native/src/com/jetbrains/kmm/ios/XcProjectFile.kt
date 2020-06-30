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

class XcProjectFile(file: File, coroutineDispatcher: ExecutorCoroutineDispatcher) {

    init {
        GlobalScope.async(coroutineDispatcher) {
            val process = CapturingProcessHandler(schemeGrabCommand)
            val schemeCollector = SchemeCollector()

            process.addProcessListener(schemeCollector)

            if (process.runProcess().exitCode != 0 || schemeCollector.schemes.isEmpty()) {
                schemesStatus = "can't grab Xcode schemes with " + schemeGrabCommand.commandLineString
                return@async
            }

            schemes = schemeCollector.schemes
        }
    }

    val absolutePath: String = file.absolutePath

    val projectName: String = file.nameWithoutExtension

    val selector: String
        get() = if (absolutePath.endsWith(XcFileExtensions.workspace))
            "-workspace"
        else
            "-project"

    private val schemeGrabCommand = GeneralCommandLine().also {
        it.workDirectory = file.parentFile
        it.exePath = "/usr/bin/xcodebuild"
        it.addParameters(selector, absolutePath)
        it.addParameter("-list")
    }

    private val schemesLock = ReentrantLock()
    var schemes: List<String> = emptyList()
        get() = schemesLock.withLock {
            return field
        }
        private set(value) = schemesLock.withLock {
            field = value
        }


    var schemesStatus = ""

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

            return if (candidates.isEmpty()) null else XcProjectFile(
                candidates.first(),
                DISPATCHER
            )
        }
    }
}