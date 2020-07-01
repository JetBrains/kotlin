package com.jetbrains.konan.debugger

import com.intellij.icons.AllIcons.Debugger
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.roots.ProjectRootUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.jetbrains.cidr.execution.debugger.CidrStackFrame
import com.jetbrains.cidr.execution.debugger.CidrSuspensionCause
import com.jetbrains.cidr.execution.debugger.backend.LLFrame
import com.jetbrains.cidr.execution.debugger.backend.LLThread
import com.jetbrains.konan.KonanLog

private const val KONAN_PREFIX = "kfun:"

private enum class PositionType {
    PROJECT_FILE,
    LIBRARY_FILE,
    DISASSEMBLED
}

private class TypedPosition(val sourcePosition: XSourcePosition?, val type: PositionType)

private operator fun PositionType.invoke(position: XSourcePosition?) = TypedPosition(position, this)

class KonanStackFrame(
    thread: LLThread,
    frame: LLFrame,
    suspensionCause: CidrSuspensionCause?,
    private val debugProcess: KonanLocalDebugProcess
) : CidrStackFrame(debugProcess, thread, frame, suspensionCause) {
    private val typedPosition: TypedPosition by lazy { doGetSourcePosition() }

    override fun getSourcePosition(): XSourcePosition? = typedPosition.sourcePosition

    override fun hasSourceFile(): Boolean = typedPosition.type != PositionType.DISASSEMBLED

    override fun customizePresentation(component: ColoredTextContainer) {
        component.setIcon(Debugger.Frame)
        if (typedPosition.type != PositionType.DISASSEMBLED) {
            component.append(functionName(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.append(
                " ${typedPosition.sourcePosition!!.file.name}:${typedPosition.sourcePosition!!.line + 1}",
                SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES
            )
        } else {
            component.append(functionName(), SimpleTextAttributes.GRAYED_ATTRIBUTES)
            component.append(" " + frame.programCounter, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
        }
    }

    private fun disasmPosition() = debugProcess.createDisasmPosition(frame.programCounter)

    private fun doGetSourcePosition(): TypedPosition {
        if (frame.file == null) {
            return PositionType.DISASSEMBLED(disasmPosition())
        }

        resolveLocalFile(frame.file!!)?.let {
            val position = XDebuggerUtil.getInstance().createPosition(it, frame.line)
            return PositionType.PROJECT_FILE(position)
        }

        debugProcess.resolveFile(frame.file!!)?.let {
            val position = XDebuggerUtil.getInstance().createPosition(it, frame.line)
            return PositionType.LIBRARY_FILE(position)
        }

        return PositionType.DISASSEMBLED(disasmPosition())
    }

    private fun resolveLocalFile(name: String): VirtualFile? {
        return ReadAction.compute<VirtualFile, RuntimeException> {
            val vFile = LocalFileSystem.getInstance().findFileByPath(name) ?: return@compute null
            val resolvedFile = ProjectRootUtil.findSymlinkedFileInContent(process.project, vFile)
            if (KonanLog.LOG.isTraceEnabled) {
                if (vFile != resolvedFile) {
                    KonanLog.LOG.trace("Debugger path resolved: $vFile -> $resolvedFile")
                }
            }
            resolvedFile
        }
    }

    fun isLibraryFile(): Boolean = typedPosition.type != PositionType.PROJECT_FILE

    private fun functionName(): String {
        val unmangledName = frame.function
        if (!unmangledName.startsWith(KONAN_PREFIX)) return unmangledName
        if (!unmangledName.contains('(')) return unmangledName.removePrefix(KONAN_PREFIX)
        val fullName = unmangledName.substring(KONAN_PREFIX.length, unmangledName.indexOf('('))
        return fullName.substring(fullName.lastIndexOf('.') + 1)
    }
}