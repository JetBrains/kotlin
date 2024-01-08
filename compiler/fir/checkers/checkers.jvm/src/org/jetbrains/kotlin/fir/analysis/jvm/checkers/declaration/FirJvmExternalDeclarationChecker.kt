/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtTokens

object FirJvmExternalDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirPropertyAccessor) return
        checkInternal(declaration, null, null, context, reporter)
    }

    private fun checkInternal(
        declaration: FirDeclaration,
        reportSource: KtSourceElement?,
        modality: Modality?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (declaration !is FirMemberDeclaration) return

        if (declaration is FirProperty) {
            declaration.getter?.let {
                checkInternal(it, declaration.source, declaration.modality, context, reporter)
            }
        }

        if (!declaration.isExternal) return
        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        // WRONG_MODIFIER_TARGET on external constructor is intentionally NOT covered in this checker.
        if (declaration !is FirFunction) {
            val target = when (declaration) {
                is FirProperty -> "property"
                is FirRegularClass -> "class"
                else -> "non-function declaration"
            }
            val externalModifier = declaration.getModifier(KtTokens.EXTERNAL_KEYWORD)
            externalModifier?.let {
                reporter.reportOn(it.source, FirErrors.WRONG_MODIFIER_TARGET, it.token, target, context)
            }
            return
        }

        val containingClassSymbol = declaration.symbol.containingClassLookupTag()?.toFirRegularClassSymbol(context.session)
        if (containingClassSymbol != null) {
            if (containingClassSymbol.isInterface) {
                reporter.reportOn(declaration.source, FirJvmErrors.EXTERNAL_DECLARATION_IN_INTERFACE, context)
            } else if ((modality ?: declaration.modality) == Modality.ABSTRACT) {
                reporter.reportOn(reportSource ?: declaration.source, FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT, context)
            }
        }

        if (declaration !is FirConstructor && declaration.body != null) {
            reporter.reportOn(declaration.source, FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_HAVE_BODY, context)
        }

        if (declaration.isInline) {
            reporter.reportOn(declaration.source, FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_BE_INLINED, context)
        }
    }
}
