/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory3
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

// TODO: check why coneTypeSafe is necessary at some points inside
object FirExposedVisibilityChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirTypeAlias -> checkTypeAlias(declaration, reporter)
            is FirProperty -> checkProperty(declaration, reporter)
            is FirFunction<*> -> checkFunction(declaration, reporter)
            is FirRegularClass -> checkClass(declaration, reporter)
        }
    }

    private fun checkClass(declaration: FirRegularClass, reporter: DiagnosticReporter) {
        checkSupertypes(declaration, reporter)
        checkParameterBounds(declaration, reporter)
    }

    private fun checkSupertypes(declaration: FirRegularClass, reporter: DiagnosticReporter) {
        val classVisibility = declaration.effectiveVisibility
        val supertypes = declaration.superTypeRefs
        val isInterface = declaration.classKind == ClassKind.INTERFACE
        for (supertypeRef in supertypes) {
            val supertype = supertypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
            val clazz = supertype.toRegularClass(declaration.session) ?: continue
            val superIsInterface = clazz.classKind == ClassKind.INTERFACE
            if (superIsInterface != isInterface) {
                continue
            }
            val restricting = supertype.findVisibilityExposure(declaration.session, classVisibility)
            if (restricting != null) {
                reporter.reportExposure(
                    if (isInterface) FirErrors.EXPOSED_SUPER_INTERFACE else FirErrors.EXPOSED_SUPER_CLASS,
                    restricting,
                    classVisibility,
                    restricting.effectiveVisibility,
                    supertypeRef.source ?: declaration.source
                )
            }
        }
    }

    private fun checkParameterBounds(declaration: FirRegularClass, reporter: DiagnosticReporter) {
        val classVisibility = declaration.effectiveVisibility
        for (parameter in declaration.typeParameters) {
            for (bound in parameter.symbol.fir.bounds) {
                val restricting = bound.coneType.findVisibilityExposure(declaration.session, classVisibility)
                if (restricting != null) {
                    reporter.reportExposure(
                        FirErrors.EXPOSED_TYPE_PARAMETER_BOUND,
                        restricting,
                        classVisibility,
                        restricting.effectiveVisibility,
                        bound.source
                    )
                }
            }
        }
    }

    private fun checkTypeAlias(declaration: FirTypeAlias, reporter: DiagnosticReporter) {
        val expandedType = declaration.expandedConeType
        val typeAliasVisibility = declaration.firEffectiveVisibility(declaration.session)
        val restricting = expandedType?.findVisibilityExposure(declaration.session, typeAliasVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE,
                restricting,
                typeAliasVisibility,
                restricting.effectiveVisibility,
                declaration.source?.getIdentifierSource() ?: declaration.source
            )
        }
    }

    private fun checkFunction(declaration: FirFunction<*>, reporter: DiagnosticReporter) {
        val functionVisibility = (declaration as FirMemberDeclaration).effectiveVisibility
        if (declaration !is FirConstructor) {
            val restricting = declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?.findVisibilityExposure(declaration.session, functionVisibility)
            if (restricting != null) {
                reporter.reportExposure(
                    FirErrors.EXPOSED_FUNCTION_RETURN_TYPE,
                    restricting,
                    functionVisibility,
                    restricting.effectiveVisibility,
                    declaration.source?.getIdentifierSource() ?: declaration.source
                )
            }
        }
        declaration.valueParameters.forEachIndexed { i, valueParameter ->
            if (i < declaration.valueParameters.size) {
                val restricting =
                    valueParameter.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                        ?.findVisibilityExposure(declaration.session, functionVisibility)
                if (restricting != null) {
                    reporter.reportExposure(
                        FirErrors.EXPOSED_PARAMETER_TYPE,
                        restricting,
                        functionVisibility,
                        restricting.effectiveVisibility,
                        valueParameter.source
                    )
                }
            }
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration as? FirCallableMemberDeclaration<*>, reporter)
    }

    private fun checkProperty(declaration: FirProperty, reporter: DiagnosticReporter) {
        val propertyVisibility = declaration.effectiveVisibility
        val restricting =
            declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?.findVisibilityExposure(declaration.session, propertyVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_PROPERTY_TYPE,
                restricting,
                propertyVisibility,
                restricting.effectiveVisibility,
                declaration.source?.getIdentifierSource() ?: declaration.source
            )
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration, reporter)
    }

    private fun checkMemberReceiver(
        typeRef: FirTypeRef?,
        memberDeclaration: FirCallableMemberDeclaration<*>?, reporter: DiagnosticReporter
    ) {
        if (typeRef == null || memberDeclaration == null) return
        val receiverParameterType = typeRef.coneType
        val memberVisibility = memberDeclaration.effectiveVisibility
        val restricting = receiverParameterType.findVisibilityExposure(memberDeclaration.session, memberVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_RECEIVER_TYPE,
                restricting,
                memberVisibility,
                restricting.effectiveVisibility,
                typeRef.source
            )
        }
    }

    private fun ConeKotlinType.findVisibilityExposure(session: FirSession, base: FirEffectiveVisibility): FirMemberDeclaration? {
        val type = this as? ConeClassLikeType
            ?: return null
        val fir = type.lookupTag.toSymbol(session)
            ?.fir
            ?: return null

        if (
            fir is FirMemberDeclaration &&
            fir.effectiveVisibility != FirEffectiveVisibility.Default // because abstract interface List<out T> returns it here
        ) {
            when (fir.effectiveVisibility.relation(base)) {
                FirEffectiveVisibility.Permissiveness.LESS,
                FirEffectiveVisibility.Permissiveness.UNKNOWN -> {
                    return fir
                }
                else -> {
                }
            }
        }

        for (it in type.typeArguments) {
            it.safeAs<ConeClassLikeType>()?.findVisibilityExposure(session, base)?.let {
                return it
            }
        }

        return null
    }

    private inline fun <reified E : FirSourceElement, P : PsiElement> DiagnosticReporter.reportExposure(
        error: FirDiagnosticFactory3<E, P, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>,
        restrictingDeclaration: FirMemberDeclaration,
        elementVisibility: FirEffectiveVisibility,
        restrictingVisibility: FirEffectiveVisibility,
        source: FirSourceElement?
    ) {
        source?.let {
            report(
                error.on(
                    it as E,
                    elementVisibility,
                    restrictingDeclaration,
                    restrictingVisibility
                )
            )
        }
    }

    private fun FirSourceElement.getIdentifierSource() = when (this) {
        is FirPsiSourceElement<*> -> (this.psi as? PsiNameIdentifierOwner)?.nameIdentifier?.toFirPsiSourceElement()
        is FirLightSourceElement -> {
            val kidsRef = Ref<Array<LighterASTNode?>>()
            this.tree.getChildren(this.element, kidsRef)
            val identifier = kidsRef.get().find { it?.tokenType == KtTokens.IDENTIFIER }
            identifier?.toFirLightSourceElement(this.tree.getStartOffset(identifier), this.tree.getEndOffset(identifier), this.tree)
        }
    }
}