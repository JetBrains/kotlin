/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.INVALID_EXTERNAL_DECLARATION_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonExternalChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

object FirWasmExternalChecker : FirWebCommonExternalChecker(allowCompanionInInterface = false) {
    private val jsNameFqn = ClassId.fromString("kotlin/js/JsName")

    override fun isNativeOrEffectivelyExternal(symbol: FirBasedSymbol<*>, session: FirSession): Boolean {
        return symbol.isEffectivelyExternal(session)
    }

    override fun reportExternalEnum(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "enum class", context)
    }

    override fun additionalCheck(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirMemberDeclaration) {
            if (context.isTopLevel) {
                val declarationName = getDeclarationName(declaration, context)
                if (declarationName == null || declarationName in RESERVED_KEYWORDS) {
                    reporter.reportOn(declaration.source, INVALID_EXTERNAL_DECLARATION_NAME, context)
                }
            }
        }

        if (declaration is FirFunction) {
            if (declaration.isInline) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.INLINE_EXTERNAL_DECLARATION, context)
            }
            if (declaration.isTailRec) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "tailrec function", context)
            }
            if (declaration.isSuspend) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "suspend function", context)
            }
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
                if (declaration.contextParameters.isNotEmpty()) {
                    reporter.reportOn(declaration.source, FirWasmErrors.EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS, context)
                }
            }
        }

        if (declaration is FirProperty) {
            if (declaration.isLateInit) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "lateinit property", context)
            }
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
                if (declaration.contextParameters.isNotEmpty()) {
                    reporter.reportOn(declaration.source, FirWasmErrors.EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS, context)
                }
            }
        }
    }

    override fun isDefinedExternallyCallableId(callableId: CallableId): Boolean =
        callableId == WebCommonStandardClassIds.Callables.JsDefinedExternally

    override fun hasExternalLikeAnnotations(declaration: FirDeclaration, session: FirSession): Boolean =
        false

    private fun getDeclarationName(declaration: FirMemberDeclaration, context: CheckerContext): String? {
        val jsNameAnnotation = declaration.annotations.getAnnotationByClassId(jsNameFqn, session = context.session)
        val jsNameArgument = jsNameAnnotation?.argumentMapping?.mapping[Name.identifier("name")]
        val jsNameString = (jsNameArgument as? FirLiteralExpression)?.value as? String
        require(jsNameAnnotation == null || jsNameString != null)
        return jsNameString ?: declaration.nameOrSpecialName.identifierOrNullIfSpecial
    }
}

