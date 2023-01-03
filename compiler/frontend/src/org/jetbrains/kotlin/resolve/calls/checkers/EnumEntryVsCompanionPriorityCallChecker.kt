/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

object EnumEntryVsCompanionPriorityCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor
        if (descriptor !is PropertyDescriptor) return
        val propertyName = descriptor.name

        val containingDescriptor = descriptor.containingDeclaration
        if (containingDescriptor !is ClassDescriptor || !containingDescriptor.isCompanionObject) return

        val grandParent = containingDescriptor.containingDeclaration

        if (grandParent is ClassDescriptor &&
            grandParent.kind == ClassKind.ENUM_CLASS &&
            grandParent.containsEntryWithName(propertyName) &&
            resolvedCall.dispatchReceiver.isQualifierFor(grandParent)) {
            context.resolutionContext.trace.report(Errors.DEPRECATED_ACCESS_TO_ENUM_COMPANION_PROPERTY.on(reportOn, descriptor))
        }
    }

    private fun ClassDescriptor.containsEntryWithName(name: Name): Boolean {
        val foundDescriptor = unsubstitutedMemberScope.getContributedClassifier(name, NoLookupLocation.FOR_ALREADY_TRACKED)
        return foundDescriptor is ClassDescriptor && foundDescriptor.kind == ClassKind.ENUM_ENTRY
    }
}

internal fun ReceiverValue?.isQualifierFor(classDescriptor: ClassDescriptor): Boolean {
    if (this !is ClassValueReceiver) return false
    val thisClass = this.classQualifier.descriptor as? ClassDescriptor ?: return false
    return thisClass.typeConstructor == classDescriptor.typeConstructor
}
