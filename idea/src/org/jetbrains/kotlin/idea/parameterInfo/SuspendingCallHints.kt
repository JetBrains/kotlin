/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun provideSuspendingCallHint(callExpression: KtCallExpression): InlayInfo? {
    val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
    val call = bindingContext[BindingContext.CALL, callExpression.calleeExpression] ?: return null
    val resolvedCall = bindingContext[BindingContext.RESOLVED_CALL, call] ?: return null
    if (resolvedCall.candidateDescriptor.isSuspend) {
        return InlayInfo(TYPE_INFO_PREFIX + "#", callExpression.calleeExpression?.startOffset ?: callExpression.startOffset)
    }
    return null
}