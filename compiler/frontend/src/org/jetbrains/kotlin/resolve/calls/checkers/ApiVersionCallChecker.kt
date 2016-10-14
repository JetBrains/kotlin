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

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.API_NOT_AVAILABLE
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.checkSinceKotlinVersionAccessibility

// TODO: consider combining with DeprecatedCallChecker somehow
object ApiVersionCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        check(resolvedCall.resultingDescriptor, context, reportOn)
    }

    private fun check(targetDescriptor: CallableDescriptor, context: CallCheckerContext, element: PsiElement) {
        // Objects will be checked by ApiVersionClassifierUsageChecker
        if (targetDescriptor is FakeCallableDescriptorForObject) return

        val accessible = targetDescriptor.checkSinceKotlinVersionAccessibility(context.languageVersionSettings) { version ->
            context.trace.report(
                    API_NOT_AVAILABLE.on(element, version.versionString, context.languageVersionSettings.apiVersion.versionString)
            )
        }

        if (accessible && targetDescriptor is PropertyDescriptor && DeprecatedCallChecker.shouldCheckPropertyGetter(element)) {
            targetDescriptor.getter?.let { check(it, context, element) }
        }
    }
}
