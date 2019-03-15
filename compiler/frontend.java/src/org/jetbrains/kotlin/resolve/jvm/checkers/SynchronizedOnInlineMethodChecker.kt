/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.annotations.findSynchronizedAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object SynchronizedOnInlineMethodChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor is FunctionDescriptor && descriptor.isInline) {
            val annotation = descriptor.findSynchronizedAnnotation()
            if (annotation != null) {
                val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: declaration
                context.trace.report(ErrorsJvm.SYNCHRONIZED_ON_INLINE.on(reportOn))
            }
        }
    }
}
