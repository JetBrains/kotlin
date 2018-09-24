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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import org.jetbrains.kotlin.codegen.inline.isFakeLocalVariableForInline

class KotlinStackFrame(frame: StackFrameProxyImpl) : JavaStackFrame(StackFrameDescriptorImpl(frame, MethodsTracker()), true) {
    override fun getVisibleVariables(): List<LocalVariableProxyImpl>? {
        return super.getVisibleVariables().filter {
            !isFakeLocalVariableForInline(it.name())
        }
    }
}