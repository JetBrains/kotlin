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
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isTailRec
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

@Suppress("DuplicatedCode")
object FirExpectActualDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirMemberDeclaration) return
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            if ((declaration.isExpect || declaration.isActual) && context.containingDeclarations.lastOrNull() is FirFile) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.UNSUPPORTED_FEATURE,
                    LanguageFeature.MultiPlatformProjects to context.session.languageVersionSettings,
                    context,
                    positioningStrategy = SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER
                )
            }
            return
        }
        if (declaration.isExpect) {
            checkExpectDeclarationModifiers(declaration, context, reporter)
        }
        if (declaration.isActual) {
            checkActualDeclarationHasExpected(declaration, context, reporter)
        }
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
        if (declaration is FirFunction && declaration.isTailRec) {
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
        if (declaration.isExternal) {
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
        val session = context.session

        checkAmbiguousExpects(symbol, compatibilityToMembersMap, symbol, context, reporter)

        val source = declaration.source
        if (!declaration.isActual) {
            if (compatibilityToMembersMap.allStrongIncompatibilities()) return

            if (Compatible in compatibilityToMembersMap) {
                if (checkActual && requireActualModifier(symbol, session)) {
                    reporter.reportOn(source, FirErrors.ACTUAL_MISSING, context)
                }
                return
            }
        }

        val singleIncompatibility = compatibilityToMembersMap.keys.singleOrNull()
        when {
            singleIncompatibility is Incompatible.ClassScopes -> {
                assert(symbol is FirRegularClassSymbol || symbol is FirTypeAliasSymbol) {
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
                            actualMember.isExplicitActualDeclaration() &&
                            !incompatibility.allStrongIncompatibilities() &&
                            actualMember.fir.expectForActual?.values?.singleOrNull()?.singleOrNull() == expectedMember
                }

                val nonTrivialUnfulfilled = singleIncompatibility.unfulfilled.filterNot(::hasSingleActualSuspect)

                if (nonTrivialUnfulfilled.isNotEmpty()) {
                    reporter.reportOn(source, FirErrors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, symbol, nonTrivialUnfulfilled, context)
                }
            }

            Compatible !in compatibilityToMembersMap -> {
                reporter.reportOn(
                    source,
                    FirErrors.ACTUAL_WITHOUT_EXPECT,
                    symbol,
                    compatibilityToMembersMap,
                    context
                )
            }

            else -> {
                val expected = compatibilityToMembersMap[Compatible]!!.first()
                if (expected is FirRegularClassSymbol && expected.classKind == ClassKind.ANNOTATION_CLASS) {
                    val klass = symbol.expandedClass(session)
                    val actualConstructor = klass?.declarationSymbols?.firstIsInstanceOrNull<FirConstructorSymbol>()
                    val expectedConstructor = expected.declarationSymbols.firstIsInstanceOrNull<FirConstructorSymbol>()
                    if (expectedConstructor != null && actualConstructor != null) {
                        checkAnnotationConstructors(source, expectedConstructor, actualConstructor, context, reporter)
                    }
                }
            }
        }
        val expectedSingleCandidate = symbol.getSingleExpectForActualOrNull()
        if (expectedSingleCandidate != null) {
            checkIfExpectHasDefaultArgumentsAndActualizedWithTypealias(
                expectedSingleCandidate,
                symbol,
                context,
                reporter,
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkAnnotationConstructors(
        source: KtSourceElement?,
        expected: FirConstructorSymbol,
        actual: FirConstructorSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (expectedValueParameter in expected.valueParameterSymbols) {
            // Actual parameter with the same name is guaranteed to exist because this method is only called for compatible annotations
            val actualValueDescriptor = actual.valueParameterSymbols.first { it.name == expectedValueParameter.name }

            if (expectedValueParameter.hasDefaultValue && actualValueDescriptor.hasDefaultValue) {
//              TODO
//                val expectedParameter =
//                    DescriptorToSourceUtils.descriptorToDeclaration(expectedValueParameter) as? KtParameter ?: continue
//
//                val expectedValue = trace.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expectedParameter.defaultValue)
//                    ?.toConstantValue(expectedValueParameter.type)
//
//                val actualValue =
//                    getActualAnnotationParameterValue(actualValueDescriptor, trace.bindingContext, expectedValueParameter.type)
//                if (expectedValue != actualValue) {
//                    val ktParameter = DescriptorToSourceUtils.descriptorToDeclaration(actualValueDescriptor)
//                    val target = (ktParameter as? KtParameter)?.defaultValue ?: (reportOn as? KtTypeAlias)?.nameIdentifier ?: reportOn
//                    trace.report(Errors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE.on(target, actualValueDescriptor))
//                }
            }
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
                compatibility.isCompatibleOrWeakCompatible()
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
        if (expectSymbol !is FirClassSymbol ||
            actualSymbol !is FirTypeAliasSymbol ||
            expectSymbol.classKind == ClassKind.ANNOTATION_CLASS
        ) return

        val membersWithDefaultValueParameters =
            expectSymbol.declaredMemberScope(expectSymbol.moduleData.session, memberRequiredPhase = null)
                .run { collectAllFunctions() + getDeclaredConstructors() }
                .filter { it.valueParameterSymbols.any(FirValueParameterSymbol::hasDefaultValue) }

        if (membersWithDefaultValueParameters.isEmpty()) return

        reporter.reportOn(
            actualSymbol.source,
            FirErrors.DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS,
            expectSymbol,
            membersWithDefaultValueParameters,
            context
        )
    }

    fun Map<out ExpectActualCompatibility<*>, *>.allStrongIncompatibilities(): Boolean {
        return keys.all { it is Incompatible && it.kind == IncompatibilityKind.STRONG }
    }

    private fun ExpectActualCompatibility<FirBasedSymbol<*>>.isCompatibleOrWeakCompatible(): Boolean {
        return this is Compatible ||
                this is Incompatible && kind == IncompatibilityKind.WEAK
    }

    // we don't require `actual` modifier on
    //  - annotation constructors, because annotation classes can only have one constructor
    //  - value class primary constructors, because value class must have primary constructor
    //  - value parameter inside primary constructor of inline class, because inline class must have one value parameter
    private fun requireActualModifier(declaration: FirBasedSymbol<*>, session: FirSession): Boolean {
        return !declaration.isAnnotationConstructor(session) &&
                !declaration.isPrimaryConstructorOfInlineOrValueClass(session) &&
                !isUnderlyingPropertyOfInlineClass(declaration)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun isUnderlyingPropertyOfInlineClass(declaration: FirBasedSymbol<*>): Boolean {
        // TODO
        // return declaration is PropertyDescriptor && declaration.isUnderlyingPropertyOfInlineClass()
        return false
    }

    private fun FirBasedSymbol<*>.isExplicitActualDeclaration(): Boolean {
//        return when (this) {
//            is FirConstructor -> DescriptorToSourceUtils.getSourceFromDescriptor(this) is KtConstructor<*>
//            is FirCallableMemberDeclaration<*> -> kind == CallableMemberDescriptor.Kind.DECLARATION
//            else -> true
//        }
        return true
    }
}

fun FirBasedSymbol<*>.expandedClass(session: FirSession): FirRegularClassSymbol? {
    return when (this) {
        is FirTypeAliasSymbol -> resolvedExpandedTypeRef.coneType.fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol
        is FirRegularClassSymbol -> this
        else -> null
    }
}
