/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace

object OptionalExpectationTargetChecker {
    fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        trace: BindingTrace
    ) {
        if (descriptor is MemberDescriptor && descriptor.isExpect) return

        for (entry in declaration.annotationEntries) {
            val annotationDescriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            if (annotationDescriptor.fqName == ExpectedActualDeclarationChecker.OPTIONAL_EXPECTATION_FQ_NAME) {
                trace.report(Errors.OPTIONAL_EXPECTATION_NOT_ON_EXPECTED.on(entry))
            }
        }
    }
}
