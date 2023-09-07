/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isTailRec
import org.jetbrains.kotlin.fir.expectActualMatchingContextFactory
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualAnnotationMatchChecker
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Compatible
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Incompatible
import org.jetbrains.kotlin.resolve.multiplatform.isCompatibleOrWeaklyIncompatible

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
        checkActual: Boolean = true
    ) {
        val symbol = declaration.symbol
        val compatibilityToMembersMap = symbol.expectForActual ?: return

        checkAmbiguousExpects(symbol, compatibilityToMembersMap, symbol, context, reporter)

        val source = declaration.source
        if (!declaration.isActual) {
            if (compatibilityToMembersMap.allStrongIncompatibilities()) return

            if (Compatible in compatibilityToMembersMap) {
                if (checkActual) {
                    reporter.reportOn(source, FirErrors.ACTUAL_MISSING, context)
                }
                return
            }
        }

        val singleIncompatibility = compatibilityToMembersMap.keys.singleOrNull()
        when {
            singleIncompatibility is Incompatible.ClassScopes -> {
                require(symbol is FirRegularClassSymbol || symbol is FirTypeAliasSymbol) {
                    "Incompatible.ClassScopes is only possible for a class or a typealias: $declaration"
                }

                // Do not report "expected members have no actual ones" for those expected members, for which there's a clear
                // (albeit maybe incompatible) single actual suspect, declared in the actual class.
                // This is needed only to reduce the number of errors. Incompatibility errors for those members will be reported
                // later when this checker is called for them
                fun hasSingleActualSuspect(
                    expectedWithIncompatibility: Pair<FirBasedSymbol<*>, Map<Incompatible<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>>
                ): Boolean {
                    val (expectedMember, incompatibility) = expectedWithIncompatibility
                    val actualMember = incompatibility.values.singleOrNull()?.singleOrNull()
                    @OptIn(SymbolInternals::class)
                    return actualMember != null &&
                            !incompatibility.allStrongIncompatibilities() &&
                            actualMember.fir.expectForActual?.values?.singleOrNull()?.singleOrNull() == expectedMember
                }

                val nonTrivialUnfulfilled = singleIncompatibility.unfulfilled.filterNot(::hasSingleActualSuspect)

                if (nonTrivialUnfulfilled.isNotEmpty()) {
                    reporter.reportOn(source, FirErrors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, symbol, nonTrivialUnfulfilled, context)
                }
            }

            Compatible !in compatibilityToMembersMap -> {
                // A nicer diagnostic for functions with default params
                if (declaration is FirFunction && compatibilityToMembersMap.keys.any { it is Incompatible.ActualFunctionWithDefaultParameters }) {
                    reporter.reportOn(declaration.source, FirErrors.ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS, context)
                } else if (requireActualModifier(declaration.symbol, context.session)) {
                    reporter.reportOn(
                        source,
                        FirErrors.ACTUAL_WITHOUT_EXPECT,
                        symbol,
                        compatibilityToMembersMap,
                        context
                    )
                }
            }

            else -> {}
        }
        // We want to report errors even if a candidate is incompatible, but it's single
        val expectedSingleCandidate = compatibilityToMembersMap[Compatible]?.singleOrNull()
            ?: symbol.getSingleExpectForActualOrNull()
        if (expectedSingleCandidate != null) {
            checkIfExpectHasDefaultArgumentsAndActualizedWithTypealias(
                expectedSingleCandidate,
                symbol,
                context,
                reporter,
            )
            checkOptInAnnotation(declaration, expectedSingleCandidate, context, reporter)
            checkAnnotationsMatch(expectedSingleCandidate, symbol, context, reporter)
        }
    }

    private fun checkAmbiguousExpects(
        actualDeclaration: FirBasedSymbol<*>,
        compatibility: Map<ExpectActualCompatibility<FirBasedSymbol<*>>, List<FirBasedSymbol<*>>>,
        symbol: FirBasedSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val filesWithAtLeastWeaklyCompatibleExpects = compatibility.asSequence()
            .filter { (compatibility, _) ->
                compatibility.isCompatibleOrWeaklyIncompatible
            }
            .map { (_, members) -> members }
            .flatten()
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

    private fun checkIfExpectHasDefaultArgumentsAndActualizedWithTypealias(
        expectSymbol: FirBasedSymbol<*>,
        actualSymbol: FirBasedSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) return
        if (expectSymbol !is FirClassSymbol || actualSymbol !is FirTypeAliasSymbol) return

        val membersWithDefaultValueParameters = getMembersWithDefaultValueParametersUnlessAnnotation(expectSymbol)
        if (membersWithDefaultValueParameters.isEmpty()) return

        reporter.reportOn(
            actualSymbol.source,
            FirErrors.DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS,
            expectSymbol,
            membersWithDefaultValueParameters,
            context
        )
    }

    private fun getMembersWithDefaultValueParametersUnlessAnnotation(classSymbol: FirClassSymbol<*>): List<FirFunctionSymbol<*>> {
        val result = mutableListOf<FirFunctionSymbol<*>>()

        fun collectFunctions(classSymbol: FirClassSymbol<*>) {
            if (classSymbol.classKind == ClassKind.ANNOTATION_CLASS) {
                return
            }
            val memberScope = classSymbol.declaredMemberScope(classSymbol.moduleData.session, memberRequiredPhase = null)
            val functionsAndConstructors = memberScope
                .run { collectAllFunctions() + getDeclaredConstructors() }

            functionsAndConstructors.filterTo(result) { it.valueParameterSymbols.any(FirValueParameterSymbol::hasDefaultValue) }

            val nestedClasses = memberScope.getClassifierNames()
                .mapNotNull { memberScope.getSingleClassifier(it) as? FirClassSymbol<*> }

            for (nestedClassSymbol in nestedClasses) {
                collectFunctions(nestedClassSymbol)
            }
        }

        collectFunctions(classSymbol)
        return result
    }

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

    fun Map<out ExpectActualCompatibility<*>, *>.allStrongIncompatibilities(): Boolean {
        return keys.all { it is Incompatible.StrongIncompatible }
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
