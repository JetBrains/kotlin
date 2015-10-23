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

import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.openapi.project.Project
import com.sun.jdi.Value
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.debugger.evaluate.getClassDescriptor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

// This method check that all parameter of functional argument are present in current frame
fun isInsideInlinedArgument(lambda: KtFunction, visibleVariables: List<Pair<LocalVariableProxyImpl, Value>>): Boolean {
    val function = lambda.analyze(BodyResolveMode.PARTIAL).get(BindingContext.FUNCTION, lambda) ?: return false

    return function.valueParameters.all { isLocalVariableForParameterPresent(lambda.project, it, visibleVariables) }
}

private fun isLocalVariableForParameterPresent(project: Project, parameter: ValueParameterDescriptor, visibleVariables: List<Pair<LocalVariableProxyImpl, Value>>): Boolean {
    return visibleVariables.any {
        if (it.first.name() != parameter.name.asString()) return false

        val parameterClassDescriptor = parameter.type.constructor.declarationDescriptor as? ClassDescriptor ?: return true
        val actualClassDescriptor = it.second.asValue().asmType.getClassDescriptor(project) ?: return true

        return runReadAction { DescriptorUtils.isSubclass(actualClassDescriptor, parameterClassDescriptor) }
    }
}

