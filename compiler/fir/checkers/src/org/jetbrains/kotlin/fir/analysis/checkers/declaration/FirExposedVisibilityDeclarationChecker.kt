/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory4
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.PermissivenessWithMigration
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.relationWithMigration
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isFromSealedClass
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*

// TODO: check why coneTypeSafe is necessary at some points inside
object FirExposedVisibilityDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        when (declaration) {
            is FirAnonymousFunction -> return
            is FirTypeAlias -> checkTypeAlias(declaration)
            is FirProperty -> checkProperty(declaration)
            is FirFunction -> checkFunction(declaration)
            is FirRegularClass -> checkClass(declaration)
            else -> {}
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkClass(declaration: FirRegularClass) {
        checkSupertypes(declaration)
        checkParameterBounds(declaration, declaration.effectiveVisibility)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkSupertypes(declaration: FirRegularClass) {
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
            supertype.findVisibilityExposure(classVisibility)
                ?.report(
                    if (isInterface) FirErrors.EXPOSED_SUPER_INTERFACE else FirErrors.EXPOSED_SUPER_CLASS,
                    supertypeRef.source ?: declaration.source,
                )
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkParameterBounds(
        declaration: FirTypeParameterRefsOwner,
        visibility: EffectiveVisibility,
    ) {
        if (visibility == EffectiveVisibility.Local || declaration is FirConstructor) return

        fun getDiagnosticByFeature(
            feature: LanguageFeature
        ): KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> {
            val reportProperError = feature.isEnabled()
            return when {
                reportProperError || declaration is FirRegularClass -> FirErrors.EXPOSED_TYPE_PARAMETER_BOUND
                else -> FirErrors.EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING
            }
        }

        val diagnosticForNonInternalBounds = getDiagnosticByFeature(LanguageFeature.ReportExposedTypeForMoreCasesOfTypeParameterBounds)
        val diagnosticForInternalBounds = getDiagnosticByFeature(LanguageFeature.ReportExposedTypeForInternalTypeParameterBounds)

        fun FirResolvedTypeRef.findVisibilityExposure(ignoreInternalExposure: Boolean): SymbolWithRelation? =
            coneType.findVisibilityExposure(visibility, ignoreInternalExposure)

        for (parameter in declaration.typeParameters) {
            for (bound in parameter.symbol.resolvedBounds) {
                // If there's an exposure due to a private type, let it overtake the exposure due to an internal type.
                val (symbolWithRelation, diagnostic) =
                    bound.findVisibilityExposure(ignoreInternalExposure = true)?.to(diagnosticForNonInternalBounds)
                        ?: bound.findVisibilityExposure(ignoreInternalExposure = false)?.to(diagnosticForInternalBounds)
                        ?: continue

                symbolWithRelation.report(diagnostic, bound.source)
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkTypeAlias(declaration: FirTypeAlias) {
        val expandedType = declaration.expandedConeType
        val typeAliasVisibility = declaration.effectiveVisibility

        if (typeAliasVisibility == EffectiveVisibility.Local) return
        checkParameterBounds(declaration, typeAliasVisibility)

        expandedType?.findVisibilityExposure(typeAliasVisibility)
            ?.report(FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE, declaration.source)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkFunction(declaration: FirFunction) {
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
                    .findVisibilityExposure(functionVisibility)
                    ?.report(FirErrors.EXPOSED_FUNCTION_RETURN_TYPE, declaration.source)
            }

            for (valueParameter in declaration.valueParameters) {
                valueParameter.checkExposure(functionVisibility)
            }
            for (valueParameter in declaration.contextParameters) {
                valueParameter.checkExposure(functionVisibility)
            }
        }

        if (isNonLocal) {
            checkMemberReceiver(declaration.receiverParameter?.typeRef, declaration as? FirCallableDeclaration)
        }

        checkParameterBounds(declaration, functionVisibility)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkProperty(declaration: FirProperty) {
        if (declaration.fromPrimaryConstructor == true) return
        if (declaration.isLocal) return
        if (declaration.source?.kind == KtFakeSourceElementKind.EnumGeneratedDeclaration) return
        val propertyVisibility = declaration.effectiveVisibility

        if (propertyVisibility == EffectiveVisibility.Local || declaration.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty) {
            return
        }

        declaration.returnTypeRef.coneType
            .findVisibilityExposure(propertyVisibility)
            ?.report(FirErrors.EXPOSED_PROPERTY_TYPE, declaration.source)

        checkMemberReceiver(declaration.receiverParameter?.typeRef, declaration)
        checkParameterBounds(declaration, propertyVisibility)
        for (parameter in declaration.contextParameters) {
            parameter.checkExposure(propertyVisibility)
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun FirValueParameter.checkExposure(
        declarationVisibility: EffectiveVisibility,
    ) {
        if (declarationVisibility != EffectiveVisibility.Local) {
            returnTypeRef.coneType
                .findVisibilityExposure(declarationVisibility)?.let {
                    it.report(
                        if (valueParameterKind == FirValueParameterKind.LegacyContextReceiver) FirErrors.EXPOSED_RECEIVER_TYPE else FirErrors.EXPOSED_PARAMETER_TYPE,
                        source,
                    )
                    return
                }
        }

        val property = correspondingProperty ?: return
        if (property.isLocal) return
        val propertyVisibility = property.effectiveVisibility

        if (propertyVisibility == EffectiveVisibility.Local) return
        property.returnTypeRef.coneType.findVisibilityExposure(propertyVisibility)
            ?.report(FirErrors.EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR, source)
    }


    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkMemberReceiver(
        typeRef: FirTypeRef?,
        memberDeclaration: FirCallableDeclaration?,
    ) {
        if (typeRef == null || memberDeclaration == null) return
        val receiverParameterType = typeRef.coneType
        val memberVisibility = memberDeclaration.effectiveVisibility

        if (memberVisibility == EffectiveVisibility.Local) return
        receiverParameterType.findVisibilityExposure(memberVisibility)
            ?.report(FirErrors.EXPOSED_RECEIVER_TYPE, typeRef.source)
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.findVisibilityExposure(
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

        val classSymbol = type.fullyExpandedType().lookupTag.toSymbol(context.session) ?: return null

        val effectiveVisibility = when (classSymbol) {
            is FirRegularClassSymbol -> classSymbol.effectiveVisibility
            is FirTypeAliasSymbol -> classSymbol.effectiveVisibility
            else -> null
        }

        if (ignoreInternalExposure && effectiveVisibility == EffectiveVisibility.Internal) {
            return null
        }

        if (effectiveVisibility != null) {
            when (val permissiveness = effectiveVisibility.relationWithMigration(base)) {
                PermissivenessWithMigration.LESS,
                PermissivenessWithMigration.UNKNOWN,
                PermissivenessWithMigration.UNKNOW_WITH_MIGRATION,
                    -> return symbolWithRelation(
                    symbol = classSymbol,
                    symbolVisibility = effectiveVisibility,
                    baseVisibility = base,
                    fromTypeArgument = visitedTypes.size > 1,
                    isMigration = permissiveness == PermissivenessWithMigration.UNKNOW_WITH_MIGRATION,
                )
                PermissivenessWithMigration.SAME,
                PermissivenessWithMigration.MORE,
                    -> {
                }
            }
        }

        for ((index, it) in type.typeArguments.withIndex()) {
            when (it) {
                is ConeClassLikeType -> it.findVisibilityExposure(base, ignoreInternalExposure, visitedTypes)
                    ?.let { return it }
                is ConeKotlinTypeProjection -> it.type.findVisibilityExposure(base, ignoreInternalExposure, visitedTypes)
                    ?.let { return it }
                is ConeStarProjection -> type.toRegularClassSymbol(context.session)
                    ?.typeParameterSymbols?.getOrNull(index)
                    ?.resolvedBounds?.firstNotNullOfOrNull {
                        it.coneType.findVisibilityExposure(base, ignoreInternalExposure, visitedTypes)
                    }
                    ?.let { return it }
            }
        }

        return null
    }

    private fun symbolWithRelation(
        symbol: FirClassLikeSymbol<*>,
        symbolVisibility: EffectiveVisibility,
        baseVisibility: EffectiveVisibility,
        fromTypeArgument: Boolean,
        isMigration: Boolean,
    ): SymbolWithRelation {
        val visibility = symbolVisibility.toVisibility()
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
            lowestRepresentative,
            symbolVisibility,
            if (lowestRepresentative !== symbol) defaultRelation.containerRelation() else defaultRelation,
            isMigration,
            baseVisibility,
        )
    }

    private data class SymbolWithRelation(
        val symbol: FirClassLikeSymbol<*>,
        val symbolVisibility: EffectiveVisibility,
        val relation: RelationToType,
        val isMigration: Boolean,
        val baseVisibility: EffectiveVisibility,
    ) {
        context(c: CheckerContext, reporter: DiagnosticReporter)
        fun report(
            factory: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility>,
            source: KtSourceElement?,
        ) {
            reporter.reportOn(
                source,
                if (isMigration) FirErrors.EXPOSED_PACKAGE_PRIVATE_TYPE_FROM_INTERNAL_WARNING else factory,
                baseVisibility,
                symbol,
                relation,
                symbolVisibility,
                factory.defaultPositioningStrategy,
            )
        }
    }
}

