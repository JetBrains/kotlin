/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

object AllowTypedEqualsApplicabilityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtClass) return
        val annotation = descriptor.annotations.findAnnotation(StandardClassIds.Annotations.AllowTypedEquals.asSingleFqName()) ?: return
        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
        if (!declaration.isValue() && !declaration.isInline()) {
            context.trace.report(Errors.INAPPLICABLE_ALLOW_TYPED_EQUALS_ANNOTATION.on(annotationEntry))
        }
    }
}