package com.jetbrains.konan.debugger

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.xdebugger.impl.breakpoints.LineBreakpointState
import com.intellij.xdebugger.impl.breakpoints.XBreakpointManagerImpl
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl
import com.jetbrains.cidr.execution.debugger.breakpoints.CidrBreakpointHandler
import com.jetbrains.cidr.execution.debugger.breakpoints.CidrLineBreakpointType
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import org.jetbrains.kotlin.idea.debugger.breakpoints.KotlinLineBreakpointType

private class Properties : XBreakpointProperties<Properties>() {
    override fun getState(): Properties? = null

    override fun loadState(state: Properties) {}
}

class KonanBreakpointHandler(
    private val cidrHandler: CidrBreakpointHandler,
    val project: Project
) : XBreakpointHandler<XLineBreakpoint<JavaLineBreakpointProperties>>(KotlinLineBreakpointType::class.java) {

    private var timeStamp = 0xFF_FF_FF_FFL

    data class BreakpointLocation(val fileUrl: String, val line: Int)

    private val cache = HashMap<BreakpointLocation, XLineBreakpoint<XBreakpointProperties<Any>>>()

    private fun convert(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>): XLineBreakpoint<XBreakpointProperties<Any>> {
        val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
        val breakpointType = CidrLineBreakpointType()

        val state = LineBreakpointState<Properties>(
            true, breakpointType.id, breakpoint.fileUrl, breakpoint.line, true,
            ++timeStamp, breakpointType.defaultSuspendPolicy
        )

        @Suppress("UNCHECKED_CAST") // cast check caused by parameterless generics
        return XLineBreakpointImpl(
            breakpointType as XLineBreakpointType<Properties>,
            breakpointManager as XBreakpointManagerImpl,
            null,
            state
        ) as XLineBreakpoint<XBreakpointProperties<Any>>
    }

    private fun convertWithCache(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>): XLineBreakpoint<XBreakpointProperties<Any>> {
        val breakpointLocation = BreakpointLocation(breakpoint.fileUrl, breakpoint.line)

        if (!cache.containsKey(breakpointLocation)) {
            cache[breakpointLocation] = convert(breakpoint)
        }

        return cache[breakpointLocation]!!
    }

    override fun registerBreakpoint(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>) {
        cidrHandler.registerBreakpoint(convertWithCache(breakpoint))
    }

    override fun unregisterBreakpoint(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>, temporary: Boolean) {
        cidrHandler.unregisterBreakpoint(convertWithCache(breakpoint), temporary)
    }
}