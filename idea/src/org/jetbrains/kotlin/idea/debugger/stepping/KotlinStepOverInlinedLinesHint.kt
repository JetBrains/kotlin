/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.jdi.StackFrameProxy
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Computable
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.request.StepRequest

// Originally copied from RequestHint
class KotlinStepOverInlinedLinesHint(
        stepThread: ThreadReferenceProxyImpl,
        suspendContext: SuspendContextImpl,
        methodFilter: KotlinMethodFilter) : RequestHint(stepThread, suspendContext, methodFilter) {

    private val LOG = Logger.getInstance(KotlinStepOverInlinedLinesHint::class.java)

    private val filter = methodFilter

    override fun getDepth(): Int = StepRequest.STEP_OVER

    override fun getNextStepDepth(context: SuspendContextImpl): Int {
        try {
            val frameProxy = context.frameProxy
            if (frameProxy != null) {
                if (isTheSameFrame(context)) {
                    return if (filter.locationMatches(context, frameProxy.location())) {
                        STOP
                    }
                    else {
                        StepRequest.STEP_OVER
                    }
                }

                if (isSteppedOut) {
                    return STOP
                }

                return StepRequest.STEP_OUT
            }
        }
        catch (ignored: VMDisconnectedException) {
        }
        catch (e: EvaluateException) {
            LOG.error(e)
        }

        return STOP
    }
}