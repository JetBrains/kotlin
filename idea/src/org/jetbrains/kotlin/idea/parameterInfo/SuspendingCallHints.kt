/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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