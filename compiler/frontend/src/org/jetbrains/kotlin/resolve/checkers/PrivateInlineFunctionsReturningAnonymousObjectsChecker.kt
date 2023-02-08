/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

object PrivateInlineFunctionsReturningAnonymousObjectsChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is SimpleFunctionDescriptor || !descriptor.isInline || !DescriptorVisibilities.isPrivate(descriptor.visibility) || declaration !is KtNamedFunction)
            return

        val nameIdentifier = declaration.nameIdentifier ?: return
        val returnType = descriptor.returnType ?: return

        checkTypeAndArguments(returnType, nameIdentifier, context)
    }

    private fun checkTypeAndArguments(type: KotlinType, reportOn: PsiElement, context: DeclarationCheckerContext) {
        checkType(type, reportOn, context)
        for (argument in type.arguments) {
            checkTypeAndArguments(argument.type, reportOn, context)
        }
    }

    private fun checkType(type: KotlinType, reportOn: PsiElement, context: DeclarationCheckerContext) {
        val returnTypeConstructor = type.constructor
        val returnTypeDeclarationDescriptor = returnTypeConstructor.declarationDescriptor ?: return
        if (DescriptorUtils.isAnonymousObject(returnTypeDeclarationDescriptor)) {
            context.trace.report(Errors.PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS.on(reportOn))
        }
    }
}
