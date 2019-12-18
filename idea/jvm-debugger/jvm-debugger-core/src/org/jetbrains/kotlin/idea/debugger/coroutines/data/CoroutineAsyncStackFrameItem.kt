/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.data

import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.xdebugger.frame.XNamedValue

class CoroutineAsyncStackFrameItem(
    val location: GeneratedLocation,
    spilledVariables: List<XNamedValue>
) : StackFrameItem(location, spilledVariables)