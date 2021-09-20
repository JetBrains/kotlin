/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.cast

class NewVariableAsFunctionResolvedCallImpl(
    override val variableCall: NewResolvedCallImpl<VariableDescriptor>,
    override val functionCall: NewResolvedCallImpl<FunctionDescriptor>,
) : VariableAsFunctionResolvedCall, ResolvedCall<FunctionDescriptor> by functionCall {
    val baseCall get() = functionCall.resolvedCallAtom.atom.psiKotlinCall.cast<PSIKotlinCallForInvoke>().baseCall
}