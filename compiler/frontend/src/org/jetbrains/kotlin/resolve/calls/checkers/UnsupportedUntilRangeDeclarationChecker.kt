/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.util.OperatorNameConventions.RANGE_UNTIL

object UnsupportedUntilRangeDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val isRangeUntilOperatorSupported = context.languageVersionSettings.supportsFeature(LanguageFeature.RangeUntilOperator)

        if (!isRangeUntilOperatorSupported && descriptor is FunctionDescriptor && descriptor.isOperator && descriptor.name == RANGE_UNTIL) {
            val operatorKeyword = declaration.modifierList?.getModifier(KtTokens.OPERATOR_KEYWORD) ?: return
            context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(operatorKeyword, LanguageFeature.RangeUntilOperator to context.languageVersionSettings)
            )
        }
    }
}