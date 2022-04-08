/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

@Suppress("DuplicatedCode")
object FirExpectActualDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return
        if (declaration !is FirMemberDeclaration) return
        if (declaration.isActual) {
            checkActualDeclarationHasExpected(declaration, context, reporter)
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
                reporter.reportOn(source, FirErrors.ACTUAL_WITHOUT_EXPECT, symbol, compatibilityToMembersMap.cast(), context)
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

    fun Map<out ExpectActualCompatibility<*>, *>.allStrongIncompatibilities(): Boolean {
        return keys.all { it is Incompatible && it.kind == IncompatibilityKind.STRONG }
    }

    private fun ExpectActualCompatibility<FirBasedSymbol<*>>.isCompatibleOrWeakCompatible(): Boolean {
        return this is Compatible ||
                this is Incompatible && kind == IncompatibilityKind.WEAK
    }

    // we don't require `actual` modifier on
    //  - annotation constructors, because annotation classes can only have one constructor
    //  - inline class primary constructors, because inline class must have primary constructor
    //  - value parameter inside primary constructor of inline class, because inline class must have one value parameter
    private fun requireActualModifier(declaration: FirBasedSymbol<*>, session: FirSession): Boolean {
        return !declaration.isAnnotationConstructor(session) &&
                !declaration.isPrimaryConstructorOfInlineClass(session) &&
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
