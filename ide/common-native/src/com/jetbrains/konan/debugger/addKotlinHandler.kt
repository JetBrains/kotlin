/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan.debugger

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.jetbrains.cidr.execution.debugger.breakpoints.CidrBreakpointHandler

fun addKotlinHandler(cidrHandlers: Array<XBreakpointHandler<*>>, project: Project): Array<XBreakpointHandler<*>> {
    val handlers = mutableListOf(*cidrHandlers)

    cidrHandlers.firstOrNull { it is CidrBreakpointHandler }?.let {
        handlers.add(KonanBreakpointHandler(it as CidrBreakpointHandler, project))
    }

    return handlers.toTypedArray()
}
