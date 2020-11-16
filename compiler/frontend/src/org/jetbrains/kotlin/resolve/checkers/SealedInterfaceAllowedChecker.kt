/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration

object SealedInterfaceAllowedChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.SealedInterfaces)) return
        if (descriptor !is ClassDescriptor) return
        if (descriptor.kind != ClassKind.INTERFACE) return
        val keyword = declaration.modifierList?.getModifier(KtTokens.SEALED_KEYWORD) ?: return
        context.trace.report(Errors.WRONG_MODIFIER_TARGET.on(keyword, KtTokens.SEALED_KEYWORD, KotlinTarget.INTERFACE.description))
    }
}
