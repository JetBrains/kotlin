/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.annotations.SYNCHRONIZED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object SynchronizedAnnotationOnLambdaChecker : AdditionalAnnotationChecker {
    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        annotated: KtAnnotated?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (entries.isEmpty()) return

        val annotation = entries.find { trace.get(BindingContext.ANNOTATION, it)?.fqName == SYNCHRONIZED_ANNOTATION_FQ_NAME } ?: return

        val literal = (annotated?.unwrapParenthesesLabelsAndAnnotations() as? KtLambdaExpression)?.functionLiteral ?: return
        val descriptor = trace.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, literal]
        if (descriptor is FunctionDescriptor && descriptor.isSuspend) {
            trace.report(ErrorsJvm.SYNCHRONIZED_ON_SUSPEND.on(annotation))
        }
    }
}