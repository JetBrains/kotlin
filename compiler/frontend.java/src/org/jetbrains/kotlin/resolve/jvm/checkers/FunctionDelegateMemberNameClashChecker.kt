/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object FunctionDelegateMemberNameClashChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is CallableMemberDescriptor) return
        val container = descriptor.containingDeclaration
        if (container !is ClassDescriptor || !container.isFun) return
        if (descriptor.extensionReceiverParameter != null || descriptor.valueParameters.isNotEmpty()) return

        if (descriptor is FunctionDescriptor && descriptor.name.asString() == "getFunctionDelegate" ||
            descriptor is PropertyDescriptor && descriptor.name.asString() == "functionDelegate"
        ) {
            val reportOn = (declaration as? KtNamedDeclaration)?.nameIdentifier ?: declaration
            context.trace.report(ErrorsJvm.FUNCTION_DELEGATE_MEMBER_NAME_CLASH.on(reportOn))
        }
    }
}
