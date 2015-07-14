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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.engine.BasicStepMethodFilter
import com.intellij.debugger.engine.DebugProcessImpl
import com.sun.jdi.Location
import org.jetbrains.kotlin.idea.debugger.MockSourcePosition
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.JetFile

public class KotlinBasicStepMethodFilter(
        val stepTarget: KotlinMethodSmartStepTarget
): BasicStepMethodFilter(stepTarget.getMethod(), stepTarget.getCallingExpressionLines()) {
    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        if (super.locationMatches(process, location)) return true

        val containingFile = runReadAction { stepTarget.resolvedElement.getContainingFile() }
        if (containingFile !is JetFile) return false

        val positionManager = process.getPositionManager() ?: return false

        val classes = positionManager.getAllClasses(MockSourcePosition(_file = containingFile, _elementAt = stepTarget.resolvedElement))

        val method = location.method()
        return stepTarget.getMethod().getName() == method.name() &&
               myTargetMethodSignature?.getName(process) == method.signature() &&
               classes.contains(location.declaringType())
    }
}