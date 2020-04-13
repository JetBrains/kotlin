/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiCall
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.source.getPsi

object ReceiverSliceProducer : SliceProducer {
    override fun produce(usage: UsageInfo, mode: KotlinSliceAnalysisMode, parent: SliceUsage): Collection<SliceUsage>? {
        val refElement = usage.element ?: return emptyList()
        when (refElement) {
            is KtExpression -> {
                val resolvedCall = refElement.resolveToCall() ?: return emptyList()
                when (val receiver = resolvedCall.extensionReceiver) {
                    is ExpressionReceiver -> {
                        return listOf(KotlinSliceUsage(receiver.expression, parent, mode, forcedExpressionMode = true))
                    }

                    is ImplicitReceiver -> {
                        val callableDescriptor = receiver.declarationDescriptor as? CallableDescriptor
                            ?: return emptyList()
                        when (val callableDeclaration = callableDescriptor.originalSource.getPsi()) {
                            is KtFunctionLiteral -> {
                                val newMode = mode.withBehaviour(LambdaCallsBehaviour(ReceiverSliceProducer))
                                return listOf(KotlinSliceUsage(callableDeclaration, parent, newMode, forcedExpressionMode = true))
                            }

                            is KtCallableDeclaration -> {
                                val receiverTypeReference = callableDeclaration.receiverTypeReference ?: return emptyList()
                                return listOf(KotlinSliceUsage(receiverTypeReference, parent, mode, false))
                            }

                            else -> return emptyList()
                        }
                    }

                    else -> return emptyList()
                }
            }

            else -> {
                val argument = (refElement.parent as? PsiCall)?.argumentList?.expressions?.getOrNull(0) ?: return emptyList()
                return listOf(KotlinSliceUsage(argument, parent, mode, false))
            }
        }
    }

    override val testPresentation: String?
        get() = "RECEIVER"

    override fun equals(other: Any?) = other === this
    override fun hashCode() = 0
}