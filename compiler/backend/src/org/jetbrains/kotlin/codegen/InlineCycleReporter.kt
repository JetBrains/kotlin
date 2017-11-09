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

package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class InlineCycleReporter(private val diagnostics: DiagnosticSink) {

    private val processingFunctions = linkedMapOf<PsiElement, CallableDescriptor>()

    fun enterIntoInlining(call: ResolvedCall<*>?): Boolean {
        //null call for default method inlining
        if (call != null) {
            val callElement = call.call.callElement
            if (processingFunctions.contains(callElement)) {
                val cycle = processingFunctions.asSequence().dropWhile { it.key != callElement }
                cycle.forEach {
                    diagnostics.report(Errors.INLINE_CALL_CYCLE.on(it.key, it.value))
                }
                return false
            }
            processingFunctions.put(callElement, call.resultingDescriptor.original)
        }
        return true
    }

    fun exitFromInliningOf(call: ResolvedCall<*>?) {
        if (call != null) {
            val callElement = call.call.callElement
            processingFunctions.remove(callElement)
        }
    }
}
