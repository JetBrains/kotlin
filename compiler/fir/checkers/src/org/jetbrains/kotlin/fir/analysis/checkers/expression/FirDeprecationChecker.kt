/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeCallToDeprecatedOverrideOfHidden
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isTypealiasExpansion
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

object FirDeprecationChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {

    private val filteredSourceKinds: Set<KtFakeSourceElementKind> = setOf(
        KtFakeSourceElementKind.PropertyFromParameter,
        KtFakeSourceElementKind.DataClassGeneratedMembers
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        if (expression.source?.kind in filteredSourceKinds) return
        if (expression is FirAnnotation) return // checked by FirDeprecatedTypeChecker
        if (expression.isLhsOfAssignment()) return

        val calleeReference = expression.toReference(context.session) ?: return
        val resolvedReference = calleeReference.resolved ?: return
        val referencedSymbol = resolvedReference.resolvedSymbol

        if (expression.isDelegatedPropertySelfAccess(referencedSymbol)) return

        val source = resolvedReference.source ?: expression.source

        if (expression is FirDelegatedConstructorCall) {
            // Report deprecations on the constructor itself, not on the declaring class as that will be handled by FirDeprecatedTypeChecker
            val constructorOnlyDeprecation = referencedSymbol.getDeprecation(context.session, expression) ?: return
            val isTypealiasExpansion = expression.constructedTypeRef.coneType.fullyExpandedType().isTypealiasExpansion

            reportApiStatus(
                source, referencedSymbol, isTypealiasExpansion,
                constructorOnlyDeprecation
            )
        } else {
            reportApiStatusIfNeeded(source, referencedSymbol, callSite = expression)
        }

        reportCallToDeprecatedOverrideOfHidden(expression, source, referencedSymbol)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportCallToDeprecatedOverrideOfHidden(
        expression: FirStatement,
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
    ) {
        if (expression !is FirQualifiedAccessExpression) return
        if (ConeCallToDeprecatedOverrideOfHidden in expression.nonFatalDiagnostics) {
            val unwrappedSymbol = (referencedSymbol as? FirSyntheticPropertySymbol)?.getterSymbol?.delegateFunctionSymbol
                ?: referencedSymbol as? FirCallableSymbol
            val callableName = unwrappedSymbol?.callableId?.callableName?.asString()
            val message = getDeprecatedOverrideOfHiddenMessage(callableName)
            reporter.reportOn(source, FirErrors.DEPRECATION, referencedSymbol, message)
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
    context(context: CheckerContext)
    private fun FirStatement.isDelegatedPropertySelfAccess(referencedSymbol: FirBasedSymbol<*>): Boolean {
        if (source?.kind != KtFakeSourceElementKind.DelegatedPropertyAccessor) return false
        val containers = context.containingDeclarations
        val size = containers.size

        val fir = referencedSymbol.fir // we need to take .fir as FirDelegateFieldSymbol references to the fir of the property

        return containers.getOrNull(size - 1)?.fir == fir // For `provideDelegate`, the call will be in the initializer
                || containers.getOrNull(size - 2)?.fir == fir // For `getValue`, the call will be in the accessor
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun reportApiStatusIfNeeded(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        callSite: FirElement? = null,
    ) {
        val deprecation = getWorstDeprecation(callSite, referencedSymbol) ?: return
        if (referencedSymbol.isNestedTypeAliasReferenceAndRelevantDeprecation(deprecation)) {
            source.requireFeatureSupport(LanguageFeature.NestedTypeAliases)
            return
        }
        val isTypealiasExpansion = deprecation.isTypealiasExpansionOf(referencedSymbol, callSite)
        reportApiStatus(source, referencedSymbol, isTypealiasExpansion, deprecation)
    }

    private val NestedTypeAliasesSinceVersion = LanguageFeature.NestedTypeAliases.sinceVersion!!.let {
        VersionRequirement.Version(it.major, it.minor)
    }

    private fun FirBasedSymbol<*>.isNestedTypeAliasReferenceAndRelevantDeprecation(deprecation: FirDeprecationInfo): Boolean {
        when (this) {
            is FirTypeAliasSymbol -> {
                if (!classId.isNestedClass) return false
            }
            is FirConstructorSymbol -> {
                if (origin != FirDeclarationOrigin.Synthetic.TypeAliasConstructor ||
                    (resolvedReturnType.abbreviatedType as? ConeClassLikeTypeImpl)?.classId?.isNestedClass != true
                ) {
                    return false
                }
            }
            else -> return false
        }

        // Make sure it's a relevant deprecation
        return (deprecation as? RequireKotlinDeprecationInfo)?.versionRequirement?.let {
            it.kind == ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION &&
                    it.level == DeprecationLevel.ERROR &&
                    it.version == NestedTypeAliasesSinceVersion
        } == true
    }

    context(context: CheckerContext)
    private fun FirDeprecationInfo.isTypealiasExpansionOf(
        referencedSymbol: FirBasedSymbol<*>,
        callSite: FirElement?,
    ): Boolean = when (referencedSymbol) {
        is FirConstructorSymbol -> referencedSymbol.typeAliasConstructorInfo?.typeAliasSymbol
            ?.let { isTypealiasExpansionOf(it, callSite) }
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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun reportApiStatus(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        isTypealiasExpansion: Boolean,
        deprecationInfo: FirDeprecationInfo,
    ) {
        when (deprecationInfo) {
            is FutureApiDeprecationInfo -> reportApiNotAvailable(source, deprecationInfo)
            is RequireKotlinDeprecationInfo -> reportVersionRequirementDeprecation(source, referencedSymbol, deprecationInfo)
            else -> reportDeprecation(source, referencedSymbol, isTypealiasExpansion, deprecationInfo)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportVersionRequirementDeprecation(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        deprecationInfo: RequireKotlinDeprecationInfo,
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
            deprecationInfo.getMessage(context.session) ?: ""
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportDeprecation(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        isTypealiasExpansion: Boolean,
        deprecationInfo: FirDeprecationInfo,
    ) {
        if (!isTypealiasExpansion) {
            val diagnostic = when (deprecationInfo.deprecationLevel) {
                DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> FirErrors.DEPRECATION_ERROR
                DeprecationLevelValue.WARNING -> FirErrors.DEPRECATION
            }
            reporter.reportOn(source, diagnostic, referencedSymbol, deprecationInfo.getMessage(context.session) ?: "")
        } else {
            val diagnostic = when (deprecationInfo.deprecationLevel) {
                DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> FirErrors.TYPEALIAS_EXPANSION_DEPRECATION_ERROR
                DeprecationLevelValue.WARNING -> FirErrors.TYPEALIAS_EXPANSION_DEPRECATION
            }
            reporter.reportOn(source, diagnostic, referencedSymbol, referencedSymbol, deprecationInfo.getMessage(context.session) ?: "")
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportApiNotAvailable(
        source: KtSourceElement?,
        deprecationInfo: FutureApiDeprecationInfo,
    ) {
        reporter.reportOn(
            source,
            FirErrors.API_NOT_AVAILABLE,
            deprecationInfo.sinceVersion,
            context.languageVersionSettings.apiVersion
        )
    }

    context(context: CheckerContext)
    private fun getWorstDeprecation(
        callSite: FirElement?,
        symbol: FirBasedSymbol<*>,
    ): FirDeprecationInfo? {
        val deprecationInfos = listOfNotNull(
            (symbol as? FirConstructorSymbol)
                ?.classSymbolItIsCalledThrough()
                ?.getDeprecation(context.session, callSite),
            symbol.getDeprecation(context.session, callSite),
        )
        return deprecationInfos.maxOrNull()
    }

    context(context: CheckerContext)
    private fun FirConstructorSymbol.classSymbolItIsCalledThrough(): FirClassLikeSymbol<*>? {
        return typeAliasConstructorInfo?.typeAliasSymbol ?: (resolvedReturnTypeRef.toRegularClassSymbol(context.session))
    }
}
