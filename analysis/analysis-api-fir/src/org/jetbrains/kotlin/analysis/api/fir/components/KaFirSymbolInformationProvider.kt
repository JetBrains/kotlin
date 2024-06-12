/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSymbolInformationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.SimpleDeprecationInfo

internal class KaFirSymbolInformationProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaSessionComponent<KaFirSession>(), KaSymbolInformationProvider, KaFirSessionComponent {
    override val KaSymbol.deprecationStatus: DeprecationInfo?
        get() = withValidityAssertion {
            if (this is KaFirPackageSymbol || this is KaReceiverParameterSymbol) return null
            require(this is KaFirSymbol<*>) { "${this::class}" }

            // Optimization: Avoid building `firSymbol` of `KtFirPsiJavaClassSymbol` if it definitely isn't deprecated.
            if (this is KaFirPsiJavaClassSymbol && !mayHaveDeprecation()) {
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

    private fun KaFirPsiJavaClassSymbol.mayHaveDeprecation(): Boolean {
        if (!hasAnnotations) return false

        // Check the simple names of the Java annotations. While presence of such an annotation name does not prove deprecation, it is a
        // necessary condition for it. Type aliases are not a problem here: Java code cannot access Kotlin type aliases. (Currently,
        // deprecation annotation type aliases do not work in Kotlin, either, but this might change in the future.)
        val deprecationAnnotationSimpleNames = analysisSession.firSession.annotationPlatformSupport.deprecationAnnotationsSimpleNames
        return annotationSimpleNames.any { it != null && it in deprecationAnnotationSimpleNames }
    }

    override fun KaSymbol.deprecationStatus(annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? = withValidityAssertion {
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

    override val KaClassOrObjectSymbol.annotationApplicableTargets: Set<KotlinTarget>?
        get() = withValidityAssertion {
            requireIsInstance<KaFirSymbol<*>>(this)
            if (this !is KaFirNamedClassOrObjectSymbolBase) return null
            if (firSymbol.classKind != ClassKind.ANNOTATION_CLASS) return null
            return firSymbol.getAllowedAnnotationTargets(analysisSession.firSession)
        }
}
