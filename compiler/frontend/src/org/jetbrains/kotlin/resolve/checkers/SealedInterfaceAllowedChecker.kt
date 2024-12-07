/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration

object SealedInterfaceAllowedChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        if (descriptor.kind != ClassKind.INTERFACE) return
        val keyword = declaration.modifierList?.getModifier(KtTokens.SEALED_KEYWORD) ?: return
        val diagnostic = if (context.languageVersionSettings.supportsFeature(LanguageFeature.SealedInterfaces)) {
            if (descriptor.isFun) {
                Errors.UNSUPPORTED_SEALED_FUN_INTERFACE.on(keyword)
            } else return
        } else {
            Errors.UNSUPPORTED_FEATURE.on(keyword, LanguageFeature.SealedInterfaces to context.languageVersionSettings)
        }
        context.trace.report(diagnostic)
    }
}
