/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.components.KaSymbolInformationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassSymbolBase
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPackageSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.SimpleDeprecationInfo

internal class KaFirSymbolInformationProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaSymbolInformationProvider, KaFirSessionComponent {
    override val KaSymbol.deprecationStatus: DeprecationInfo?
        get() = withValidityAssertion {
            if (this is KaFirPackageSymbol || this is KaReceiverParameterSymbol) return null
            require(this is KaFirSymbol<*>) { "${this::class}" }

            // Optimization: Avoid building `firSymbol` and resolve if definitely not deprecated
            if (deprecationsAreDefinitelyEmpty()) {
                return null
            }

            return when (firSymbol) {
                is FirPropertySymbol -> {
                    firSymbol.getDeprecationForCallSite(analysisSession.firSession, AnnotationUseSiteTarget.PROPERTY)
                }
                is FirBackingFieldSymbol -> {
                    firSymbol.getDeprecationForCallSite(analysisSession.firSession, AnnotationUseSiteTarget.FIELD)
                }
                else -> {
                    firSymbol.getDeprecationForCallSite(analysisSession.firSession)
                }
            }?.toDeprecationInfo()
        }

    override val KaNamedFunctionSymbol.canBeOperator: Boolean
        get() = withValidityAssertion {
            val functionFir = this@canBeOperator.firSymbol.fir as? FirSimpleFunction ?: return false
            return OperatorFunctionChecks.isOperator(
                functionFir,
                analysisSession.firSession,
                analysisSession.getScopeSessionFor(analysisSession.firSession)
            ).isSuccess
        }


    private fun KaSymbol.deprecationsAreDefinitelyEmpty(): Boolean {
        return when (val psi = psi) {
            is PsiMember -> deprecatedAnnotationsListIsEmpty(psi)
            is KtProperty -> psi.deprecatedAnnotationsListIsEmpty() && psi.accessors.all { it.deprecatedAnnotationsListIsEmpty() }
            is KtDeclaration -> psi.deprecatedAnnotationsListIsEmpty()
            else -> return false
        }
    }

    private fun deprecatedAnnotationsListIsEmpty(psi: PsiMember): Boolean {
        return psi.annotations.isEmpty()
    }

    private fun KtDeclaration.deprecatedAnnotationsListIsEmpty() =
        annotationEntries.none { it.shortName?.identifier in deprecationAnnotationSimpleNames }

    private val deprecationAnnotationSimpleNames: Set<String>
        get() = analysisSession.firSession.annotationPlatformSupport.deprecationAnnotationsSimpleNames

    override fun KaSymbol.deprecationStatus(annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? = withValidityAssertion {
        if (this is KaReceiverParameterSymbol) return null

        require(this is KaFirSymbol<*>)
        return if (annotationUseSiteTarget != null) {
            firSymbol.getDeprecationForCallSite(analysisSession.firSession, annotationUseSiteTarget)
        } else {
            firSymbol.getDeprecationForCallSite(analysisSession.firSession)
        }?.toDeprecationInfo()
    }

    override val KaPropertySymbol.getterDeprecationStatus: DeprecationInfo?
        get() = withValidityAssertion {
            require(this is KaFirSymbol<*>)
            return firSymbol.getDeprecationForCallSite(
                analysisSession.firSession,
                AnnotationUseSiteTarget.PROPERTY_GETTER,
                AnnotationUseSiteTarget.PROPERTY,
            )?.toDeprecationInfo()
        }

    override val KaPropertySymbol.setterDeprecationStatus: DeprecationInfo?
        get() = withValidityAssertion {
            require(this is KaFirSymbol<*>)
            return firSymbol.getDeprecationForCallSite(
                analysisSession.firSession,
                AnnotationUseSiteTarget.PROPERTY_SETTER,
                AnnotationUseSiteTarget.PROPERTY,
            )?.toDeprecationInfo()
        }

    private fun FirDeprecationInfo.toDeprecationInfo(): DeprecationInfo {
        // We pass null as the message, otherwise we can trigger a contract violation
        // as getMessage will call lazyResolveToPhase(ANNOTATION_ARGUMENTS)
        // TODO(KT-67823) stop exposing compiler internals, as the message isn't actually required by the callers.
        return SimpleDeprecationInfo(deprecationLevel, propagatesToOverrides, null)
    }

    override val KaClassSymbol.annotationApplicableTargets: Set<KotlinTarget>?
        get() = withValidityAssertion {
            if (this !is KaFirNamedClassSymbolBase<*>) return null
            if (firSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null
            return firSymbol.getAllowedAnnotationTargets(analysisSession.firSession)
        }
}
