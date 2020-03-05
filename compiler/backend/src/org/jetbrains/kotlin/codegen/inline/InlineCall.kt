/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

interface InlineCall {
    val id: Any
    val calleeDescriptor: CallableDescriptor
    val callElement: PsiElement?
}

class InlineCallImpl(
    override val calleeDescriptor: CallableDescriptor,
    override val callElement: PsiElement
) : InlineCall {

    override val id: Any
        get() = callElement

    companion object {
        fun of(resolvedCall: ResolvedCall<*>?) =
            resolvedCall?.run {
                InlineCallImpl(resultingDescriptor.original, call.callElement)
            }
    }
}