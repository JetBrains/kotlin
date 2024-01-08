/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isFromSealedClass
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*

// TODO: check why coneTypeSafe is necessary at some points inside
object FirExposedVisibilityDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirAnonymousFunction -> return
            is FirTypeAlias -> checkTypeAlias(declaration, reporter, context)
            is FirProperty -> checkProperty(declaration, reporter, context)
            is FirFunction -> checkFunction(declaration, reporter, context)
            is FirRegularClass -> checkClass(declaration, reporter, context)
            else -> {}
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
            val classSymbol = supertype.toRegularClassSymbol(context.session) ?: continue
            val superIsInterface = classSymbol.classKind == ClassKind.INTERFACE
            if (superIsInterface != isInterface) {
                continue
            }
            val (restricting, restrictingVisibility) = supertype.findVisibilityExposure(context, classVisibility) ?: continue
            reporter.reportOn(
                supertypeRef.source ?: declaration.source,
                if (isInterface) FirErrors.EXPOSED_SUPER_INTERFACE else FirErrors.EXPOSED_SUPER_CLASS,
                classVisibility,
                restricting,
                restrictingVisibility,
                context
            )
        }
    }

    private fun checkParameterBounds(declaration: FirRegularClass, reporter: DiagnosticReporter, context: CheckerContext) {
        val classVisibility = declaration.effectiveVisibility

        if (classVisibility == EffectiveVisibility.Local) return
        for (parameter in declaration.typeParameters) {
            for (bound in parameter.symbol.resolvedBounds) {
                val (restricting, restrictingVisibility) = bound.coneType.findVisibilityExposure(context, classVisibility) ?: continue
                reporter.reportOn(
                    bound.source,
                    FirErrors.EXPOSED_TYPE_PARAMETER_BOUND,
                    classVisibility,
                    restricting,
                    restrictingVisibility,
                    context
                )
            }
        }
    }

    private fun checkTypeAlias(declaration: FirTypeAlias, reporter: DiagnosticReporter, context: CheckerContext) {
        val expandedType = declaration.expandedConeType
        val typeAliasVisibility = declaration.effectiveVisibility

        if (typeAliasVisibility == EffectiveVisibility.Local) return
        val (restricting, restrictingVisibility) = expandedType?.findVisibilityExposure(context, typeAliasVisibility) ?: return
        reporter.reportOn(
            declaration.source,
            FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE,
            typeAliasVisibility,
            restricting,
            restrictingVisibility,
            context
        )
    }

    private fun checkFunction(declaration: FirFunction, reporter: DiagnosticReporter, context: CheckerContext) {
        if (declaration.source?.kind is KtFakeSourceElementKind) {
            return
        }

        var functionVisibility = (declaration as FirMemberDeclaration).effectiveVisibility
        if (declaration is FirConstructor && declaration.isFromSealedClass) {
            functionVisibility = EffectiveVisibility.PrivateInClass
        }

        val isNonLocal = functionVisibility != EffectiveVisibility.Local

        if (declaration !is FirPropertyAccessor) {
            if (isNonLocal && declaration !is FirConstructor) {
                declaration.returnTypeRef.coneType
                    .findVisibilityExposure(context, functionVisibility)?.let { (restricting, restrictingVisibility) ->
                        reporter.reportOn(
                            declaration.source,
                            FirErrors.EXPOSED_FUNCTION_RETURN_TYPE,
                            functionVisibility,
                            restricting,
                            restrictingVisibility,
                            context
                        )
                    }
            }

            for (valueParameter in declaration.valueParameters) {
                var exposureFound = false

                if (isNonLocal) {
                    valueParameter.returnTypeRef.coneType
                        .findVisibilityExposure(context, functionVisibility)?.let { (restricting, restrictingVisibility) ->
                            reporter.reportOn(
                                valueParameter.source,
                                FirErrors.EXPOSED_PARAMETER_TYPE,
                                functionVisibility,
                                restricting,
                                restrictingVisibility,
                                context
                            )
                            exposureFound = true
                        }
                }

                if (exposureFound) continue

                val property = valueParameter.correspondingProperty ?: continue
                if (property.isLocal) continue
                val propertyVisibility = property.effectiveVisibility

                if (propertyVisibility == EffectiveVisibility.Local) continue
                property.returnTypeRef.coneType
                    .findVisibilityExposure(context, propertyVisibility)?.let { (restricting, restrictingVisibility) ->
                        reporter.reportOn(
                            valueParameter.source,
                            FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR,
                            propertyVisibility,
                            restricting,
                            restrictingVisibility,
                            context
                        )
                    }
            }
        }

        if (isNonLocal) {
            checkMemberReceiver(declaration.receiverParameter?.typeRef, declaration as? FirCallableDeclaration, reporter, context)
        }
    }

    private fun checkProperty(declaration: FirProperty, reporter: DiagnosticReporter, context: CheckerContext) {
        if (declaration.fromPrimaryConstructor == true) return
        if (declaration.isLocal) return
        val propertyVisibility = declaration.effectiveVisibility

        if (propertyVisibility == EffectiveVisibility.Local) return
        declaration.returnTypeRef.coneType
            .findVisibilityExposure(context, propertyVisibility)?.let { (restricting, restrictingVisibility) ->
                reporter.reportOn(
                    declaration.source,
                    FirErrors.EXPOSED_PROPERTY_TYPE,
                    propertyVisibility,
                    restricting,
                    restrictingVisibility,
                    context
                )
            }
        checkMemberReceiver(declaration.receiverParameter?.typeRef, declaration, reporter, context)
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
        val (restricting, restrictingVisibility) = receiverParameterType.findVisibilityExposure(context, memberVisibility) ?: return
        reporter.reportOn(
            typeRef.source,
            FirErrors.EXPOSED_RECEIVER_TYPE,
            memberVisibility,
            restricting,
            restrictingVisibility,
            context
        )
    }

    private fun ConeKotlinType.findVisibilityExposure(
        context: CheckerContext,
        base: EffectiveVisibility,
        visitedTypes: MutableSet<ConeKotlinType> = mutableSetOf(),
    ): Pair<FirBasedSymbol<*>, EffectiveVisibility>? {
        if (!visitedTypes.add(this)) return null

        val type = when (this) {
            is ConeClassLikeType -> this
            is ConeFlexibleType -> lowerBound as? ConeClassLikeType ?: return null
            else -> return null
        }

        val classSymbol = type.fullyExpandedType(context.session).lookupTag.toSymbol(context.session) ?: return null

        val effectiveVisibility = when (classSymbol) {
            is FirRegularClassSymbol -> classSymbol.effectiveVisibility
            is FirTypeAliasSymbol -> classSymbol.effectiveVisibility
            else -> null
        }
        if (effectiveVisibility != null) {
            when (effectiveVisibility.relation(base, context.session.typeContext)) {
                EffectiveVisibility.Permissiveness.LESS,
                EffectiveVisibility.Permissiveness.UNKNOWN -> {
                    return classSymbol to effectiveVisibility
                }
                else -> {
                }
            }
        }

        for ((index, it) in type.typeArguments.withIndex()) {
            when (it) {
                is ConeClassLikeType -> it.findVisibilityExposure(context, base, visitedTypes)?.let { return it }
                is ConeKotlinTypeProjection -> it.type.findVisibilityExposure(context, base, visitedTypes)?.let { return it }
                is ConeStarProjection -> type.toRegularClassSymbol(context.session)
                    ?.typeParameterSymbols?.getOrNull(index)
                    ?.resolvedBounds?.firstNotNullOfOrNull { it.type.findVisibilityExposure(context, base, visitedTypes) }
                    ?.let { return it }
            }
        }

        return null
    }
}
