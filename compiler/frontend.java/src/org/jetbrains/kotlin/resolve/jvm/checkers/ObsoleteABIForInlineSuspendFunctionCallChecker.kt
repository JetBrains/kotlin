/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.getContainingKotlinJvmBinaryClass
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object ObsoleteABIForInlineSuspendFunctionCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val candidateDescriptor = resolvedCall.candidateDescriptor as? FunctionDescriptor ?: return
        if (!candidateDescriptor.isSuspend || !candidateDescriptor.isInline) return

        val jvmBytecodeVersion = candidateDescriptor.getContainingKotlinJvmBinaryClass()?.classHeader?.bytecodeVersion ?: return

        if (!jvmBytecodeVersion.isAtLeast(1, 0, 2)) {
            context.trace.report(ErrorsJvm.OBSOLETE_SUSPEND_INLINE_FUNCTIONS_ABI.on(reportOn))
        }
    }
}
