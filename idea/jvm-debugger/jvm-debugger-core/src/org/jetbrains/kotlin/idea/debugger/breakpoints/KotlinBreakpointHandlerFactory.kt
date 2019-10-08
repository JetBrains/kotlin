/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaBreakpointHandler
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory

class KotlinFieldBreakpointHandlerFactory : JavaBreakpointHandlerFactory {
    override fun createHandler(process: DebugProcessImpl): JavaBreakpointHandler? {
        return KotlinFieldBreakpointHandler(process)
    }
}

class KotlinLineBreakpointHandlerFactory : JavaBreakpointHandlerFactory {
    override fun createHandler(process: DebugProcessImpl): JavaBreakpointHandler? {
        return KotlinLineBreakpointHandler(process)
    }
}

class KotlinFunctionBreakpointHandlerFactory : JavaBreakpointHandlerFactory {
    override fun createHandler(process: DebugProcessImpl): JavaBreakpointHandler? {
        return KotlinFunctionBreakpointHandler(process)
    }
}

class KotlinFieldBreakpointHandler(process: DebugProcessImpl) : JavaBreakpointHandler(KotlinFieldBreakpointType::class.java, process)
class KotlinLineBreakpointHandler(process: DebugProcessImpl) : JavaBreakpointHandler(KotlinLineBreakpointType::class.java, process)
class KotlinFunctionBreakpointHandler(process: DebugProcessImpl) : JavaBreakpointHandler(KotlinFunctionBreakpointType::class.java, process)