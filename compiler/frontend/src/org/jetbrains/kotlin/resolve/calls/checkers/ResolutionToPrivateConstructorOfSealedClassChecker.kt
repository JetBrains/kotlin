/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.isSealed
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

object ResolutionToPrivateConstructorOfSealedClassChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        // If AllowSealedInheritorsInDifferentFilesOfSamePackage disabled then all sealed constructors are private by default
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage)) return
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.UseConsistentRulesForPrivateConstructorsOfSealedClasses)) return
        val descriptor = resolvedCall.resultingDescriptor as? ConstructorDescriptor ?: return
        if (descriptor.visibility != DescriptorVisibilities.PRIVATE) return
        if (!descriptor.constructedClass.isSealed()) return
        val containingDescriptor = context.scope.ownerDescriptor
        val receiver = resolvedCall.dispatchReceiver ?: DescriptorVisibilities.ALWAYS_SUITABLE_RECEIVER
        if (DescriptorVisibilities.findInvisibleMember(receiver, descriptor, containingDescriptor, false) != null) {
            context.trace.report(Errors.RESOLUTION_TO_PRIVATE_CONSTRUCTOR_OF_SEALED_CLASS.on(reportOn))
        }
    }
}
