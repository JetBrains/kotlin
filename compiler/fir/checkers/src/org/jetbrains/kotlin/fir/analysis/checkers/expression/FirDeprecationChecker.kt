/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeCallToDeprecatedOverrideOfHidden
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasForConstructor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isTypealiasExpansion
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

object FirDeprecationChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {

    private val filteredSourceKinds: Set<KtFakeSourceElementKind> = setOf(
        KtFakeSourceElementKind.PropertyFromParameter,
        KtFakeSourceElementKind.DataClassGeneratedMembers
    )

    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.source?.kind in filteredSourceKinds) return
        if (expression is FirAnnotation) return // checked by FirDeprecatedTypeChecker
        if (expression.isLhsOfAssignment(context)) return

        val calleeReference = expression.toReference(context.session) ?: return
        val resolvedReference = calleeReference.resolved ?: return
        val referencedSymbol = resolvedReference.resolvedSymbol

        if (expression.isDelegatedPropertySelfAccess(context, referencedSymbol)) return

        val source = resolvedReference.source ?: expression.source

        if (expression is FirDelegatedConstructorCall) {
            // Report deprecations on the constructor itself, not on the declaring class as that will be handled by FirDeprecatedTypeChecker
            val constructorOnlyDeprecation = referencedSymbol.getDeprecation(context.session, expression) ?: return
            val isTypealiasExpansion = expression.constructedTypeRef.coneType.fullyExpandedType(context.session).isTypealiasExpansion

            reportApiStatus(
                source, referencedSymbol, isTypealiasExpansion,
                constructorOnlyDeprecation, reporter, context
            )
        } else {
            reportApiStatusIfNeeded(source, referencedSymbol, context, reporter, callSite = expression)
        }

        reportCallToDeprecatedOverrideOfHidden(expression, source, referencedSymbol, reporter, context)
    }

    private fun reportCallToDeprecatedOverrideOfHidden(
        expression: FirStatement,
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        if (expression !is FirQualifiedAccessExpression) return
        if (ConeCallToDeprecatedOverrideOfHidden in expression.nonFatalDiagnostics) {
            val unwrappedSymbol = (referencedSymbol as? FirSyntheticPropertySymbol)?.getterSymbol?.delegateFunctionSymbol
                ?: referencedSymbol as? FirCallableSymbol
            val callableName = unwrappedSymbol?.callableId?.callableName?.asString()
            val message = getDeprecatedOverrideOfHiddenMessage(callableName)
            reporter.reportOn(source, FirErrors.DEPRECATION, referencedSymbol, message, context)
        }
    }

    internal val DeprecatedOverrideOfHiddenReplacements: Map<String, String?> = mapOf(
        "getFirst" to "first()",
        "getLast" to "last()",
        "toArray" to null,
    )

    internal fun getDeprecatedOverrideOfHiddenMessage(callableName: String?): String {
        val getFirstOrLastReplacement = DeprecatedOverrideOfHiddenReplacements[callableName]
        return if (getFirstOrLastReplacement != null) {
            "This declaration will be renamed in a future version of Kotlin. Please consider using the '$getFirstOrLastReplacement' stdlib extension if the collection supports fast random access."
        } else {
            "This declaration is redundant in Kotlin and might be removed soon."
        }
    }

    /** Checks if this is an access to a delegated property inside the delegated property itself.
     *  Deprecations shouldn't be reported here. */
    @OptIn(SymbolInternals::class)
    private fun FirStatement.isDelegatedPropertySelfAccess(context: CheckerContext, referencedSymbol: FirBasedSymbol<*>): Boolean {
        if (source?.kind != KtFakeSourceElementKind.DelegatedPropertyAccessor) return false
        val containers = context.containingDeclarations
        val size = containers.size
        val fir = referencedSymbol.fir

        return containers.getOrNull(size - 1) == fir // For `provideDelegate`, the call will be in the initializer
                || containers.getOrNull(size - 2) == fir // For `getValue`, the call will be in the accessor
    }

    internal fun reportApiStatusIfNeeded(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        callSite: FirElement? = null,
    ) {
        val deprecation = getWorstDeprecation(callSite, referencedSymbol, context) ?: return
        val isTypealiasExpansion = deprecation.isTypealiasExpansionOf(referencedSymbol, callSite, context)
        reportApiStatus(source, referencedSymbol, isTypealiasExpansion, deprecation, reporter, context)
    }

    private fun FirDeprecationInfo.isTypealiasExpansionOf(
        referencedSymbol: FirBasedSymbol<*>,
        callSite: FirElement?,
        context: CheckerContext,
    ): Boolean = when (referencedSymbol) {
        is FirConstructorSymbol -> referencedSymbol.typeAliasForConstructor
            ?.let { isTypealiasExpansionOf(it, callSite, context) }
            ?: false
        !is FirTypeAliasSymbol -> false
        else -> referencedSymbol.getOwnDeprecation(context.session, callSite).let {
            // If 2 deprecations along a typealias "expansion chain"
            // are equivalent (a <= b && a >= b), then getDeprecation()
            // has returned the first of them.
            // When calling getWorstDeprecation(), deprecations
            // from typealiases should come first.
            it == null || it < this
        }
    }

    internal fun reportApiStatus(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        isTypealiasExpansion: Boolean,
        deprecationInfo: FirDeprecationInfo,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        when (deprecationInfo) {
            is FutureApiDeprecationInfo -> reportApiNotAvailable(source, deprecationInfo, reporter, context)
            is RequireKotlinDeprecationInfo -> reportVersionRequirementDeprecation(source, referencedSymbol, deprecationInfo, reporter, context)
            else -> reportDeprecation(source, referencedSymbol, isTypealiasExpansion, deprecationInfo, reporter, context)
        }
    }

    private fun reportVersionRequirementDeprecation(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        deprecationInfo: RequireKotlinDeprecationInfo,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        val diagnostic = when (deprecationInfo.deprecationLevel) {
            DeprecationLevelValue.WARNING -> FirErrors.VERSION_REQUIREMENT_DEPRECATION
            else -> FirErrors.VERSION_REQUIREMENT_DEPRECATION_ERROR
        }
        val languageVersionSettings = context.session.languageVersionSettings
        val currentVersionString = when (deprecationInfo.versionRequirement.kind) {
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION -> KotlinCompilerVersion.VERSION
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION -> languageVersionSettings.languageVersion.versionString
            ProtoBuf.VersionRequirement.VersionKind.API_VERSION -> languageVersionSettings.apiVersion.versionString
        }

        reporter.reportOn(
            source,
            diagnostic,
            referencedSymbol,
            deprecationInfo.versionRequirement.version,
            currentVersionString,
            deprecationInfo.getMessage(context.session) ?: "",
            context
        )
    }

    private fun reportDeprecation(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        isTypealiasExpansion: Boolean,
        deprecationInfo: FirDeprecationInfo,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        if (!isTypealiasExpansion) {
            val diagnostic = when (deprecationInfo.deprecationLevel) {
                DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> FirErrors.DEPRECATION_ERROR
                DeprecationLevelValue.WARNING -> FirErrors.DEPRECATION
            }
            reporter.reportOn(source, diagnostic, referencedSymbol, deprecationInfo.getMessage(context.session) ?: "", context)
        } else {
            val diagnostic = when (deprecationInfo.deprecationLevel) {
                DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> FirErrors.TYPEALIAS_EXPANSION_DEPRECATION_ERROR
                DeprecationLevelValue.WARNING -> FirErrors.TYPEALIAS_EXPANSION_DEPRECATION
            }
            reporter.reportOn(source, diagnostic, referencedSymbol, referencedSymbol, deprecationInfo.getMessage(context.session) ?: "", context)
        }
    }

    private fun reportApiNotAvailable(
        source: KtSourceElement?,
        deprecationInfo: FutureApiDeprecationInfo,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        reporter.reportOn(
            source,
            FirErrors.API_NOT_AVAILABLE,
            deprecationInfo.sinceVersion,
            context.languageVersionSettings.apiVersion,
            context,
        )
    }

    private fun getWorstDeprecation(
        callSite: FirElement?,
        symbol: FirBasedSymbol<*>,
        context: CheckerContext
    ): FirDeprecationInfo? {
        val deprecationInfos = listOfNotNull(
            (symbol as? FirConstructorSymbol)
                ?.classSymbolItIsCalledThrough(context)
                ?.getDeprecation(context.session, callSite),
            symbol.getDeprecation(context.session, callSite),
        )
        return deprecationInfos.maxOrNull()
    }

    private fun FirConstructorSymbol.classSymbolItIsCalledThrough(context: CheckerContext): FirClassLikeSymbol<*>? {
        return typeAliasForConstructor ?: (resolvedReturnTypeRef.toRegularClassSymbol(context.session))
    }
}
