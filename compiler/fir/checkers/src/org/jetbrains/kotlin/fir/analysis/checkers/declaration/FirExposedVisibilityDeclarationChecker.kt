/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory4
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isFromSealedClass
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
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
        checkParameterBounds(declaration, declaration.effectiveVisibility, reporter, context)
    }

    private fun checkSupertypes(declaration: FirRegularClass, reporter: DiagnosticReporter, context: CheckerContext) {
        val classVisibility = declaration.effectiveVisibility

        if (classVisibility == EffectiveVisibility.Local) return
        val supertypes = declaration.superTypeRefs
        val isInterface = declaration.classKind == ClassKind.INTERFACE
        for (supertypeRef in supertypes) {
            if (supertypeRef.source?.kind == KtFakeSourceElementKind.EnumSuperTypeRef) continue
            val supertype = supertypeRef.coneType
            val classSymbol = supertype.toRegularClassSymbol(context.session) ?: continue
            val superIsInterface = classSymbol.classKind == ClassKind.INTERFACE
            if (superIsInterface != isInterface) {
                continue
            }
            val (restricting, restrictingVisibility, relation) = supertype.findVisibilityExposure(context, classVisibility) ?: continue
            reporter.reportOn(
                supertypeRef.source ?: declaration.source,
                if (isInterface) FirErrors.EXPOSED_SUPER_INTERFACE else FirErrors.EXPOSED_SUPER_CLASS,
                classVisibility,
                restricting,
                relation,
                restrictingVisibility,
                context
            )
        }
    }

    private fun checkParameterBounds(
        declaration: FirTypeParameterRefsOwner,
        visibility: EffectiveVisibility,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        if (visibility == EffectiveVisibility.Local || declaration is FirConstructor) return

        fun getDiagnosticByFeature(
            feature: LanguageFeature
        ): KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> {
            val reportProperError = context.languageVersionSettings.supportsFeature(feature)
            return when {
                reportProperError || declaration is FirRegularClass -> FirErrors.EXPOSED_TYPE_PARAMETER_BOUND
                else -> FirErrors.EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING
            }
        }

        val diagnosticForNonInternalBounds = getDiagnosticByFeature(LanguageFeature.ReportExposedTypeForMoreCasesOfTypeParameterBounds)
        val diagnosticForInternalBounds = getDiagnosticByFeature(LanguageFeature.ReportExposedTypeForInternalTypeParameterBounds)

        fun FirResolvedTypeRef.findVisibilityExposure(ignoreInternalExposure: Boolean): SymbolWithRelation? =
            coneType.findVisibilityExposure(context, visibility, ignoreInternalExposure)

        for (parameter in declaration.typeParameters) {
            for (bound in parameter.symbol.resolvedBounds) {
                // If there's an exposure due to a private type, let it overtake the exposure due to an internal type.
                val (symbolWithRelation, diagnostic) =
                    bound.findVisibilityExposure(ignoreInternalExposure = true)?.to(diagnosticForNonInternalBounds)
                        ?: bound.findVisibilityExposure(ignoreInternalExposure = false)?.to(diagnosticForInternalBounds)
                        ?: continue

                val (restricting, restrictingVisibility, relation) = symbolWithRelation
                reporter.reportOn(
                    bound.source,
                    diagnostic,
                    visibility,
                    restricting,
                    relation,
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
        checkParameterBounds(declaration, typeAliasVisibility, reporter, context)
        val (restricting, restrictingVisibility, relation) = expandedType?.findVisibilityExposure(context, typeAliasVisibility) ?: return
        reporter.reportOn(
            declaration.source,
            FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE,
            typeAliasVisibility,
            restricting,
            relation,
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
                    .findVisibilityExposure(context, functionVisibility)?.let { (restricting, restrictingVisibility, relation) ->
                        reporter.reportOn(
                            declaration.source,
                            FirErrors.EXPOSED_FUNCTION_RETURN_TYPE,
                            functionVisibility,
                            restricting,
                            relation,
                            restrictingVisibility,
                            context
                        )
                    }
            }

            for (valueParameter in declaration.valueParameters) {
                valueParameter.checkExposure(functionVisibility, reporter, context)
            }
            for (valueParameter in declaration.contextParameters) {
                valueParameter.checkExposure(functionVisibility, reporter, context)
            }
        }

        if (isNonLocal) {
            checkMemberReceiver(declaration.receiverParameter?.typeRef, declaration as? FirCallableDeclaration, reporter, context)
        }

        checkParameterBounds(declaration, functionVisibility, reporter, context)
    }

    private fun checkProperty(declaration: FirProperty, reporter: DiagnosticReporter, context: CheckerContext) {
        if (declaration.fromPrimaryConstructor == true) return
        if (declaration.isLocal) return
        if (declaration.source?.kind == KtFakeSourceElementKind.EnumGeneratedDeclaration) return
        val propertyVisibility = declaration.effectiveVisibility

        if (propertyVisibility == EffectiveVisibility.Local || declaration.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty) {
            return
        }
        declaration.returnTypeRef.coneType
            .findVisibilityExposure(context, propertyVisibility)?.let { (restricting, restrictingVisibility, relation) ->
                reporter.reportOn(
                    declaration.source,
                    FirErrors.EXPOSED_PROPERTY_TYPE,
                    propertyVisibility,
                    restricting,
                    relation,
                    restrictingVisibility,
                    context
                )
            }
        checkMemberReceiver(declaration.receiverParameter?.typeRef, declaration, reporter, context)
        checkParameterBounds(declaration, propertyVisibility, reporter, context)
        for (parameter in declaration.contextParameters) {
            parameter.checkExposure(propertyVisibility, reporter, context)
        }
    }

    private fun FirValueParameter.checkExposure(
        declarationVisibility: EffectiveVisibility,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        if (declarationVisibility != EffectiveVisibility.Local) {
            returnTypeRef.coneType
                .findVisibilityExposure(context, declarationVisibility)?.let { (restricting, restrictingVisibility, relation) ->
                    reporter.reportOn(
                        source,
                        FirErrors.EXPOSED_PARAMETER_TYPE,
                        declarationVisibility,
                        restricting,
                        relation,
                        restrictingVisibility,
                        context
                    )
                    return
                }
        }

        val property = correspondingProperty ?: return
        if (property.isLocal) return
        val propertyVisibility = property.effectiveVisibility

        if (propertyVisibility == EffectiveVisibility.Local) return
        property.returnTypeRef.coneType
            .findVisibilityExposure(context, propertyVisibility)?.let { (restricting, restrictingVisibility, relation) ->
                reporter.reportOn(
                    source,
                    FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR,
                    propertyVisibility,
                    restricting,
                    relation,
                    restrictingVisibility,
                    context
                )
            }
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
        val (restricting, restrictingVisibility, relation) = receiverParameterType.findVisibilityExposure(context, memberVisibility)
            ?: return
        reporter.reportOn(
            typeRef.source,
            FirErrors.EXPOSED_RECEIVER_TYPE,
            memberVisibility,
            restricting,
            relation,
            restrictingVisibility,
            context
        )
    }

    private fun ConeKotlinType.findVisibilityExposure(
        context: CheckerContext,
        base: EffectiveVisibility,
        ignoreInternalExposure: Boolean = false,
        visitedTypes: MutableSet<ConeKotlinType> = mutableSetOf(),
    ): SymbolWithRelation? {
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

        if (ignoreInternalExposure && effectiveVisibility == EffectiveVisibility.Internal) {
            return null
        }

        if (effectiveVisibility != null) {
            when (effectiveVisibility.relation(base, context.session.typeContext)) {
                EffectiveVisibility.Permissiveness.LESS,
                EffectiveVisibility.Permissiveness.UNKNOWN,
                    -> return symbolWithRelation(classSymbol, effectiveVisibility, fromTypeArgument = visitedTypes.size > 1)
                EffectiveVisibility.Permissiveness.SAME,
                EffectiveVisibility.Permissiveness.MORE,
                    -> {
                }
            }
        }

        for ((index, it) in type.typeArguments.withIndex()) {
            when (it) {
                is ConeClassLikeType -> it.findVisibilityExposure(context, base, ignoreInternalExposure, visitedTypes)
                    ?.let { return it }
                is ConeKotlinTypeProjection -> it.type.findVisibilityExposure(context, base, ignoreInternalExposure, visitedTypes)
                    ?.let { return it }
                is ConeStarProjection -> type.toRegularClassSymbol(context.session)
                    ?.typeParameterSymbols?.getOrNull(index)
                    ?.resolvedBounds?.firstNotNullOfOrNull {
                        it.coneType.findVisibilityExposure(context, base, ignoreInternalExposure, visitedTypes)
                    }
                    ?.let { return it }
            }
        }

        return null
    }

    private fun symbolWithRelation(
        symbol: FirClassLikeSymbol<*>,
        effectiveVisibility: EffectiveVisibility,
        fromTypeArgument: Boolean,
    ): SymbolWithRelation {
        val visibility = effectiveVisibility.toVisibility()
        var lowestVisibility = symbol.visibility
        var lowestRepresentative = symbol
        var currentSymbol: FirClassLikeSymbol<*>? = symbol.getContainingClassSymbol()
        while (currentSymbol != null && lowestVisibility != visibility) {
            val compareResult = currentSymbol.visibility.compareTo(lowestVisibility)
            lowestVisibility = if (compareResult != null && compareResult < 0) {
                lowestRepresentative = currentSymbol
                currentSymbol.visibility
            } else {
                lowestVisibility
            }
            currentSymbol = currentSymbol.getContainingClassSymbol()
        }
        val defaultRelation = if (fromTypeArgument) RelationToType.ARGUMENT else RelationToType.CONSTRUCTOR
        return SymbolWithRelation(
            lowestRepresentative, effectiveVisibility,
            if (lowestRepresentative !== symbol) defaultRelation.containerRelation() else defaultRelation
        )
    }

    private data class SymbolWithRelation(
        val symbol: FirClassLikeSymbol<*>, val visibility: EffectiveVisibility, val relation: RelationToType
    )
}

