/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.util.OperatorNameConventions

object DataObjectContentChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is FunctionDescriptor) return
        if (descriptor.name != OperatorNameConventions.EQUALS && descriptor.name != OperatorNameConventions.HASH_CODE) return

        val container = descriptor.containingDeclaration
        if (container !is ClassDescriptor) return
        if (!container.isData || container.kind != ClassKind.OBJECT) return

        if (DescriptorUtils.getAllOverriddenDescriptors(descriptor).any(::isDeclaredInAny)) {
            val target = declaration.modifierList?.getModifier(KtTokens.OVERRIDE_KEYWORD) ?: declaration
            context.trace.report(Errors.DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE.on(target))
        }
    }

    private fun isDeclaredInAny(descriptor: FunctionDescriptor): Boolean {
        val container = descriptor.containingDeclaration
        return container is ClassDescriptor && KotlinBuiltIns.isAny(container)
    }
}
