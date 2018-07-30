/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource

class OptionalExpectationUsageChecker : ClassifierUsageChecker {
    override fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext) {
        if (!ExpectedActualDeclarationChecker.isOptionalAnnotationClass(targetDescriptor)) return

        if (!element.isUsageAsAnnotationOrImport()) {
            context.trace.report(Errors.OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY.on(element))
        }

        val ktFile = element.containingFile as KtFile
        if (ktFile.isCommonSource != true) {
            context.trace.report(Errors.OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE.on(element))
        }
    }
}
