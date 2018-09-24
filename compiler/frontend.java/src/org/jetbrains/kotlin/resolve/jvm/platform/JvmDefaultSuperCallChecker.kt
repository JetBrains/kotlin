/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.platform

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getSuperCallExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmDefaultSuperCallChecker : CallChecker {

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val jvmDefaultMode = context.languageVersionSettings.getFlag(AnalysisFlag.jvmDefaultMode)
        if (jvmDefaultMode.isEnabled) return
        val superExpression = getSuperCallExpression(resolvedCall.call) ?: return
        val resultingDescriptor = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return
        if (!resultingDescriptor.hasJvmDefaultAnnotation()) return

        if (DescriptorUtils.isInterface(resultingDescriptor.containingDeclaration)) {
            context.trace.report(ErrorsJvm.USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL.on(reportOn))
        }
    }
}
