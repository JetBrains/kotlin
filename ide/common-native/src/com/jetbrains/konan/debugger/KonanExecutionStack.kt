package com.jetbrains.konan.debugger

import com.intellij.execution.ExecutionException
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.frame.XStackFrameContainerEx
import com.intellij.xdebugger.settings.XDebuggerSettingsManager
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.CidrDebuggerUtil
import com.jetbrains.cidr.execution.debugger.CidrSuspensionCause
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.LLFrame
import com.jetbrains.cidr.execution.debugger.backend.LLThread
import com.jetbrains.konan.KonanLog
import java.util.ArrayList

// If we are collecting the first bunch of frames, we should return as quickly as possible to
// give way for other activities like variables collection.
// On the other hand, we better have top-most frame with code to select it in the the frames list.
// So 100 is a reasonable bunch size and also a limit for looking for a frame with code.
// Note though, that we still might not have source file for first frame with code, in this case
// we'll try to read the rest of the batch (see below)
const val BATCH_SIZE = 100

// FIRST_BATCH_LOOKUP -> [FIRST_BATCH_FOLLOWUP ->] -> REGULAR_BATCH -> ... -> REGULAR_BATCH -> ALL_DONE
private enum class ComputationState {
    FIRST_BATCH_LOOKUP,
    FIRST_BATCH_FOLLOWUP,
    REGULAR_BATCH,
    ALL_DONE
}

internal class KonanExecutionStack(
    private val thread: LLThread,
    frame: LLFrame?,
    private val suspensionCause: CidrSuspensionCause?,
    val process: KonanLocalDebugProcess
) : XExecutionStack(thread.displayName) {
    private var topFrame: KonanStackFrame? = if (frame == null) null else newFrame(frame)
    private var selectedFrame: KonanStackFrame? = null
    private var state = ComputationState.FIRST_BATCH_LOOKUP

    private val cachedFrames = ArrayList<KonanStackFrame>()
    private var cachedFirstFrameIndex: Int? = null

    private var hasMoreFrames: Boolean = true
    private var currentFrom: Int = 0
    private var currentCount: Int = 0

    override fun getTopFrame(): XStackFrame? = topFrame

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        process.postCommand(object : CidrDebugProcess.DebuggerUIUpdateCommand {
            override fun run(driver: DebuggerDriver) {
                doComputeStackFrames(driver, firstFrameIndex, container)
            }
        })
    }

    private fun doComputeStackFrames(driver: DebuggerDriver, firstFrameIndex: Int, container: XStackFrameContainer) {
        synchronized(this) {
            try {
                if (firstFrameIndex == cachedFirstFrameIndex) {
                    filterAndDeliver(cachedFrames, container)
                    return
                }

                currentFrom = firstFrameIndex
                collectFramesIteratively(driver, container)
                if (state == ComputationState.ALL_DONE) cachedFirstFrameIndex = firstFrameIndex
            } catch (e: DebuggerCommandException) {
                container.errorOccurred(e.message!!)
            } catch (e: ExecutionException) {
                container.errorOccurred(CidrDebuggerUtil.getExceptionMessage(e))
                throw e
            }
        }
    }

    private fun filterAndDeliver(frames: List<KonanStackFrame>, container: XStackFrameContainer) {
        if (container.isObsolete) return
        val toDeliver = if (XDebuggerSettingsManager.getInstance().dataViewSettings.isShowLibraryStackFrames)
            frames
        else
            frames.filter { !it.isLibraryFile() || it == topFrame }

        if (container is XStackFrameContainerEx) {
            container.addStackFrames(toDeliver, selectedFrame, !hasMoreFrames)
        } else {
            KonanLog.LOG.error("Expected instanceof XStackFrameContainerEx, got " + container.javaClass)
            container.addStackFrames(toDeliver, !hasMoreFrames)
        }
    }

    private fun collectFramesIteratively(driver: DebuggerDriver, container: XStackFrameContainer) {
        state = if (currentFrom == 0) ComputationState.FIRST_BATCH_LOOKUP else ComputationState.REGULAR_BATCH
        currentCount = BATCH_SIZE
        selectedFrame = null
        cachedFrames.clear()

        while (state != ComputationState.ALL_DONE) {
            if (container.isObsolete) return
            val batch = nextBatchOfFrames(driver)
            cachedFrames.addAll(batch)

            if (state != ComputationState.REGULAR_BATCH) {
                updateSelectedFrame()
            }

            // we might have no source file for a frame with debug info (untilFirstLineWithCode = true)
            // let's read rest of the first hundred to see if we have one with available source file.
            if (state == ComputationState.FIRST_BATCH_LOOKUP) {
                if (selectedFrame == null && hasMoreFrames && currentCount > 0) {
                    state = ComputationState.FIRST_BATCH_FOLLOWUP
                    // do not send first part of the first batch into container to avoid subsequent redraw
                    // better will take it from cachedFrames next step
                    continue
                }
            }

            filterAndDeliver(if (state == ComputationState.FIRST_BATCH_FOLLOWUP) cachedFrames else batch, container)

            currentCount = BATCH_SIZE
            state = if (hasMoreFrames) ComputationState.REGULAR_BATCH else ComputationState.ALL_DONE
        }
    }

    private fun newFrame(frame: LLFrame): KonanStackFrame {
        return KonanStackFrame(thread, frame, suspensionCause, process)
    }

    private fun nextBatchOfFrames(driver: DebuggerDriver): ArrayList<KonanStackFrame> {
        val llFrames = driver.getFrames(thread.id, currentFrom, currentCount, state == ComputationState.FIRST_BATCH_LOOKUP)
        hasMoreFrames = llFrames.hasMore
        currentFrom += llFrames.list.size
        currentCount -= llFrames.list.size

        val result = ArrayList<KonanStackFrame>(llFrames.list.size)
        llFrames.list.forEach { result.add(newFrame(it)) }

        if (result.isNotEmpty() && state == ComputationState.FIRST_BATCH_LOOKUP) {
            topFrame = topFrame ?: result[0]
            result[0] = topFrame!!
        }

        return result
    }

    private fun updateSelectedFrame() {
        if (selectedFrame != null) return
        selectedFrame = if (topFrame?.hasSourceFile() == true) topFrame else cachedFrames.firstOrNull { it.hasSourceFile() }
    }
}