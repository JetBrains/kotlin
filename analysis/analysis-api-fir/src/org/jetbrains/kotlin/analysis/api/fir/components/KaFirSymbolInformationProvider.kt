/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationTarget
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaKlibDecompiledFileAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassSymbolBase
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPackageSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseDeprecation
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSymbolInformationProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.toKaAnnotationTarget
import org.jetbrains.kotlin.analysis.api.impl.base.components.toKaLevel
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.klibFileAnnotations
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.calls.noJavaOrigin
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.ReturnValueStatus
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirSymbolInformationProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSymbolInformationProvider<KaFirSession>(), KaFirSessionComponent {
    private companion object {
        private val OBSOLETE_SYMBOL_DEPRECATION = KaBaseDeprecation(KaDeprecationLevel.HIDDEN, isPropagatedToOverrides = true)
    }

    override fun deprecation(symbol: KaSymbol): KaDeprecation? = withValidityAssertion {
        if (symbol is KaFirPackageSymbol || symbol is KaReceiverParameterSymbol) return null
        require(symbol is KaFirSymbol<*>) { "${symbol::class}" }

        // Optimization: Avoid building `firSymbol` of `KtFirPsiJavaClassSymbol` if it definitely isn't deprecated.
        if (symbol is KaFirPsiJavaClassSymbol && !symbol.mayHaveDeprecation()) {
            return null
        }

        val firSymbol = symbol.firSymbol

        // A symbol exists for compatibility reasons and should never be referenced in user code
        val isObsoleteSymbol = (firSymbol.fir as? FirCallableDeclaration)?.hiddenEverywhereBesideSuperCallsStatus != null
                || (firSymbol is FirSimpleSyntheticPropertySymbol && firSymbol.noJavaOrigin)

        if (isObsoleteSymbol) {
            return OBSOLETE_SYMBOL_DEPRECATION
        }

        return firSymbol.computeDeprecationInfo()?.toKaDeprecation()
    }

    private fun FirBasedSymbol<*>.computeDeprecationInfo(): FirDeprecationInfo? {
        return when (this) {
            is FirPropertySymbol -> getDeprecationForCallSite(
                analysisSession.firSession,
                AnnotationUseSiteTarget.PROPERTY,
                AnnotationUseSiteTarget.ALL,
            )

            is FirBackingFieldSymbol -> getDeprecationForCallSite(
                analysisSession.firSession,
                AnnotationUseSiteTarget.FIELD,
                AnnotationUseSiteTarget.ALL,
            )

            is FirPropertyAccessorSymbol -> propertySymbol.getDeprecationForCallSite(
                analysisSession.firSession,
                if (isGetter) AnnotationUseSiteTarget.PROPERTY_GETTER else AnnotationUseSiteTarget.PROPERTY_SETTER,
                AnnotationUseSiteTarget.PROPERTY,
                AnnotationUseSiteTarget.ALL,
            )

            else -> getDeprecationForCallSite(analysisSession.firSession, AnnotationUseSiteTarget.ALL)
        }
    }

    override fun canBeOperator(symbol: KaNamedFunctionSymbol): Boolean = withValidityAssertion {
        val functionFir = symbol.firSymbol.fir as? FirNamedFunction ?: return false
        return OperatorFunctionChecks.isOperator(
            functionFir,
            analysisSession.firSession,
            analysisSession.getScopeSessionFor(analysisSession.firSession)
        ).isSuccess
    }

    private fun KaFirPsiJavaClassSymbol.mayHaveDeprecation(): Boolean {
        if (!hasAnnotations) return false

        // Check the simple names of the Java annotations. While presence of such an annotation name does not prove deprecation, it is a
        // necessary condition for it. Type aliases are not a problem here: Java code cannot access Kotlin type aliases. (Currently,
        // deprecation annotation type aliases do not work in Kotlin, either, but this might change in the future.)
        val deprecationAnnotationSimpleNames = analysisSession.firSession.annotationPlatformSupport.deprecationAnnotationsSimpleNames
        return annotationSimpleNames.any { it != null && it in deprecationAnnotationSimpleNames }
    }

    private fun FirDeprecationInfo.toKaDeprecation(): KaDeprecation {
        return KaBaseDeprecation(deprecationLevel.toKaLevel(), propagatesToOverrides)
    }

    override fun computeAnnotationApplicableTargets(symbol: KaClassSymbol): Set<KotlinTarget>? {
        if (symbol !is KaFirNamedClassSymbolBase<*>) return null
        if (symbol.firSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null
        return symbol.firSymbol.getAllowedAnnotationTargets(analysisSession.firSession)
    }

    override fun importableFqName(symbol: KaSymbol): FqName? = withValidityAssertion {
        when (symbol) {
            is KaClassLikeSymbol -> symbol.classId?.asSingleFqName()
            is KaConstructorSymbol -> symbol.containingClassId?.takeIf {
                context(analysisSession) {
                    when (val containingDeclaration = symbol.containingDeclaration) {
                        is KaNamedClassSymbol -> !containingDeclaration.isInner
                        is KaTypeAliasSymbol -> true
                        else -> false
                    }
                }
            }?.asSingleFqName()

            is KaCallableSymbol -> {
                val callableId = symbol.callableId ?: return null
                if (callableId.classId == null) {
                    // no containing class -> top level callable
                    return callableId.asSingleFqName()
                }

                val containingClass = context(analysisSession) { symbol.containingDeclaration as? KaNamedClassSymbol } ?: return null
                val canBeImported = when (containingClass.classKind) {
                    KaClassKind.CLASS, KaClassKind.ENUM_CLASS, KaClassKind.INTERFACE, KaClassKind.ANNOTATION_CLASS -> symbol.isCompanion
                    KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT -> true
                    KaClassKind.ANONYMOUS_OBJECT -> errorWithAttachment("Anonymous object is not expected here since it cannot have ClassId") {
                        withSymbolAttachment("symbol", analysisSession, symbol)
                        withSymbolAttachment("containingClass", analysisSession, containingClass)
                    }
                }

                if (canBeImported) callableId.asSingleFqName() else null
            }

            else -> null
        }
    }

    override fun defaultAnnotationTargets(symbol: KaSymbol): Set<KaAnnotationTarget>? = withValidityAssertion {
        val firSymbol = symbol.firSymbol.fir as? FirAnnotationContainer ?: return null
        return getActualTargetList(firSymbol, rootModuleSession)
            .defaultTargets
            .mapNotNullTo(mutableSetOf()) { it.toKaAnnotationTarget() }
    }

    override fun returnValueStatus(symbol: KaNamedFunctionSymbol): KaReturnValueStatus = withValidityAssertion {
        when (symbol.firSymbol.resolvedStatus.returnValueStatus) {
            ReturnValueStatus.MustUse -> KaReturnValueStatus.MustUse
            ReturnValueStatus.ExplicitlyIgnorable -> KaReturnValueStatus.ExplicitlyIgnorable
            ReturnValueStatus.Unspecified -> KaReturnValueStatus.Unspecified
        }
    }

    override fun containingFileAnnotations(symbol: KaDeclarationSymbol): KaAnnotationList = withValidityAssertion {
        if (!symbol.isTopLevel) {
            return KaBaseEmptyAnnotationList(analysisSession.firSymbolBuilder.token)
        }

        val containingFile = with(analysisSession) { symbol.containingFile }
        if (containingFile != null) {
            return KaFirAnnotationListForDeclaration.create(containingFile.firSymbol, analysisSession.firSymbolBuilder)
        }

        /**
         * TODO Once KT-85997 is implemented, [org.jetbrains.kotlin.analysis.api.KaSession.containingFile] will be available also
         * for libraries, making this branch (as well as the entire [containingFileAnnotations] endpoint) obsolete.
         */
        return KaKlibDecompiledFileAnnotationList.create(symbol.firSymbol.klibFileAnnotations, analysisSession.firSymbolBuilder)
    }
}
