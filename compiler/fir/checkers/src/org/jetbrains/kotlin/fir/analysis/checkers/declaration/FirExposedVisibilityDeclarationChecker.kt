/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolved
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

// TODO: check why coneTypeSafe is necessary at some points inside
object FirExposedVisibilityDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirAnonymousFunction -> return
            is FirTypeAlias -> checkTypeAlias(declaration, reporter, context)
            is FirProperty -> checkProperty(declaration, reporter, context)
            is FirFunction -> checkFunction(declaration, reporter, context)
            is FirRegularClass -> checkClass(declaration, reporter, context)
        }
    }

    private fun checkClass(declaration: FirRegularClass, reporter: DiagnosticReporter, context: CheckerContext) {
        checkSupertypes(declaration, reporter, context)
        checkParameterBounds(declaration, reporter, context)
    }

    private fun checkSupertypes(declaration: FirRegularClass, reporter: DiagnosticReporter, context: CheckerContext) {
        val classVisibility = declaration.effectiveVisibility

        if (classVisibility == EffectiveVisibility.Local) return
        val supertypes = declaration.superTypeRefs
        val isInterface = declaration.classKind == ClassKind.INTERFACE
        for (supertypeRef in supertypes) {
            val supertype = supertypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
            val clazz = supertype.toRegularClass(context.session) ?: continue
            val superIsInterface = clazz.classKind == ClassKind.INTERFACE
            if (superIsInterface != isInterface) {
                continue
            }
            val restricting = supertype.findVisibilityExposure(context, classVisibility)
            if (restricting != null) {
                reporter.reportOn(
                    supertypeRef.source ?: declaration.source,
                    if (isInterface) FirErrors.EXPOSED_SUPER_INTERFACE else FirErrors.EXPOSED_SUPER_CLASS,
                    classVisibility,
                    restricting,
                    restricting.effectiveVisibility,
                    context
                )
            }
        }
    }

    private fun checkParameterBounds(declaration: FirRegularClass, reporter: DiagnosticReporter, context: CheckerContext) {
        val classVisibility = declaration.effectiveVisibility

        if (classVisibility == EffectiveVisibility.Local) return
        for (parameter in declaration.typeParameters) {
            for (bound in parameter.symbol.fir.bounds) {
                val restricting = bound.coneType.findVisibilityExposure(context, classVisibility)
                if (restricting != null) {
                    reporter.reportOn(
                        bound.source,
                        FirErrors.EXPOSED_TYPE_PARAMETER_BOUND,
                        classVisibility,
                        restricting,
                        restricting.effectiveVisibility,
                        context
                    )
                }
            }
        }
    }

    private fun checkTypeAlias(declaration: FirTypeAlias, reporter: DiagnosticReporter, context: CheckerContext) {
        val expandedType = declaration.expandedConeType
        val typeAliasVisibility = declaration.effectiveVisibility

        if (typeAliasVisibility == EffectiveVisibility.Local) return
        val restricting = expandedType?.findVisibilityExposure(context, typeAliasVisibility)
        if (restricting != null) {
            reporter.reportOn(
                declaration.source,
                FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE,
                typeAliasVisibility,
                restricting,
                restricting.effectiveVisibility,
                context
            )
        }
    }

    private fun checkFunction(declaration: FirFunction, reporter: DiagnosticReporter, context: CheckerContext) {
        val functionVisibility = (declaration as FirMemberDeclaration).effectiveVisibility

        if (functionVisibility == EffectiveVisibility.Local) return
        if (declaration !is FirConstructor && declaration !is FirPropertyAccessor) {
            val restricting = declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?.findVisibilityExposure(context, functionVisibility)
            if (restricting != null) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.EXPOSED_FUNCTION_RETURN_TYPE,
                    functionVisibility,
                    restricting,
                    restricting.effectiveVisibility,
                    context
                )
            }
        }
        declaration.valueParameters.forEachIndexed { i, valueParameter ->
            if (i < declaration.valueParameters.size) {
                val restricting =
                    valueParameter.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                        ?.findVisibilityExposure(context, functionVisibility)
                if (restricting != null) {
                    reporter.reportOn(
                        valueParameter.source,
                        FirErrors.EXPOSED_PARAMETER_TYPE,
                        functionVisibility,
                        restricting,
                        restricting.effectiveVisibility,
                        context
                    )
                }
            }
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration as? FirCallableDeclaration, reporter, context)
    }

    private fun checkProperty(declaration: FirProperty, reporter: DiagnosticReporter, context: CheckerContext) {
        if (declaration.isLocal) return
        val propertyVisibility = declaration.effectiveVisibility

        if (propertyVisibility == EffectiveVisibility.Local) return
        val restricting =
            declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?.findVisibilityExposure(context, propertyVisibility)
        if (restricting != null) {
            val diagnostic = if (declaration.fromPrimaryConstructor == true) {
                FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR
            } else {
                FirErrors.EXPOSED_PROPERTY_TYPE
            }
            reporter.reportOn(
                declaration.source,
                diagnostic,
                propertyVisibility,
                restricting,
                restricting.effectiveVisibility,
                context
            )
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration, reporter, context)
    }

    private fun checkMemberReceiver(
        typeRef: FirTypeRef?,
        memberDeclaration: FirCallableDeclaration?,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        if (typeRef == null || memberDeclaration == null) return
        val receiverParameterType = typeRef.coneType
        val memberVisibility = memberDeclaration.effectiveVisibility

        if (memberVisibility == EffectiveVisibility.Local) return
        val restricting = receiverParameterType.findVisibilityExposure(context, memberVisibility)
        if (restricting != null) {
            reporter.reportOn(
                typeRef.source,
                FirErrors.EXPOSED_RECEIVER_TYPE,
                memberVisibility,
                restricting,
                restricting.effectiveVisibility,
                context
            )
        }
    }

    private fun ConeKotlinType.findVisibilityExposure(
        context: CheckerContext,
        base: EffectiveVisibility
    ): FirMemberDeclaration? {
        val type = this as? ConeClassLikeType ?: return null
        val fir = type.fullyExpandedType(context.session).lookupTag.toSymbol(context.session)?.let { firSymbol ->
            firSymbol.ensureResolved(FirResolvePhase.DECLARATIONS, context.session)
            firSymbol.fir
        } ?: return null

        if (fir is FirMemberDeclaration) {
            val effectiveVisibility = fir.effectiveVisibility
            when (effectiveVisibility.relation(base, context.session.typeContext)) {
                EffectiveVisibility.Permissiveness.LESS,
                EffectiveVisibility.Permissiveness.UNKNOWN -> {
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
}
