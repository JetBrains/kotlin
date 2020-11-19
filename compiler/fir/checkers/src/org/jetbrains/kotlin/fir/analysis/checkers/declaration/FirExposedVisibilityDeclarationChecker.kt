/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getChildren
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory3
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolved
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

// TODO: check why coneTypeSafe is necessary at some points inside
object FirExposedVisibilityDeclarationChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirTypeAlias -> checkTypeAlias(declaration, reporter, context)
            is FirProperty -> checkProperty(declaration, reporter, context)
            is FirFunction<*> -> checkFunction(declaration, reporter, context)
            is FirRegularClass -> checkClass(declaration, reporter, context)
        }
    }

    private fun checkClass(declaration: FirRegularClass, reporter: DiagnosticReporter, context: CheckerContext) {
        checkSupertypes(declaration, reporter, context)
        checkParameterBounds(declaration, reporter, context)
    }

    private fun checkSupertypes(declaration: FirRegularClass, reporter: DiagnosticReporter, context: CheckerContext) {
        val classVisibility = declaration.getEffectiveVisibility(context)

        if (classVisibility == FirEffectiveVisibilityImpl.Local) return
        val supertypes = declaration.superTypeRefs
        val isInterface = declaration.classKind == ClassKind.INTERFACE
        for (supertypeRef in supertypes) {
            val supertype = supertypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
            val clazz = supertype.toRegularClass(declaration.session) ?: continue
            val superIsInterface = clazz.classKind == ClassKind.INTERFACE
            if (superIsInterface != isInterface) {
                continue
            }
            val restricting = supertype.findVisibilityExposure(context, classVisibility)
            if (restricting != null) {
                reporter.reportExposure(
                    if (isInterface) FirErrors.EXPOSED_SUPER_INTERFACE else FirErrors.EXPOSED_SUPER_CLASS,
                    restricting,
                    classVisibility,
                    restricting.getEffectiveVisibility(context),
                    supertypeRef.source ?: declaration.source
                )
            }
        }
    }

    private fun checkParameterBounds(declaration: FirRegularClass, reporter: DiagnosticReporter, context: CheckerContext) {
        val classVisibility = declaration.getEffectiveVisibility(context)

        if (classVisibility == FirEffectiveVisibilityImpl.Local) return
        for (parameter in declaration.typeParameters) {
            for (bound in parameter.symbol.fir.bounds) {
                val restricting = bound.coneType.findVisibilityExposure(context, classVisibility)
                if (restricting != null) {
                    reporter.reportExposure(
                        FirErrors.EXPOSED_TYPE_PARAMETER_BOUND,
                        restricting,
                        classVisibility,
                        restricting.getEffectiveVisibility(context),
                        bound.source
                    )
                }
            }
        }
    }

    private fun checkTypeAlias(declaration: FirTypeAlias, reporter: DiagnosticReporter, context: CheckerContext) {
        val expandedType = declaration.expandedConeType
        val typeAliasVisibility = declaration.getEffectiveVisibility(context)

        if (typeAliasVisibility == FirEffectiveVisibilityImpl.Local) return
        val restricting = expandedType?.findVisibilityExposure(context, typeAliasVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE,
                restricting,
                typeAliasVisibility,
                restricting.getEffectiveVisibility(context),
                declaration.source?.getIdentifierSource() ?: declaration.source
            )
        }
    }

    private fun checkFunction(declaration: FirFunction<*>, reporter: DiagnosticReporter, context: CheckerContext) {
        val functionVisibility = (declaration as FirMemberDeclaration).getEffectiveVisibility(context)

        if (functionVisibility == FirEffectiveVisibilityImpl.Local) return
        if (declaration !is FirConstructor) {
            val restricting = declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?.findVisibilityExposure(context, functionVisibility)
            if (restricting != null) {
                reporter.reportExposure(
                    FirErrors.EXPOSED_FUNCTION_RETURN_TYPE,
                    restricting,
                    functionVisibility,
                    restricting.getEffectiveVisibility(context),
                    declaration.source?.getIdentifierSource() ?: declaration.source
                )
            }
        }
        declaration.valueParameters.forEachIndexed { i, valueParameter ->
            if (i < declaration.valueParameters.size) {
                val restricting =
                    valueParameter.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                        ?.findVisibilityExposure(context, functionVisibility)
                if (restricting != null) {
                    reporter.reportExposure(
                        FirErrors.EXPOSED_PARAMETER_TYPE,
                        restricting,
                        functionVisibility,
                        restricting.getEffectiveVisibility(context),
                        valueParameter.source
                    )
                }
            }
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration as? FirCallableMemberDeclaration<*>, reporter, context)
    }

    private fun checkProperty(declaration: FirProperty, reporter: DiagnosticReporter, context: CheckerContext) {
        val propertyVisibility = declaration.getEffectiveVisibility(context)

        if (propertyVisibility == FirEffectiveVisibilityImpl.Local) return
        val restricting =
            declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?.findVisibilityExposure(context, propertyVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_PROPERTY_TYPE,
                restricting,
                propertyVisibility,
                restricting.getEffectiveVisibility(context),
                declaration.source?.getIdentifierSource() ?: declaration.source
            )
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration, reporter, context)
    }

    private fun checkMemberReceiver(
        typeRef: FirTypeRef?,
        memberDeclaration: FirCallableMemberDeclaration<*>?,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        if (typeRef == null || memberDeclaration == null) return
        val receiverParameterType = typeRef.coneType
        val memberVisibility = memberDeclaration.getEffectiveVisibility(context)

        if (memberVisibility == FirEffectiveVisibilityImpl.Local) return
        val restricting = receiverParameterType.findVisibilityExposure(context, memberVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_RECEIVER_TYPE,
                restricting,
                memberVisibility,
                restricting.getEffectiveVisibility(context),
                typeRef.source
            )
        }
    }

    private fun ConeKotlinType.findVisibilityExposure(
        context: CheckerContext,
        base: FirEffectiveVisibility
    ): FirMemberDeclaration? {
        val type = this as? ConeClassLikeType ?: return null
        val fir = type.fullyExpandedType(context.session).lookupTag.toSymbol(context.session)?.let { firSymbol ->
            firSymbol.ensureResolved(FirResolvePhase.DECLARATIONS, context.session)
            firSymbol.fir
        } ?: return null

        if (fir is FirMemberDeclaration) {
            when (fir.getEffectiveVisibility(context).relation(base)) {
                FirEffectiveVisibility.Permissiveness.LESS,
                FirEffectiveVisibility.Permissiveness.UNKNOWN -> {
                    return fir
                }
                else -> {
                }
            }
        }

        for (it in type.typeArguments) {
            it.safeAs<ConeClassLikeType>()?.findVisibilityExposure(context, base)?.let {
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
            val identifier = lighterASTNode.getChildren(treeStructure).find { it?.tokenType == KtTokens.IDENTIFIER }
            identifier?.toFirLightSourceElement(treeStructure)
        }
    }

    private fun FirMemberDeclaration.getEffectiveVisibility(context: CheckerContext) =
        getEffectiveVisibility(context.session, context.containingDeclarations, context.sessionHolder.scopeSession)
}
