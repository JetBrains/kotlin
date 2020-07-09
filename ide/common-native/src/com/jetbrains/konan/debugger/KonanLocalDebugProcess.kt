package com.jetbrains.konan.debugger

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.util.PathUtil
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.frame.XExecutionStack
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess
import com.jetbrains.cidr.execution.debugger.CidrSuspensionCause
import com.jetbrains.cidr.execution.debugger.backend.LLFrame
import com.jetbrains.cidr.execution.debugger.backend.LLThread


open class KonanLocalDebugProcess(
    parameters: RunParameters,
    session: XDebugSession,
    consoleBuilder: TextConsoleBuilder,
    backendFilterProvider: ConsoleFilterProvider
) : CidrLocalDebugProcess(parameters, session, consoleBuilder, backendFilterProvider) {
    private val sourcesIndex = HashMap<String, VirtualFile?>()

    override fun isLibraryFrameFilterSupported(): Boolean = true

    override fun newExecutionStack(
        thread: LLThread,
        frame: LLFrame?,
        current: Boolean,
        cause: CidrSuspensionCause?
    ): XExecutionStack {
        return KonanExecutionStack(thread, frame, cause, this)
    }

    override fun getBreakpointHandlers() = addKotlinHandler(super.getBreakpointHandlers(), session.project)

    fun resolveFile(originalFullName: String): VirtualFile? {
        synchronized(sourcesIndex) {
            if (sourcesIndex.containsKey(originalFullName)) return sourcesIndex[originalFullName]
        }

        val originalName = PathUtil.getFileName(originalFullName)

        val files = ReadAction.compute<Collection<VirtualFile>, RuntimeException> {
            FilenameIndex.getVirtualFilesByName(project, originalName, ProjectScope.getLibrariesScope(project))
        }

        val selected = if (files.size > 1) bestCandidate(originalFullName, files) else files.firstOrNull()

        synchronized(sourcesIndex) {
            sourcesIndex[originalFullName] = selected
        }

        return selected
    }

    private fun bestCandidate(name: String, files: Collection<VirtualFile>): VirtualFile? {
        val maxSuffix = files.map { it.path.commonSuffixWith(name).length }.toIntArray().max()
        val goodMatches = files.filter { it.path.commonSuffixWith(name).length == maxSuffix }

        if (goodMatches.size == 1) { // most solid guess
            return goodMatches.first()
        }

        if (name.contains("backend.native")) {
            return goodMatches.firstOrNull { it.path.contains("kotlin-stdlib-common-sources") }
        }

        if (name.contains("runtime")) {
            return goodMatches.firstOrNull { it.path.contains("kotlin-stdlib-native-sources") }
        }

        return null // can not distinguish
    }
}