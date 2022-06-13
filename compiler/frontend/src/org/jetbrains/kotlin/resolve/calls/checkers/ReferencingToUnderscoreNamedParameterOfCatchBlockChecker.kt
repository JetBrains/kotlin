/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isUnderscoreNamed
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

object ReferencingToUnderscoreNamedParameterOfCatchBlockChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.resultingDescriptor

        if (descriptor !is LocalVariableDescriptor || !descriptor.isUnderscoreNamed) return

        val sourceElement = descriptor.source as? KotlinSourceElement ?: return
        val ktParameter = sourceElement.psi as? KtParameter ?: return

        if (ktParameter.isCatchParameter) {
            context.trace.reportDiagnosticOnce(Errors.RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER.on(reportOn))
        }
    }
}
