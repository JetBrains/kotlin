/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.belongsToDelegatedSuperTypeEntry
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DELEGATE_TO_DEFAULTS_ANNOTATION_FQ_NAME

object JvmDelegateToDefaultsAnnotationChecker : AdditionalAnnotationChecker {
    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        annotated: KtAnnotated?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        entries.find { trace.get(BindingContext.ANNOTATION, it)?.fqName == JVM_DELEGATE_TO_DEFAULTS_ANNOTATION_FQ_NAME }?.let {
            if (annotated?.belongsToDelegatedSuperTypeEntry() != true) {
                trace.report(Errors.WRONG_ANNOTATION_TARGET.on(it, "expression"))
            }
        }
    }
}