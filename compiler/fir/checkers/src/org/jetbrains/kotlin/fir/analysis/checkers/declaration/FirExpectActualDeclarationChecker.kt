/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
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
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualChecker
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.MemberIncompatibility
import org.jetbrains.kotlin.utils.addToStdlib.partitionIsInstance

@Suppress("DuplicatedCode")
object FirExpectActualDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration !is FirMemberDeclaration) return
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            if ((declaration.isExpect || declaration.isActual) && containsExpectOrActualModifier(declaration) &&
                declaration.source?.kind?.shouldSkipErrorTypeReporting == false
            ) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.NOT_A_MULTIPLATFORM_COMPILATION,
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
        val actualContainingClass = context.containingDeclarations.lastOrNull() as? FirRegularClassSymbol
        val expectContainingClass = actualContainingClass?.getSingleMatchedExpectForActualOrNull() as? FirRegularClassSymbol
        val checkingIncompatibilities = if (expectedSingleCandidate != null) {
            getCheckingCompatibility(
                symbol,
                expectedSingleCandidate,
                actualContainingClass,
                expectContainingClass,
                expectActualMatchingContext,
                context,
            )
        } else emptyList()

        checkAmbiguousExpects(symbol, matchingCompatibilityToMembersMap, symbol, context, reporter)

        val source = declaration.source
        if (!declaration.hasActualModifier() &&
            // The presence of @ExpectRefinement annotation is checked by a separate FirExpectRefinementAnnotationChecker
            !declaration.isExpect &&
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

        if (ExpectActualMatchingCompatibility.MatchedSuccessfully !in matchingCompatibilityToMembersMap ||
            expectedSingleCandidate != null &&
            declaration.hasActualModifier() &&
            expectedSingleCandidate.isFakeOverride(expectContainingClass, expectActualMatchingContext)
        ) {
            reporter.reportOn(
                source,
                FirErrors.ACTUAL_WITHOUT_EXPECT,
                symbol,
                matchingCompatibilityToMembersMap,
                context
            )
            return
        }

        val (classScopesIncompatibilities, normalIncompatibilities) =
            checkingIncompatibilities.partitionIsInstance<_, ExpectActualIncompatibility.ClassScopes<FirBasedSymbol<*>>>()

        for (incompatibility in normalIncompatibilities) {
            check(expectedSingleCandidate != null) // It can't be null, because checkingIncompatibilities is not empty
            // A nicer diagnostic for functions with default params
            if (declaration is FirFunction && incompatibility == ExpectActualIncompatibility.ActualFunctionWithOptionalParameters) {
                reporter.reportOn(declaration.source, FirErrors.ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS, context)
            } else {
                reporter.reportOn(
                    source,
                    incompatibility.toDiagnostic(),
                    expectedSingleCandidate,
                    symbol,
                    incompatibility.reason,
                    context
                )
            }
        }
        // CLASS_SCOPE incompatibilities might be confusing if class kinds or class modalities don't match
        if (normalIncompatibilities.none { it is ExpectActualIncompatibility.ClassKind || it is ExpectActualIncompatibility.Modality }) {
            for (incompatibility in classScopesIncompatibilities) {
                reportClassScopesIncompatibility(symbol, expectedSingleCandidate, incompatibility, reporter, source, context)
            }
        }
    }

    private fun reportClassScopesIncompatibility(
        symbol: FirBasedSymbol<FirDeclaration>,
        expectedSingleCandidate: FirBasedSymbol<*>?,
        checkingCompatibility: ExpectActualIncompatibility.ClassScopes<FirBasedSymbol<*>>,
        reporter: DiagnosticReporter,
        source: KtSourceElement?,
        context: CheckerContext,
    ) {
        require((symbol is FirRegularClassSymbol || symbol is FirTypeAliasSymbol) && expectedSingleCandidate is FirRegularClassSymbol) {
            "Incompatible.ClassScopes is only possible for a class or a typealias: $symbol $expectedSingleCandidate"
        }

        // Do not report "expected members have no actual ones" for those expected members, for which there's a clear
        // (albeit maybe incompatible) single actual suspect, declared in the actual class.
        // This is needed only to reduce the number of errors. Incompatibility errors for those members will be reported
        // later when this checker is called for them
        fun hasSingleActualSuspect(incompatibility: MemberIncompatibility<FirBasedSymbol<*>>): Boolean {
            @OptIn(SymbolInternals::class)
            return incompatibility.actual.fir.expectForActual?.values?.singleOrNull()?.singleOrNull() == incompatibility.expect
        }

        val nonTrivialIncompatibleMembers = checkingCompatibility.incompatibleMembers.filterNot(::hasSingleActualSuspect)

        if (nonTrivialIncompatibleMembers.isNotEmpty()) {
            val (defaultArgsIncompatibleMembers, otherIncompatibleMembers) =
                nonTrivialIncompatibleMembers.partition { it.incompatibility == ExpectActualIncompatibility.ParametersWithDefaultValuesInExpectActualizedByFakeOverride }

            if (defaultArgsIncompatibleMembers.isNotEmpty()) { // report a nicer diagnostic for DefaultArgumentsInExpectActualizedByFakeOverride
                val problematicExpectMembers = defaultArgsIncompatibleMembers
                    .map {
                        it.expect as? FirNamedFunctionSymbol
                            ?: error("${ExpectActualIncompatibility.ParametersWithDefaultValuesInExpectActualizedByFakeOverride} can be reported only for ${FirNamedFunctionSymbol::class}")
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
                for (member in otherIncompatibleMembers) {
                    reporter.reportOn(
                        source,
                        FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE,
                        symbol,
                        member.expect,
                        member.actual,
                        member.incompatibility.reason,
                        context
                    )
                }
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
    ): List<ExpectActualIncompatibility<FirBasedSymbol<*>>> =
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
        val filesWithMatchedExpects = compatibility[ExpectActualMatchingCompatibility.MatchedSuccessfully]
            .orEmpty()
            .map { it.moduleData }
            .sortedBy { it.name.asString() }
            .toList()

        if (filesWithMatchedExpects.size > 1) {
            reporter.reportOn(
                actualDeclaration.source,
                FirErrors.AMBIGUOUS_EXPECTS,
                symbol,
                filesWithMatchedExpects,
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
        return when (source?.kind) {
            null -> false
            KtFakeSourceElementKind.DataClassGeneratedMembers -> false
            KtFakeSourceElementKind.EnumGeneratedDeclaration -> false
            KtFakeSourceElementKind.ImplicitConstructor -> false
            else -> hasModifier(KtTokens.ACTUAL_KEYWORD)
        }
    }

    private fun isUnderlyingPropertyOfInlineClass(
        symbol: FirBasedSymbol<*>,
        actualContainingClass: FirRegularClassSymbol,
        platformSession: FirSession
    ): Boolean = (actualContainingClass.isInlineOrValue) &&
            symbol is FirPropertySymbol &&
            actualContainingClass.primaryConstructorIfAny(platformSession)?.valueParameterSymbols?.singleOrNull() == symbol.correspondingValueParameterFromPrimaryConstructor
}

private fun ExpectActualIncompatibility<*>.toDiagnostic() = when (this) {
    ExpectActualIncompatibility.ActualFunctionWithOptionalParameters -> error("unreachable")
    is ExpectActualIncompatibility.ClassScopes<*> -> error("unreachable")

    ExpectActualIncompatibility.ClassKind -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND
    ExpectActualIncompatibility.ClassModifiers -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS
    ExpectActualIncompatibility.ClassTypeParameterCount -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_COUNT
    ExpectActualIncompatibility.ClassTypeParameterUpperBounds -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_UPPER_BOUNDS
    ExpectActualIncompatibility.ContextParameterNames -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_CONTEXT_PARAMETER_NAMES
    ExpectActualIncompatibility.EnumEntries -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_ENUM_ENTRIES
    ExpectActualIncompatibility.FunInterfaceModifier -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER
    ExpectActualIncompatibility.FunctionModifiersDifferent -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_DIFFERENT
    ExpectActualIncompatibility.FunctionModifiersNotSubset -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_NOT_SUBSET
    ExpectActualIncompatibility.IllegalRequiresOpt -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_ILLEGAL_REQUIRES_OPT_IN
    ExpectActualIncompatibility.NestedTypeAlias -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_NESTED_TYPE_ALIAS
    ExpectActualIncompatibility.ParameterNames -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PARAMETER_NAMES
    ExpectActualIncompatibility.ParametersWithDefaultValuesInExpectActualizedByFakeOverride -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PARAMETERS_WITH_DEFAULT_VALUES_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE
    ExpectActualIncompatibility.PropertyConstModifier -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_CONST_MODIFIER
    ExpectActualIncompatibility.PropertyKind -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_KIND
    ExpectActualIncompatibility.PropertyLateinitModifier -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_LATEINIT_MODIFIER
    ExpectActualIncompatibility.ReturnType -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE
    ExpectActualIncompatibility.Supertypes -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES
    ExpectActualIncompatibility.TypeParameterNames -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_NAMES
    ExpectActualIncompatibility.TypeParameterReified -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_REIFIED
    ExpectActualIncompatibility.TypeParameterVariance -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_VARIANCE
    ExpectActualIncompatibility.ValueParameterCrossinline -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_CROSSINLINE
    ExpectActualIncompatibility.ValueParameterNoinline -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_NOINLINE
    ExpectActualIncompatibility.ValueParameterVararg -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_VARARG
    is ExpectActualIncompatibility.Modality -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_MODALITY
    is ExpectActualIncompatibility.PropertySetterVisibility -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY
    is ExpectActualIncompatibility.Visibility -> FirErrors.EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY
}
