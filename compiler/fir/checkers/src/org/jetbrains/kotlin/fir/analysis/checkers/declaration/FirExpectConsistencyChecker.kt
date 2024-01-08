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
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.types.FirTypeRef

// See old FE's [DeclarationsChecker]
object FirExpectConsistencyChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        val lastClass = context.containingDeclarations.lastOrNull() as? FirClass
        if (declaration is FirAnonymousInitializer) {
            if (lastClass?.isExpect == true) {
                reporter.reportOn(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY, context)
            }
            return
        }

        if (declaration !is FirMemberDeclaration || !declaration.isExpect) {
            return
        }

        getConstructorDelegationCall(declaration)?.let { delegatedConstructor ->
            reporter.reportOn(delegatedConstructor.source, FirErrors.EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL, context)
        }
        for (superTypeRef in getClassSuperTypeReferencesWithInitializers(declaration)) {
            reporter.reportOn(superTypeRef.source, FirErrors.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS, context)
        }
        for (propertyParameter in getConstructorProhibitedPropertyParameters(declaration, lastClass)) {
            reporter.reportOn(propertyParameter.source, FirErrors.EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER, context)
        }
        if (isProhibitedEnumConstructor(declaration, lastClass)) {
            reporter.reportOn(source, FirErrors.EXPECTED_ENUM_CONSTRUCTOR, context)
        }
        if (isProhibitedEnumEntryWithBody(declaration)) {
            reporter.reportOn(source, FirErrors.EXPECTED_ENUM_ENTRY_WITH_BODY, context)
        }
        if (isProhibitedEnumEntryWithInitializer(declaration)) {
            reporter.reportOn(source, FirErrors.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS, context)
        }

        if (isProhibitedPrivateDeclaration(declaration)) {
            reporter.reportOn(source, FirErrors.EXPECTED_PRIVATE_DECLARATION, context)
        }

        if (isProhibitedDeclarationWithBody(declaration)) {
            reporter.reportOn(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY, context)
        }
    }

    private fun getConstructorProhibitedPropertyParameters(
        declaration: FirMemberDeclaration,
        containingClass: FirClass?,
    ): List<FirValueParameter> {
        if (declaration is FirPrimaryConstructor &&
            containingClass != null && containingClass.classKind != ClassKind.ANNOTATION_CLASS && !containingClass.isInline
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

    private fun isProhibitedEnumConstructor(declaration: FirMemberDeclaration, lastClass: FirClass?): Boolean {
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
