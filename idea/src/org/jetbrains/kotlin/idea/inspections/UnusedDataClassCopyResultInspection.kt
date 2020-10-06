/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver

class UnusedDataClassCopyResultInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(call) {
        val callee = call.calleeExpression ?: return
        if (callee.text != "copy") return
        val context = call.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
        val descriptor = call.getResolvedCall(context)?.resultingDescriptor ?: return
        val receiver = descriptor.dispatchReceiverParameter ?: descriptor.extensionReceiverParameter ?: return
        if ((receiver.value as? ImplicitClassReceiver)?.classDescriptor?.isData != true) return
        if (call.getQualifiedExpressionForSelectorOrThis().isUsedAsExpression(context)) return
        holder.registerProblem(callee, KotlinBundle.message("inspection.unused.result.of.data.class.copy"))
    })
}
