/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.checkers.isComputingDeferredType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.ClassifierUsageChecker
import org.jetbrains.kotlin.resolve.checkers.ClassifierUsageCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

object MissingBuiltInDeclarationChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.languageVersionSettings.getFlag(JvmAnalysisFlags.suppressMissingBuiltinsError)) return

        val descriptor = resolvedCall.resultingDescriptor
        val error = diagnosticFor(descriptor, reportOn)
            ?: diagnosticFor(descriptor.returnType?.takeUnless(this::isComputingDeferredType)?.constructor?.declarationDescriptor, reportOn)
        error?.let(context.trace::report)
    }

    private fun diagnosticFor(descriptor: DeclarationDescriptor?, reportOn: PsiElement): Diagnostic? {
        if (descriptor == null) return null

        val containingClassOrPackage = DescriptorUtils.getParentOfType(descriptor, ClassOrPackageFragmentDescriptor::class.java)

        if (containingClassOrPackage is ClassDescriptor) {
            val containingPackage = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor::class.java)
            if ((containingPackage as? BuiltInsPackageFragment)?.isFallback == true) {
                return Errors.MISSING_BUILT_IN_DECLARATION.on(reportOn, containingClassOrPackage.fqNameSafe)
            }
        } else if (containingClassOrPackage is BuiltInsPackageFragment && containingClassOrPackage.isFallback) {
            return Errors.MISSING_BUILT_IN_DECLARATION.on(reportOn, descriptor.fqNameSafe)
        }

        return null
    }

    object ClassifierUsage : ClassifierUsageChecker {
        override fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext) {
            if (context.languageVersionSettings.getFlag(JvmAnalysisFlags.suppressMissingBuiltinsError)) return

            diagnosticFor(targetDescriptor, element)?.let(context.trace::report)
        }
    }
}
