/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

object PrivateInlineFunctionsReturningAnonymousObjectsChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ApproximateAnonymousReturnTypesInPrivateInlineFunctions))
            return

        if (descriptor !is SimpleFunctionDescriptor || !descriptor.isInline || !DescriptorVisibilities.isPrivate(descriptor.visibility) || declaration !is KtNamedFunction)
            return

        val returnTypeConstructor = descriptor.returnType?.constructor ?: return

        if (returnTypeConstructor.supertypes.singleOrNull { it.isAnyOrNullableAny() } == null) return

        val nameIdentifier = declaration.nameIdentifier ?: return
        val returnTypeDeclarationDescriptor = returnTypeConstructor.declarationDescriptor ?: return

        if (DescriptorUtils.isAnonymousObject(returnTypeDeclarationDescriptor)) {
            context.trace.report(Errors.PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS.on(nameIdentifier))
        }
    }
}
