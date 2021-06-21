/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OVERRIDING_FINAL_MEMBER_BY_DELEGATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.util.ImplementationStatus

object FirNotImplementedOverrideChecker : FirClassChecker() {

    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        val sourceKind = source.kind
        if (sourceKind is FirFakeSourceElementKind && sourceKind != FirFakeSourceElementKind.EnumInitializer) return
        val modality = declaration.modality()
        val canHaveAbstractDeclarations = modality == Modality.ABSTRACT || modality == Modality.SEALED
        if (declaration is FirRegularClass && declaration.isExpect) return
        val classKind = declaration.classKind
        if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) return

        val classScope = declaration.unsubstitutedScope(context)

        val notImplementedSymbols = mutableListOf<FirCallableSymbol<*>>()
        val notImplementedIntersectionSymbols = mutableListOf<FirCallableSymbol<*>>()
        val invisibleSymbols = mutableListOf<FirCallableSymbol<*>>()

        fun collectSymbol(symbol: FirCallableSymbol<*>) {
            val fir = symbol.fir as? FirCallableMemberDeclaration<*> ?: return
            when (fir.getImplementationStatus(context.sessionHolder, declaration)) {
                ImplementationStatus.AMBIGUOUSLY_INHERITED -> notImplementedIntersectionSymbols.add(symbol)
                ImplementationStatus.NOT_IMPLEMENTED -> when {
                    fir.isVisibleInClass(declaration) -> notImplementedSymbols.add(symbol)
                    else -> invisibleSymbols.add(symbol)
                }
                else -> {
                    // nothing to do
                }
            }
        }

        for (name in classScope.getCallableNames()) {
            classScope.processFunctionsByName(name, ::collectSymbol)
            classScope.processPropertiesByName(name, ::collectSymbol)
        }

        if (!canHaveAbstractDeclarations && notImplementedSymbols.isNotEmpty()) {
            val notImplemented = notImplementedSymbols.first().unwrapFakeOverrides().fir
            if (notImplemented.isFromInterfaceOrEnum(context)) {
                reporter.reportOn(source, ABSTRACT_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
            } else {
                reporter.reportOn(source, ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
            }
        }
        if (!canHaveAbstractDeclarations && invisibleSymbols.isNotEmpty()) {
            val invisible = invisibleSymbols.first().fir
            if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses)) {
                reporter.reportOn(source, INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER, declaration, invisible, context)
            } else {
                reporter.reportOn(source, INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING, declaration, invisible, context)
            }
        }
        if (notImplementedIntersectionSymbols.isNotEmpty()) {
            var overridingFinalByDelegationReported = false
            var manyMemberNotImplementedReported = false
            var delegatedHidesSupertypeReported = false
            for (notImplementedIntersectionSymbol in notImplementedIntersectionSymbols) {
                val notImplementedIntersection = notImplementedIntersectionSymbol.fir
                val intersections = (notImplementedIntersectionSymbol as FirIntersectionCallableSymbol).intersections
                val delegatedIntersected = intersections.find {
                    val fir = it.fir as FirCallableMemberDeclaration
                    fir.origin == FirDeclarationOrigin.Delegated
                }
                if (delegatedIntersected != null) {
                    val finalIntersected = intersections.find { (it.fir as FirCallableMemberDeclaration).modality == Modality.FINAL }
                    if (finalIntersected != null) {
                        if (!overridingFinalByDelegationReported) {
                            reporter.reportOn(
                                source,
                                OVERRIDING_FINAL_MEMBER_BY_DELEGATION,
                                delegatedIntersected.fir,
                                finalIntersected.fir,
                                context
                            )
                            overridingFinalByDelegationReported = true
                        }
                        continue
                    }
                    val notDelegatedIntersected = intersections.firstOrNull {
                        (it.fir as FirCallableMemberDeclaration).origin != FirDeclarationOrigin.Delegated
                    }
                    if (notDelegatedIntersected != null) {
                        if (!delegatedHidesSupertypeReported) {
                            reporter.reportOn(
                                source,
                                DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE,
                                delegatedIntersected.fir,
                                notDelegatedIntersected.fir,
                                context
                            )
                            delegatedHidesSupertypeReported = true
                        }
                        continue
                    }
                }
                if (manyMemberNotImplementedReported) continue
                if (intersections.any {
                        (it.containingClass()?.toSymbol(context.session)?.fir as? FirRegularClass)?.classKind == ClassKind.CLASS
                    }
                ) {
                    reporter.reportOn(source, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, declaration, notImplementedIntersection, context)
                } else {
                    reporter.reportOn(source, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED, declaration, notImplementedIntersection, context)
                }
                manyMemberNotImplementedReported = true
            }
        }
    }

    private fun FirCallableDeclaration<*>.isFromInterfaceOrEnum(context: CheckerContext): Boolean =
        (getContainingClass(context) as? FirRegularClass)?.let { it.isInterface || it.isEnumClass } == true
}
