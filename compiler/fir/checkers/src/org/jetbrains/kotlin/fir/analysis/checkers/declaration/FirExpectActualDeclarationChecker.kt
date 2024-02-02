/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.mpp.RegularClassSymbolMarker
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualChecker
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCheckingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

@Suppress("DuplicatedCode")
object FirExpectActualDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirMemberDeclaration) return
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            if ((declaration.isExpect || declaration.isActual) && containsExpectOrActualModifier(declaration)) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.NOT_A_MULTIPLATFORM_COMPILATION,
                    context,
                    positioningStrategy = SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER
                )
            }
            return
        }

        // This checker performs a more high level checking. It speaks in terms of "properties" and "functions".
        // It doesn't make sense to check:
        // - backing fields (fields can't be declared in expect declaration)
        // - property accessors (they will be checked as part of the properties checking)
        // - functions and setters parameters (they will be checked as part of the properties/functions checking)
        // This if is added because hasModifier(KtTokens.ACTUAL_KEYWORD) mistakenly returns `true` for these declarations (KT-63751)
        if (declaration is FirBackingField || declaration is FirPropertyAccessor || declaration is FirValueParameter) return

        if (declaration.isExpect) {
            checkExpectDeclarationModifiers(declaration, context, reporter)
            checkOptInAnnotation(declaration, declaration.symbol, context, reporter)
        }
        val matchingCompatibilityToMembersMap = declaration.symbol.expectForActual.orEmpty()
        if ((ExpectActualMatchingCompatibility.MatchedSuccessfully in matchingCompatibilityToMembersMap || declaration.hasActualModifier()) &&
            !declaration.isLocalMember // Reduce verbosity. WRONG_MODIFIER_TARGET will be reported anyway.
        ) {
            checkActualDeclarationHasExpected(declaration, context, reporter, matchingCompatibilityToMembersMap)
        }
    }

    private fun containsExpectOrActualModifier(declaration: FirMemberDeclaration): Boolean {
        return declaration.source.getModifierList()?.let { modifiers ->
            KtTokens.EXPECT_KEYWORD in modifiers || KtTokens.ACTUAL_KEYWORD in modifiers
        } ?: false
    }

    private fun checkExpectDeclarationModifiers(
        declaration: FirMemberDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        checkExpectDeclarationHasNoExternalModifier(declaration, context, reporter)
        if (declaration is FirProperty) {
            checkExpectPropertyAccessorsModifiers(declaration, context, reporter)
        }
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions) &&
            declaration is FirFunction && declaration.isTailRec
        ) {
            reporter.reportOn(declaration.source, FirErrors.EXPECTED_TAILREC_FUNCTION, context)
        }
    }

    private fun checkExpectPropertyAccessorsModifiers(
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        for (accessor in listOfNotNull(property.getter, property.setter)) {
            checkExpectPropertyAccessorModifiers(accessor, context, reporter)
        }
    }

    private fun checkExpectPropertyAccessorModifiers(
        accessor: FirPropertyAccessor,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        fun FirPropertyAccessor.isDefault(): Boolean {
            val source = source
            check(source != null) { "expect-actual matching is only possible for code with sources" }
            return source.kind == KtFakeSourceElementKind.DefaultAccessor
        }

        if (!accessor.isDefault()) {
            checkExpectDeclarationHasNoExternalModifier(accessor, context, reporter)
        }
    }

    private fun checkExpectDeclarationHasNoExternalModifier(
        declaration: FirMemberDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions) &&
            declaration.isExternal
        ) {
            reporter.reportOn(declaration.source, FirErrors.EXPECTED_EXTERNAL_DECLARATION, context)
        }
    }

    private fun checkActualDeclarationHasExpected(
        declaration: FirMemberDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        matchingCompatibilityToMembersMap: ExpectForActualMatchingData,
    ) {
        val symbol = declaration.symbol
        val expectedSingleCandidate =
            matchingCompatibilityToMembersMap[ExpectActualMatchingCompatibility.MatchedSuccessfully]?.singleOrNull()
        val expectActualMatchingContext = context.session.expectActualMatchingContextFactory.create(
            context.session, context.scopeSession,
            allowedWritingMemberExpectForActualMapping = true,
        )
        val actualContainingClass = context.containingDeclarations.lastOrNull()?.symbol as? FirRegularClassSymbol
        val expectContainingClass = actualContainingClass?.getSingleMatchedExpectForActualOrNull() as? FirRegularClassSymbol
        val checkingCompatibility = if (expectedSingleCandidate != null) {
            getCheckingCompatibility(
                symbol,
                expectedSingleCandidate,
                actualContainingClass,
                expectContainingClass,
                expectActualMatchingContext,
                context,
            )
        } else null

        checkAmbiguousExpects(symbol, matchingCompatibilityToMembersMap, symbol, context, reporter)

        val source = declaration.source
        if (!declaration.hasActualModifier() &&
            ExpectActualMatchingCompatibility.MatchedSuccessfully in matchingCompatibilityToMembersMap &&
            (actualContainingClass == null || requireActualModifier(symbol, actualContainingClass, context.session)) &&
            expectedSingleCandidate != null &&
            // Don't require 'actual' keyword on fake-overrides actualizations.
            // It's an inconsistency in the language design, but it's the way it works right now
            !expectedSingleCandidate.isFakeOverride(expectContainingClass, expectActualMatchingContext)
        ) {
            reporter.reportOn(source, FirErrors.ACTUAL_MISSING, context)
            return
        }

        when {
            checkingCompatibility is ExpectActualCheckingCompatibility.ClassScopes -> {
                reportClassScopesIncompatibility(symbol, expectedSingleCandidate, declaration, checkingCompatibility, reporter, source, context)
            }

            ExpectActualMatchingCompatibility.MatchedSuccessfully !in matchingCompatibilityToMembersMap ||
                    expectedSingleCandidate != null &&
                    declaration.hasActualModifier() &&
                    expectedSingleCandidate.isFakeOverride(expectContainingClass, expectActualMatchingContext) -> {
                reporter.reportOn(
                    source,
                    FirErrors.ACTUAL_WITHOUT_EXPECT,
                    symbol,
                    matchingCompatibilityToMembersMap,
                    context
                )
            }

            checkingCompatibility != null && checkingCompatibility != ExpectActualCheckingCompatibility.Compatible -> {
                check(expectedSingleCandidate != null) // It can't be null, because checkingCompatibility is not null
                // A nicer diagnostic for functions with default params
                if (declaration is FirFunction && checkingCompatibility == ExpectActualCheckingCompatibility.ActualFunctionWithDefaultParameters) {
                    reporter.reportOn(declaration.source, FirErrors.ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS, context)
                } else {
                    reporter.reportOn(
                        source,
                        FirErrors.ACTUAL_WITHOUT_EXPECT,
                        symbol,
                        mapOf(checkingCompatibility to listOf(expectedSingleCandidate)),
                        context
                    )
                }
            }

            else -> {}
        }
        if (expectedSingleCandidate != null) {
            checkOptInAnnotation(declaration, expectedSingleCandidate, context, reporter)
        }
    }

    private fun reportClassScopesIncompatibility(
        symbol: FirBasedSymbol<out FirDeclaration>,
        expectedSingleCandidate: FirBasedSymbol<*>?,
        declaration: FirMemberDeclaration,
        checkingCompatibility: ExpectActualCheckingCompatibility.ClassScopes<FirBasedSymbol<*>>,
        reporter: DiagnosticReporter,
        source: KtSourceElement?,
        context: CheckerContext,
    ) {
        require((symbol is FirRegularClassSymbol || symbol is FirTypeAliasSymbol) && expectedSingleCandidate is FirRegularClassSymbol) {
            "Incompatible.ClassScopes is only possible for a class or a typealias: $declaration"
        }

        // Do not report "expected members have no actual ones" for those expected members, for which there's a clear
        // (albeit maybe incompatible) single actual suspect, declared in the actual class.
        // This is needed only to reduce the number of errors. Incompatibility errors for those members will be reported
        // later when this checker is called for them
        fun hasSingleActualSuspect(
            expectedWithIncompatibility: Pair<FirBasedSymbol<*>, Map<out ExpectActualCheckingCompatibility.Incompatible<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>>,
        ): Boolean {
            val (expectedMember, incompatibility) = expectedWithIncompatibility
            val actualMember = incompatibility.values.singleOrNull()?.singleOrNull()
            @OptIn(SymbolInternals::class)
            return actualMember != null &&
                    actualMember.fir.expectForActual?.values?.singleOrNull()?.singleOrNull() == expectedMember
        }

        val nonTrivialIncompatibleMembers = checkingCompatibility.incompatibleMembers.filterNot(::hasSingleActualSuspect)

        if (nonTrivialIncompatibleMembers.isNotEmpty()) {
            val (defaultArgsIncompatibleMembers, otherIncompatibleMembers) =
                nonTrivialIncompatibleMembers.partition { it.second.contains(ExpectActualCheckingCompatibility.DefaultArgumentsInExpectActualizedByFakeOverride) }

            if (defaultArgsIncompatibleMembers.isNotEmpty()) { // report a nicer diagnostic for DefaultArgumentsInExpectActualizedByFakeOverride
                val problematicExpectMembers = defaultArgsIncompatibleMembers
                    .map {
                        it.first as? FirNamedFunctionSymbol
                            ?: error("${ExpectActualCheckingCompatibility.DefaultArgumentsInExpectActualizedByFakeOverride} can be reported only for ${FirNamedFunctionSymbol::class}")
                    }
                reporter.reportOn(
                    source,
                    FirErrors.DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE,
                    expectedSingleCandidate,
                    problematicExpectMembers,
                    context
                )
            }
            if (otherIncompatibleMembers.isNotEmpty()) {
                reporter.reportOn(source, FirErrors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, symbol, otherIncompatibleMembers, context)
            }
        }
        if (checkingCompatibility.mismatchedMembers.isNotEmpty()) {
            reporter.reportOn(
                source,
                FirErrors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS,
                symbol,
                checkingCompatibility.mismatchedMembers,
                context
            )
        }
    }

    private fun FirBasedSymbol<*>.isFakeOverride(
        expectContainingClass: FirRegularClassSymbol?,
        expectActualMatchingContext: FirExpectActualMatchingContext,
    ): Boolean = expectContainingClass != null &&
            this@isFakeOverride is FirCallableSymbol<*> &&
            with(expectActualMatchingContext) { this@isFakeOverride.isFakeOverride(expectContainingClass) }

    private fun getCheckingCompatibility(
        actualSymbol: FirBasedSymbol<*>,
        expectSymbol: FirBasedSymbol<*>,
        actualContainingClass: FirRegularClassSymbol?,
        expectContainingClass: FirRegularClassSymbol?,
        expectActualMatchingContext: FirExpectActualMatchingContext,
        context: CheckerContext,
    ): ExpectActualCompatibility<FirBasedSymbol<*>> =
        when {
            actualSymbol is FirCallableSymbol<*> && expectSymbol is FirCallableSymbol<*> -> {
                AbstractExpectActualChecker.getCallablesCompatibility(
                    expectSymbol,
                    actualSymbol,
                    expectContainingClass,
                    actualContainingClass,
                    expectActualMatchingContext,
                    context.languageVersionSettings
                )
            }
            actualSymbol is FirClassLikeSymbol<*> && expectSymbol is RegularClassSymbolMarker -> {
                AbstractExpectActualChecker.getClassifiersCompatibility(
                    expectSymbol,
                    actualSymbol,
                    expectActualMatchingContext,
                    context.languageVersionSettings
                )
            }
            else -> error("These expect/actual shouldn't have been matched by FirExpectActualResolver")
        }

    private fun checkAmbiguousExpects(
        actualDeclaration: FirBasedSymbol<*>,
        compatibility: Map<ExpectActualMatchingCompatibility, List<FirBasedSymbol<*>>>,
        symbol: FirBasedSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val filesWithAtLeastWeaklyCompatibleExpects = compatibility[ExpectActualMatchingCompatibility.MatchedSuccessfully]
            .orEmpty()
            .map { it.moduleData }
            .sortedBy { it.name.asString() }
            .toList()

        if (filesWithAtLeastWeaklyCompatibleExpects.size > 1) {
            reporter.reportOn(
                actualDeclaration.source,
                FirErrors.AMBIGUOUS_EXPECTS,
                symbol,
                filesWithAtLeastWeaklyCompatibleExpects,
                context
            )
        }
    }

    // we don't require `actual` modifier on
    //  - implicit primary constructors
    //  - data class fake members
    //  - annotation constructors, because annotation classes can only have one constructor
    //  - value class primary constructors, because value class must have primary constructor
    //  - value parameter inside primary constructor of inline class, because inline class must have one value parameter
    private fun requireActualModifier(
        declaration: FirBasedSymbol<*>,
        actualContainingClass: FirRegularClassSymbol,
        platformSession: FirSession
    ): Boolean {
        val source = declaration.source
        check(source != null) { "expect-actual matching is only possible for code with sources" }
        return source.kind != KtFakeSourceElementKind.ImplicitConstructor &&
                declaration.origin != FirDeclarationOrigin.Synthetic.DataClassMember &&
                !declaration.isAnnotationConstructor(platformSession) &&
                !declaration.isPrimaryConstructorOfInlineOrValueClass(platformSession) &&
                !isUnderlyingPropertyOfInlineClass(declaration, actualContainingClass, platformSession)
    }

    // Ideally, this function shouldn't exist KT-63751
    private fun FirElement.hasActualModifier(): Boolean {
        val source = source
        check(source != null) { "expect-actual matching is only possible for code with sources" }
        return when (source.kind) {
            KtFakeSourceElementKind.DataClassGeneratedMembers -> false
            KtFakeSourceElementKind.EnumGeneratedDeclaration -> false
            KtFakeSourceElementKind.ImplicitConstructor -> false
            else -> hasModifier(KtTokens.ACTUAL_KEYWORD) || hasModifier(KtTokens.IMPL_KEYWORD)
        }
    }

    private fun isUnderlyingPropertyOfInlineClass(
        symbol: FirBasedSymbol<*>,
        actualContainingClass: FirRegularClassSymbol,
        platformSession: FirSession
    ): Boolean = actualContainingClass.isInline &&
            symbol is FirPropertySymbol &&
            symbol.receiverParameter == null &&
            actualContainingClass.primaryConstructorSymbol(platformSession)?.valueParameterSymbols?.singleOrNull()?.name == symbol.name

    private fun checkOptInAnnotation(
        declaration: FirMemberDeclaration,
        expectDeclarationSymbol: FirBasedSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions) &&
            declaration is FirClass &&
            declaration.classKind == ClassKind.ANNOTATION_CLASS &&
            !expectDeclarationSymbol.hasAnnotation(StandardClassIds.Annotations.OptionalExpectation, context.session) &&
            declaration.hasAnnotation(OptInNames.REQUIRES_OPT_IN_CLASS_ID, context.session)
        ) {
            reporter.reportOn(declaration.source, FirErrors.EXPECT_ACTUAL_OPT_IN_ANNOTATION, context)
        }
    }
}
