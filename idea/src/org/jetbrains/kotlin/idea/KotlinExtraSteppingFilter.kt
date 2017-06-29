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

package org.jetbrains.kotlin.idea

import com.intellij.debugger.NoDataException
import com.intellij.debugger.engine.ExtraSteppingFilter
import com.intellij.debugger.engine.SuspendContext
import com.intellij.debugger.settings.DebuggerSettings
import com.sun.jdi.Location
import com.sun.jdi.request.StepRequest
import org.jetbrains.kotlin.idea.debugger.KotlinPositionManager
import org.jetbrains.kotlin.idea.debugger.isOnSuspendReturnOrReenter
import org.jetbrains.kotlin.idea.debugger.isOneLineMethod
import org.jetbrains.kotlin.idea.util.application.runReadAction

class KotlinExtraSteppingFilter : ExtraSteppingFilter {
    override fun isApplicable(context: SuspendContext?): Boolean {
        if (context == null) {
            return false
        }

        val debugProcess = context.debugProcess ?: return false
        val positionManager = KotlinPositionManager(debugProcess)
        val location = context.frameProxy?.location() ?: return false
        return runReadAction {
            shouldFilter(positionManager, location)
        }
    }


    private fun shouldFilter(positionManager: KotlinPositionManager, location: Location): Boolean {
        val defaultStrata = location.declaringType()?.defaultStratum()
        if ("Kotlin" != defaultStrata) {
            return false
        }

        val sourcePosition =
                try {
                    positionManager.getSourcePosition(location)
                }
                catch(e: NoDataException) {
                    return false
                } ?: return false

        if (isOnSuspendReturnOrReenter(location) && !isOneLineMethod(location)) {
            return true
        }

        val settings = DebuggerSettings.getInstance()
        if (settings.TRACING_FILTERS_ENABLED) {
            val classNames = positionManager.originalClassNamesForPosition(sourcePosition).map { it.replace('/', '.') }
            if (classNames.isEmpty()) {
                return false
            }

            for (className in classNames) {
                for (filter in settings.steppingFilters) {
                    if (filter.isEnabled) {
                        if (filter.matches(className)) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    override fun getStepRequestDepth(context: SuspendContext?): Int {
        return StepRequest.STEP_INTO
    }
}

