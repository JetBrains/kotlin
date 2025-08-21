/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.SourceNavigator.Companion.withNavigator
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.valOrVarKeyword
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

// See old FE's [DeclarationsChecker]
object FirExpectConsistencyChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        val lastClass = context.containingDeclarations.lastOrNull() as? FirClassSymbol<*>
        if (declaration is FirAnonymousInitializer) {
            if (lastClass?.isExpect == true) {
                reporter.reportOn(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY)
            }
            return
        }

        if (declaration !is FirMemberDeclaration || !declaration.isExpect) {
            return
        }

        getConstructorDelegationCall(declaration)?.let { delegatedConstructor ->
            reporter.reportOn(delegatedConstructor.source, FirErrors.EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL)
        }
        for (superTypeRef in getClassSuperTypeReferencesWithInitializers(declaration)) {
            reporter.reportOn(superTypeRef.source, FirErrors.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS)
        }
        for (propertyParameter in getConstructorProhibitedPropertyParameters(declaration, lastClass)) {
            reporter.reportOn(propertyParameter.source, FirErrors.EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER)
        }
        if (isProhibitedEnumConstructor(declaration, lastClass)) {
            reporter.reportOn(source, FirErrors.EXPECTED_ENUM_CONSTRUCTOR)
        }
        if (isProhibitedEnumEntryWithBody(declaration)) {
            reporter.reportOn(source, FirErrors.EXPECTED_ENUM_ENTRY_WITH_BODY)
        }
        if (isProhibitedEnumEntryWithInitializer(declaration)) {
            reporter.reportOn(source, FirErrors.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS)
        }

        if (isProhibitedPrivateDeclaration(declaration)) {
            reporter.reportOn(source, FirErrors.EXPECTED_PRIVATE_DECLARATION)
        }

        if (isProhibitedDeclarationWithBody(declaration)) {
            reporter.reportOn(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY)
        }
    }

    private fun getConstructorProhibitedPropertyParameters(
        declaration: FirMemberDeclaration,
        containingClass: FirClassSymbol<*>?,
    ): List<FirValueParameter> {
        if (declaration is FirPrimaryConstructor &&
            containingClass != null && containingClass.classKind != ClassKind.ANNOTATION_CLASS && !containingClass.isInlineOrValue
        ) {
            return declaration.valueParameters.filter { it.source.valOrVarKeyword != null }
        }
        return emptyList()
    }

    private fun getConstructorDelegationCall(declaration: FirMemberDeclaration): FirDelegatedConstructorCall? {
        if (declaration is FirConstructor) {
            if (!declaration.isPrimary) {
                val delegatedConstructor = declaration.delegatedConstructor
                val delegatedConstructorSource = delegatedConstructor?.source
                if (delegatedConstructorSource?.kind !is KtFakeSourceElementKind) {
                    return delegatedConstructor
                }
            }
        }
        return null
    }

    private fun getClassSuperTypeReferencesWithInitializers(declaration: FirMemberDeclaration): List<FirTypeRef> {
        if (declaration !is FirRegularClass) return emptyList()
        return declaration.withNavigator {
            declaration.superTypeRefs.filter { it.isInConstructorCallee() }
        }
    }

    private fun isProhibitedPrivateDeclaration(declaration: FirMemberDeclaration): Boolean {
        return declaration !is FirConstructor && declaration !is FirPropertyAccessor && Visibilities.isPrivate(declaration.visibility)
    }

    private fun isProhibitedEnumConstructor(declaration: FirMemberDeclaration, lastClass: FirClassSymbol<*>?): Boolean {
        return declaration is FirConstructor && lastClass?.classKind == ClassKind.ENUM_CLASS
    }

    private fun isProhibitedDeclarationWithBody(declaration: FirMemberDeclaration): Boolean {
        return declaration is FirFunction && declaration.hasBody
    }

    private fun isProhibitedEnumEntryWithBody(declaration: FirMemberDeclaration): Boolean {
        return declaration is FirEnumEntry && declaration.withNavigator { declaration.hasBody() == true }
    }

    private fun isProhibitedEnumEntryWithInitializer(declaration: FirMemberDeclaration): Boolean {
        return declaration is FirEnumEntry && declaration.withNavigator { declaration.hasInitializer() == true }
    }
}
