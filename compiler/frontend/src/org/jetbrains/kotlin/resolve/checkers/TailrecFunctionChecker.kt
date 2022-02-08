/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.isEffectivelyFinal

object TailrecFunctionChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtNamedFunction || descriptor !is FunctionDescriptor || !descriptor.isTailrec) return

        // Private check is needed for opened private functions by allopen plugin (see KT-48117)
        if (descriptor.isEffectivelyFinal(false) || DescriptorVisibilities.isPrivate(descriptor.visibility)) return

        context.trace.report(Errors.TAILREC_ON_VIRTUAL_MEMBER.on(context.languageVersionSettings, declaration))
    }
}
