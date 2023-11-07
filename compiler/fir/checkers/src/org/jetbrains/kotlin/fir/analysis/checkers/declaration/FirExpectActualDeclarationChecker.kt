/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isTailRec
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.mpp.RegularClassSymbolMarker
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualAnnotationMatchChecker
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualChecker
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCheckingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

@Suppress("DuplicatedCode")
object FirExpectActualDeclarationChecker : FirBasicDeclarationChecker() {
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
        if (declaration.isExpect) {
            checkExpectDeclarationModifiers(declaration, context, reporter)
            checkOptInAnnotation(declaration, declaration.symbol, context, reporter)
        }
        if (declaration.isActual) {
            checkActualDeclarationHasExpected(declaration, context, reporter)
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
        fun FirPropertyAccessor.isDefault() = source?.kind == KtFakeSourceElementKind.DefaultAccessor

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
        checkActual: Boolean = true,
    ) {
        val symbol = declaration.symbol
        val matchingCompatibilityToMembersMap = symbol.expectForActual ?: return
        val expectedSingleCandidate =
            matchingCompatibilityToMembersMap[ExpectActualMatchingCompatibility.MatchedSuccessfully]?.singleOrNull()
        val checkingCompatibility = if (expectedSingleCandidate != null) {
            val expectActualMatchingContext = context.session.expectActualMatchingContextFactory.create(
                context.session, context.scopeSession,
                allowedWritingMemberExpectForActualMapping = true,
            )
            val actualContainingClass = context.containingDeclarations.lastOrNull()?.symbol as? FirRegularClassSymbol
            val expectContainingClass = actualContainingClass?.getSingleMatchedExpectForActualOrNull() as? FirRegularClassSymbol
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
        if (!declaration.isActual) {
            if (checkActual && ExpectActualMatchingCompatibility.MatchedSuccessfully in matchingCompatibilityToMembersMap) {
                reporter.reportOn(source, FirErrors.ACTUAL_MISSING, context)
            }
            return
        }

        when {
            checkingCompatibility is ExpectActualCheckingCompatibility.ClassScopes -> {
                require(symbol is FirRegularClassSymbol || symbol is FirTypeAliasSymbol) {
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
                    reporter.reportOn(source, FirErrors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, symbol, nonTrivialIncompatibleMembers, context)
                } else if (checkingCompatibility.mismatchedMembers.isNotEmpty()) {
                    reporter.reportOn(source, FirErrors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, symbol, checkingCompatibility.mismatchedMembers, context)
                }
            }

            ExpectActualMatchingCompatibility.MatchedSuccessfully !in matchingCompatibilityToMembersMap &&
                    requireActualModifier(declaration.symbol, context.session) -> {
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
                } else if (requireActualModifier(declaration.symbol, context.session)) {
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
            checkAnnotationsMatch(expectedSingleCandidate, symbol, context, reporter)
        }
    }

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

    // TODO(Roman.Efremov): KT-62559 prevent reporting ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT twice in CLI mode in K2
    @OptIn(InternalDiagnosticFactoryMethod::class)
    private fun checkAnnotationsMatch(
        expectSymbol: FirBasedSymbol<*>,
        actualSymbol: FirBasedSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) return
        val matchingContext = context.session.expectActualMatchingContextFactory.create(context.session, context.scopeSession)
        val incompatibility =
            AbstractExpectActualAnnotationMatchChecker.areAnnotationsCompatible(expectSymbol, actualSymbol, matchingContext) ?: return
        val actualAnnotationTargetSourceElement = (incompatibility.actualAnnotationTargetElement as FirSourceElement).element

        reporter.report(
            FirErrors.ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT.on(
                actualSymbol.source.requireNotNull(),
                incompatibility.expectSymbol as FirBasedSymbol<*>,
                incompatibility.actualSymbol as FirBasedSymbol<*>,
                actualAnnotationTargetSourceElement,
                incompatibility.type.mapAnnotationType { it.annotationSymbol as FirAnnotation },
                positioningStrategy = null,
            ),
            context,
        )
    }

    // we don't require `actual` modifier on
    //  - annotation constructors, because annotation classes can only have one constructor
    //  - value class primary constructors, because value class must have primary constructor
    //  - value parameter inside primary constructor of inline class, because inline class must have one value parameter
    private fun requireActualModifier(declaration: FirBasedSymbol<*>, session: FirSession): Boolean {
        return !declaration.isAnnotationConstructor(session) &&
                !declaration.isPrimaryConstructorOfInlineOrValueClass(session)
    }

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
