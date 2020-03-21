/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.onSource
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

object FirExposedVisibilityChecker : FirDeclarationChecker<FirMemberDeclaration>() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirTypeAlias -> checkTypeAlias(declaration, context, reporter)
            is FirProperty -> checkProperty(declaration, context, reporter)
            is FirFunction<*> -> checkFunction(declaration, context, reporter)
        }
    }

    private fun checkTypeAlias(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        val expandedType = declaration.expandedConeType
        val typeAliasVisibility = declaration.firEffectiveVisibility(declaration.session)
        val restricting = expandedType?.leastPermissiveDescriptor(declaration.session, typeAliasVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE,
                restricting,
                typeAliasVisibility,
                restricting.firEffectiveVisibility(declaration.session),
                declaration.source
            )
        }
    }

    private fun checkFunction(declaration: FirFunction<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val functionVisibility = (declaration as FirMemberDeclaration).firEffectiveVisibility(declaration.session)
        if (declaration !is FirConstructor) {
            val restricting = declaration.returnTypeRef.coneTypeSafe<ConeClassLikeType>()
                ?.leastPermissiveDescriptor(declaration.session, functionVisibility)
            if (restricting != null) {
                reporter.reportExposure(
                    FirErrors.EXPOSED_FUNCTION_RETURN_TYPE,
                    restricting,
                    functionVisibility,
                    restricting.firEffectiveVisibility(declaration.session),
                    declaration.source
                )
            }
        }
    }

    private fun checkProperty(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val propertyVisibility = declaration.firEffectiveVisibility(declaration.session)
        val restricting =
            declaration.returnTypeRef.coneTypeSafe<ConeClassLikeType>()?.leastPermissiveDescriptor(declaration.session, propertyVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_PROPERTY_TYPE,
                restricting,
                propertyVisibility,
                restricting.firEffectiveVisibility(declaration.session),
                declaration.source
            )
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration, reporter)
    }

    private fun checkMemberReceiver(
        typeRef: FirTypeRef?,
        memberDeclaration: FirCallableMemberDeclaration<*>, reporter: DiagnosticReporter
    ) {
        if (typeRef == null) return
        val receiverParameterType = typeRef.coneTypeSafe<ConeClassLikeType>()
        val memberVisibility = memberDeclaration.firEffectiveVisibility(memberDeclaration.session)
        val restricting = receiverParameterType?.leastPermissiveDescriptor(memberDeclaration.session, memberVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_RECEIVER_TYPE,
                restricting,
                memberVisibility,
                restricting.firEffectiveVisibility(memberDeclaration.session),
                typeRef.source
            )
        }
    }

    private inline fun <reified E : PsiElement> DiagnosticReporter.reportExposure(
        error: DiagnosticFactory3<E, FirEffectiveVisibility, DeclarationWithRelation, FirEffectiveVisibility>,
        restrictingDeclaration: DeclarationWithRelation,
        elementVisibility: FirEffectiveVisibility,
        restrictingVisibility: FirEffectiveVisibility,
        source: FirSourceElement?
    ) {
        source?.let {
            report(
                error.onSource(
                    it,
                    elementVisibility,
                    restrictingDeclaration,
                    restrictingVisibility
                )
            )
        }
    }
}