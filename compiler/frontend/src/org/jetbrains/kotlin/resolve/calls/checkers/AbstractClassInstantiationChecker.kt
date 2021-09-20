/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.Errors.CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.resolve.calls.util.isSuperOrDelegatingConstructorCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

object AbstractClassInstantiationChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val candidateDescriptor = resolvedCall.candidateDescriptor
        val call = resolvedCall.call

        if (candidateDescriptor is ConstructorDescriptor &&
            !isSuperOrDelegatingConstructorCall(call)
        ) {
            if (candidateDescriptor.constructedClass.modality == Modality.ABSTRACT) {
                context.trace.reportDiagnosticOnce(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS.on(call.callElement))
            }
        }
    }
}