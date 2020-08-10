/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.concurrency.AppExecutorUtil
import com.jetbrains.kmm.ios.XcFileExtensions.isXcFile
import com.jetbrains.konan.KonanBundle
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

        private const val PROPERTY_NAME = "xcodeproj"
        val gradleProperty get() = KonanBundle.message("property.$PROPERTY_NAME")

        fun findXcProjectFile(location: File): XcProjectFile? =
            findXcFile(location, false)?.let { XcProjectFile(it) }

        fun findXcFile(location: File, takeSubDir: Boolean = true): File? {
            if (location.isXcFile()) return location

            val candidates = location.walk()
                .maxDepth(if (takeSubDir) 2 else 1)
                .filter { it.isXcFile() }
                .toList()

            return candidates.firstOrNull { it.extension == XcFileExtensions.workspace } // workspaces are preferable
                ?: candidates.firstOrNull()
        }

        fun setupXcProjectPath(project: Project, xcFile: File) {
            if (!xcFile.isXcFile()) error("$xcFile is not XcFile!")
            val propFile = File(project.basePath, "gradle.properties")
            LocalFileSystem.getInstance().findFileByIoFile(propFile)?.let { vf ->
                WriteCommandAction.runWriteCommandAction(project) {
                    val text = VfsUtilCore.loadText(vf) + "\n$PROPERTY_NAME=${xcFile.relativeTo(propFile.parentFile)}"
                    VfsUtil.saveText(vf, text)
                }
            }
        }
    }
}

internal object XcFileExtensions {
    const val project = "xcodeproj"
    const val workspace = "xcworkspace"

    internal fun File.isXcFile(): Boolean =
        extension == workspace || extension == project
}