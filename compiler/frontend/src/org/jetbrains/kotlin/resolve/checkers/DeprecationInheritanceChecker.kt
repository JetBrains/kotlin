/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature.StopPropagatingDeprecationThroughOverrides
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty

object DeprecationInheritanceChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtNamedDeclaration) return
        if (declaration is KtProperty && descriptor is PropertyAccessorDescriptor && descriptor.isDefault) {
            return
        }
        val deprecationResolver = context.deprecationResolver
        if (!deprecationResolver.areDeprecationsInheritedFromOverriden(descriptor)) return
        val (deprecations, message) = if (context.languageVersionSettings.supportsFeature(StopPropagatingDeprecationThroughOverrides)) {
            deprecationResolver.getHiddenDeprecationsFromOverriden(descriptor) to ""
        } else {
            deprecationResolver.getDeprecations(descriptor) to "This deprecation won't be inherited in future releases. "
        }
        context.trace.report(Errors.OVERRIDE_DEPRECATION.on(declaration, message, descriptor, deprecations))
    }
}
