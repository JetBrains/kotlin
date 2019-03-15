/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.typeUtil.contains

object TypeOfChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!isTypeOf(resolvedCall.resultingDescriptor)) return

        for ((_, argument) in resolvedCall.typeArguments) {
            if (argument.contains { type ->
                    val descriptor = type.constructor.declarationDescriptor
                    descriptor is TypeParameterDescriptor && !descriptor.isReified
                }) {
                context.trace.report(Errors.UNSUPPORTED.on(reportOn, "'typeOf' with non-reified type parameters is not supported"))
            }
        }
    }

    fun isTypeOf(descriptor: CallableDescriptor): Boolean =
        descriptor.name.asString() == "typeOf" &&
                descriptor.valueParameters.isEmpty() &&
                (descriptor.containingDeclaration as? PackageFragmentDescriptor)?.fqName == KOTLIN_REFLECT_FQ_NAME
}
