/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.declaration.FirJsExternalChecker.isExtension
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

@OptIn(SymbolInternals::class)
object FirJsNameClashChecker : FirBasicDeclarationChecker() {
    private val nameSuggestion = FirJsNameSuggestion()

    private val scopes = mutableMapOf<FirDeclaration, MutableMap<String, FirDeclaration>>()
    private val clashedFakeOverrides = mutableMapOf<FirDeclaration, Pair<FirDeclaration, FirDeclaration>>()
    private val clashedDeclarations = mutableSetOf<Pair<FirDeclaration, String>>()

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirProperty && declaration.isExtension) {
            return
        }

        checkDeclaration(declaration, context, reporter)
    }

    private fun checkDeclaration(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirPrimaryConstructor || declaration is FirTypeAlias) return

        val immediateParent = context.containingDeclarations.lastOrNull()

        if (immediateParent !is FirClass && immediateParent !is FirFile) {
            return
        }

        for (suggested in nameSuggestion.suggestAllPossibleNames(declaration, context)) {
            if (
                suggested.stable &&
                FirDeclarationWithContext(suggested.declaration, context).presentsInGeneratedCode()
            ) {
                val scope = getScope(immediateParent, context)
                val name = suggested.names.last()
                val existing = scope[name]
                // NB: The original checker also checks if a "common diagnostic"
                // hasn't been reported previously
                if (
                    existing != null &&
                    existing != declaration &&
                    existing.isActual == declaration.isActual &&
                    existing.isExpect == declaration.isExpect
                ) {
                    reporter.reportOn(declaration.source, FirJsErrors.JS_NAME_CLASH, name, existing, context)
                    if (
                        clashedDeclarations.add(existing to name) &&
                        existing.source?.kind is KtRealSourceElementKind &&
                        existing.source != declaration.source
                    ) {
                        reporter.reportOn(existing.source, FirJsErrors.JS_NAME_CLASH, name, declaration, context)
                    }
                }
            }
        }

        if (declaration is FirClass) {
            val memberScope = declaration.unsubstitutedScope(context)
            val fakeOverrides = mutableListOf<FirBasedSymbol<*>>()

            memberScope.processAllFunctions {
                if (it.isSubstitutionOrIntersectionOverride || it.fir.dispatchReceiverType != declaration.defaultType()) {
                    fakeOverrides.add(it)
                }
            }

            memberScope.processAllProperties {
                if (it.isSubstitutionOrIntersectionOverride || it.fir.dispatchReceiverType != declaration.defaultType()) {
                    fakeOverrides.add(it)
                }
            }

            for (override in fakeOverrides) {
                val overrideFqName = nameSuggestion.suggest(FirDeclarationWithContext(override.fir, context))!!
                val scope = getScope(declaration, context)
                val name = overrideFqName.names.last()
                val existing = scope[name] as? FirCallableDeclaration
                val overrideDeclaration = overrideFqName.declaration as? FirCallableDeclaration

                if (
                    existing != null &&
                    overrideDeclaration != null &&
                    existing != overrideDeclaration &&
                    !isFakeOverridingNative(existing, context)
                ) {
                    reporter.reportOn(declaration.source, FirJsErrors.JS_FAKE_NAME_CLASH, name, override.fir, existing, context)
                }

                clashedFakeOverrides[override.fir]?.let { (firstExample, secondExample) ->
                    reporter.reportOn(declaration.source, FirJsErrors.JS_FAKE_NAME_CLASH, name, firstExample, secondExample, context)
                }
            }
        }
    }

    private fun FirJsNameSuggestion.suggestAllPossibleNames(
        declaration: FirDeclaration,
        context: CheckerContext,
    ): Collection<SuggestedName> {
        return if (declaration is FirCallableDeclaration) {
            val primary = suggest(FirDeclarationWithContext(declaration, context))
            if (primary != null) {
                val overriddenNames = declaration.getAllOverridden(context.session, context).flatMap {
                    suggestAllPossibleNames(it.fir, context).map { overridden ->
                        SuggestedName(overridden.names, overridden.stable, primary.declaration, primary.scope)
                    }
                }
                (overriddenNames + primary).distinctBy { it.names }
            } else {
                emptyList()
            }
        } else {
            listOfNotNull(suggest(FirDeclarationWithContext(declaration, context)))
        }
    }

    private val FirDeclaration.isActual
        get() = this is FirMemberDeclaration && status.isActual ||
                this is FirPropertyAccessor && propertySymbol?.fir?.status?.isActual == true

    private val FirDeclaration.isExpect
        get() = this is FirMemberDeclaration && status.isExpect ||
                this is FirPropertyAccessor && propertySymbol?.fir?.status?.isExpect == true

    private fun isFakeOverridingNative(declaration: FirCallableDeclaration, context: CheckerContext): Boolean {
        if (!declaration.isSubstitutionOrIntersectionOverride) return false

        return declaration.getAllOverridden(context.session, context).all {
            !FirDeclarationWithContext(it.fir, context).presentsInGeneratedCode()
        }
    }

    private fun getScope(declaration: FirDeclaration, context: CheckerContext) = scopes.getOrPut(declaration) {
        val scope = mutableMapOf<String, FirDeclaration>()

        when (declaration) {
            is FirFile -> {
                // NB: The original checker may receive a PackageFragmentDescriptor
                // and traverse the subpackages of the containing module
                declaration.declarations.forEach {
                    collect(it, scope, context)
                }
            }
            is FirClass -> {
                // In the FE 1.0 checker the member scope was used here,
                // but in FIR we may not get fake overrides for some cases
                // where we would have them in FE 1.0, so the member scope
                // may be empty.
                collect(declaration.unsubstitutedScope(context), scope, context)
            }
            else -> {}
        }

        scope
    }

    private fun collect(scope: FirContainingNamesAwareScope, target: MutableMap<String, FirDeclaration>, context: CheckerContext) {
        scope.processAllFunctions {
            collect(it.fir, target, context)
        }

        scope.processAllProperties {
            collect(it.fir, target, context)
        }
    }

    private fun FirProperty.hasJsNameInAccessors() = getter?.getJsName() != null || setter?.getJsName() != null

    private fun collect(
        declaration: FirDeclaration,
        target: MutableMap<String, FirDeclaration>,
        context: CheckerContext,
    ) {
        if (declaration is FirProperty) {
            if (declaration.isExtension || declaration.hasJsNameInAccessors()) {
                declaration.getter?.let { collect(it, target, context) }
                declaration.setter?.let { collect(it, target, context) }
                return
            }
        }

        for (fqName in nameSuggestion.suggestAllPossibleNames(declaration, context)) {
            if (fqName.stable && FirDeclarationWithContext(fqName.declaration, context).presentsInGeneratedCode()) {
                target[fqName.names.last()] = fqName.declaration
                (fqName.declaration as? FirCallableDeclaration)?.let { checkOverrideClashes(it, target, context) }
            }
        }
    }

    private fun checkOverrideClashes(
        declaration: FirCallableDeclaration,
        target: MutableMap<String, FirDeclaration>,
        context: CheckerContext,
    ) {
        for (overriddenSymbol in declaration.getAllOverridden(context.session, context)) {
            val overridden = overriddenSymbol.fir
            val overriddenFqName = nameSuggestion.suggest(FirDeclarationWithContext(overridden, context))!!
            if (overriddenFqName.stable) {
                val existing = target[overriddenFqName.names.last()]
                if (existing != null) {
                    if (existing != declaration/* && declaration.isSubstitutionOrIntersectionOverride*/) {
                        clashedFakeOverrides[declaration] = existing to overridden
                    }
                } else {
                    target[overriddenFqName.names.last()] = declaration
                }
            }
        }
    }

    private fun FirDeclarationWithContext<*>.presentsInGeneratedCode() = !isNativeObject() && !isLibraryObject()
}