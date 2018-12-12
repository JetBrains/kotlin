/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * This checker detects if call by operator convention to a Java method violates some expected contract:
 * - "key in map" commonly resolves to an stdlib extension that calls Map.containsKey(),
 *   but there's a member in ConcurrentHashMap with acceptable signature that delegates to `containsValue` instead
 */
object InconsistentOperatorFromJavaCallChecker : CallChecker {
    private val CONCURRENT_HASH_MAP_FQ_NAME = FqName("java.util.concurrent.ConcurrentHashMap")

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (candidateDescriptor.name != OperatorNameConventions.CONTAINS) return
        if (candidateDescriptor.valueParameters.singleOrNull()?.type?.isAnyOrNullableAny() != true) return
        if (resolvedCall.call.callElement !is KtBinaryExpression || !resolvedCall.status.possibleTransformToSuccess()) return

        for (callableDescriptor in candidateDescriptor.overriddenTreeUniqueAsSequence(useOriginal = false)) {
            val containingClass = callableDescriptor.containingDeclaration as? ClassDescriptor ?: continue
            if (containingClass.fqNameOrNull() != CONCURRENT_HASH_MAP_FQ_NAME) continue

            val diagnosticFactory =
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitConcurrentHashMapContains))
                    ErrorsJvm.CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR
                else
                    ErrorsJvm.CONCURRENT_HASH_MAP_CONTAINS_OPERATOR

            context.trace.report(diagnosticFactory.on(reportOn))
            break
        }
    }
}
