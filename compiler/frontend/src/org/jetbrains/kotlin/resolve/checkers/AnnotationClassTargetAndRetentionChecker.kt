/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention

class AnnotationClassTargetAndRetentionChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        if (declaration !is KtClassOrObject) return
        if (!DescriptorUtils.isAnnotationClass(descriptor)) return

        val targets = AnnotationChecker.applicableTargetSet(descriptor) ?: return
        val retention = descriptor.getAnnotationRetention() ?: KotlinRetention.RUNTIME

        if (targets.contains(KotlinTarget.EXPRESSION) && retention != KotlinRetention.SOURCE) {
            val retentionAnnotation = descriptor.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.retention)
            val targetAnnotation = descriptor.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.target)

            val diagnostics =
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.RestrictRetentionForExpressionAnnotations))
                    Errors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION
                else
                    Errors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_WARNING
            context.trace.report(diagnostics.on(retentionAnnotation?.psi ?: targetAnnotation?.psi ?: declaration))
        }
    }

    private val AnnotationDescriptor.psi: PsiElement?
        get() = DescriptorToSourceUtils.getSourceFromAnnotation(this)
}