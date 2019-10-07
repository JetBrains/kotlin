/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType

// TODO: remove these checkers before 1.4 is released

object TrailingCommaChecker {
    fun check(trailingComma: PsiElement?, trace: BindingTrace, languageVersionSettings: LanguageVersionSettings) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.TrailingCommas) && trailingComma != null) {
            trace.report(Errors.UNSUPPORTED_FEATURE.on(trailingComma, LanguageFeature.TrailingCommas to languageVersionSettings))
        }
    }
}

object TrailingCommaDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        when (declaration) {
            is KtClass -> {
                TrailingCommaChecker.check(declaration.typeParameterList?.trailingComma, context.trace, context.languageVersionSettings)
            }
            is KtCallableDeclaration -> { // also it's executed for anonymous function declarations
                TrailingCommaChecker.check(declaration.valueParameterList?.trailingComma, context.trace, context.languageVersionSettings)
                TrailingCommaChecker.check(declaration.typeParameterList?.trailingComma, context.trace, context.languageVersionSettings)
                if (declaration is KtProperty && declaration.setter != null) {
                    TrailingCommaChecker.check(
                        declaration.setter?.parameterList?.trailingComma,
                        context.trace,
                        context.languageVersionSettings
                    )
                }
            }
            is KtDestructuringDeclaration -> {
                TrailingCommaChecker.check(declaration.trailingComma, context.trace, context.languageVersionSettings)
            }
            is KtTypeAlias -> {
                TrailingCommaChecker.check(declaration.typeParameterList?.trailingComma, context.trace, context.languageVersionSettings)
            }
        }
    }
}

object TrailingCommaCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        when (val callElement = resolvedCall.call.callElement) {
            is KtArrayAccessExpression -> TrailingCommaChecker.check(
                callElement.trailingComma,
                context.trace,
                context.languageVersionSettings
            )
            is KtCollectionLiteralExpression -> TrailingCommaChecker.check(
                callElement.trailingComma,
                context.trace,
                context.languageVersionSettings
            )
            is KtWhenExpression -> {
                if (callElement.subjectExpression != null) {
                    callElement.entries.forEach { whenEntry ->
                        TrailingCommaChecker.check(whenEntry.trailingComma, context.trace, context.languageVersionSettings)
                    }
                }
            }
            else -> {
                resolvedCall.call.run {
                    TrailingCommaChecker.check(valueArgumentList?.trailingComma, context.trace, context.languageVersionSettings)
                }
            }
        }
    }
}
