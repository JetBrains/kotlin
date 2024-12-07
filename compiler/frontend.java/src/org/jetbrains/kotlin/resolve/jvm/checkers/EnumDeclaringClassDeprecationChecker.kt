/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver

object EnumDeclaringClassDeprecationChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is PropertyDescriptor || resultingDescriptor.kind != CallableMemberDescriptor.Kind.SYNTHESIZED) return
        val extensionReceiver = resultingDescriptor.extensionReceiverParameter?.value as? ExtensionReceiver ?: return
        if (resultingDescriptor.name.asString() != "declaringClass") return
        val extensionReceiverConstructor = extensionReceiver.type.constructor
        if (extensionReceiverConstructor.declarationDescriptor.classId != StandardClassIds.Enum &&
            extensionReceiverConstructor.supertypes.none { it.constructor.declarationDescriptor.classId == StandardClassIds.Enum }
        ) return
        context.trace.report(ErrorsJvm.ENUM_DECLARING_CLASS_DEPRECATED.on(context.languageVersionSettings, reportOn))
    }
}