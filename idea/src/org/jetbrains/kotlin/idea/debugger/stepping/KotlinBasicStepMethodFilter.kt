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

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.NamedMethodFilter
import com.intellij.util.Range
import com.sun.jdi.Location
import com.sun.jdi.Method
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.JetConstructor
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetPropertyAccessor

public class KotlinBasicStepMethodFilter(
        val resolvedFunction: JetElement,
        val myCallingExpressionLines: Range<Int>
) : NamedMethodFilter {
    private val myTargetMethodName: String

    init {
        myTargetMethodName = when (resolvedFunction) {
            is JetConstructor<*> -> "<init>"
            is JetPropertyAccessor -> JvmAbi.getterName((resolvedFunction.getParent() as JetProperty).getName()!!)
            else -> resolvedFunction.getName()!!
        }
    }

    override fun getCallingExpressionLines() = myCallingExpressionLines

    override fun getMethodName() = myTargetMethodName

    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        val method = location.method()
        if (myTargetMethodName != method.name()) return false

        val sourcePosition = runReadAction { SourcePosition.createFromElement(resolvedFunction) } ?: return false
        val positionManager = process.getPositionManager() ?: return false

        val classes = positionManager.getAllClasses(sourcePosition)

        return classes.contains(location.declaringType())
    }
}