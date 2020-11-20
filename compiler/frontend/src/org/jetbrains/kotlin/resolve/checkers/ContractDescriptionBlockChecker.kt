/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor

object ContractDescriptionBlockChecker: DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val contractDescription = when (declaration) {
            is KtNamedFunction -> declaration.contractDescription
            is KtPropertyAccessor -> declaration.contractDescription
            else -> null
        }
        if (contractDescription != null) {
            context.trace.report(Errors.UNSUPPORTED.on(contractDescription, "Contract description blocks are not supported"))
        }
    }
}