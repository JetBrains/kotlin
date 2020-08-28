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

import com.intellij.debugger.engine.DebugProcess.JAVA_STRATUM
import com.intellij.debugger.engine.RequestHint
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.openapi.diagnostic.Logger
import com.sun.jdi.Location
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.request.StepRequest
import org.jetbrains.kotlin.idea.debugger.isOnSuspensionPoint
import org.jetbrains.kotlin.idea.debugger.safeLineNumber
import org.jetbrains.kotlin.idea.debugger.safeLocation
import org.jetbrains.kotlin.idea.debugger.safeMethod
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.Type

// Originally copied from RequestHint
class KotlinStepOverRequestHint(
    stepThread: ThreadReferenceProxyImpl,
    suspendContext: SuspendContextImpl,
    private val filter: KotlinMethodFilter
) : RequestHint(stepThread, suspendContext, StepRequest.STEP_LINE, StepRequest.STEP_OVER, filter) {
    private companion object {
        private val LOG = Logger.getInstance(KotlinStepOverRequestHint::class.java)
    }

    private class LocationData(val method: String, val signature: Type, val declaringType: String) {
        companion object {
            fun create(location: Location?): LocationData? {
                val method = location?.safeMethod() ?: return null
                val signature = Type.getMethodType(method.signature())
                return LocationData(method.name(), signature, location.declaringType().name())
            }
        }
    }

    private val startLocation = LocationData.create(suspendContext.getLocationCompat())

    override fun getNextStepDepth(context: SuspendContextImpl): Int {
        try {
            val frameProxy = context.frameProxy ?: return STOP
            if (isTheSameFrame(context)) {
                if (frameProxy.isOnSuspensionPoint()) {
                    // Coroutine will sleep now so we can't continue stepping.
                    // Let's put a run-to-cursor breakpoint and resume the debugger.
                    return if (!installCoroutineResumedBreakpoint(context)) STOP else RESUME
                }

                val location = frameProxy.safeLocation()
                val isAcceptable = location != null && filter.locationMatches(context, location)
                return if (isAcceptable) STOP else StepRequest.STEP_OVER
            } else if (isSteppedOut) {
                val location = frameProxy.safeLocation()
                val method = location?.safeMethod()
                if (method != null && method.isSyntheticMethodForDefaultParameters() && isSteppedFromDefaultParamsOriginal(location)) {
                    return StepRequest.STEP_OVER
                }

                val lineNumber = location?.safeLineNumber(JAVA_STRATUM) ?: -1
                return if (lineNumber >= 0) STOP else StepRequest.STEP_OVER
            }

            return StepRequest.STEP_OUT
        } catch (ignored: VMDisconnectedException) {
        } catch (e: EvaluateException) {
            LOG.error(e)
        }

        return STOP
    }

    private fun isSteppedFromDefaultParamsOriginal(location: Location): Boolean {
        val startLocation = this.startLocation ?: return false
        val endLocation = LocationData.create(location) ?: return false

        if (startLocation.declaringType != endLocation.declaringType) {
            return false
        }

        if (startLocation.method + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX != endLocation.method) {
            return false
        }

        val startArgs = startLocation.signature.argumentTypes
        val endArgs = endLocation.signature.argumentTypes

        if (startArgs.size >= endArgs.size) {
            // Default params function should always have at least one additional flag parameter
            return false
        }

        for ((index, type) in startArgs.withIndex()) {
            if (endArgs[index] != type) {
                return false
            }
        }

        for (index in startArgs.size until (endArgs.size - 1)) {
            if (endArgs[index].sort != Type.INT) {
                return false
            }
        }

        if (endArgs[endArgs.size - 1].descriptor != "Ljava/lang/Object;") {
            return false
        }

        return true
    }

    private fun installCoroutineResumedBreakpoint(context: SuspendContextImpl): Boolean {
        val frameProxy = context.frameProxy ?: return false
        val location = frameProxy.safeLocation() ?: return false
        val method = location.safeMethod() ?: return false

        context.debugProcess.cancelRunToCursorBreakpoint()
        return CoroutineBreakpointFacility.installCoroutineResumedBreakpoint(context, location, method)
    }
}