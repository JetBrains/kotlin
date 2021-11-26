/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedConstantValue
import org.jetbrains.kotlin.analysis.api.fir.evaluate.KtFirConstantValueConverter
import org.jetbrains.kotlin.analysis.api.fir.utils.FirRefWithValidityCheck
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

internal class KtFirAnnotationApplicationForDeclaration(
    private val containingDeclaration: FirRefWithValidityCheck<FirDeclaration>,
    private val useSiteSession: FirSession,
    private val annotation: FirAnnotation,
    override val token: ValidityToken,
) : KtAnnotationApplication() {

    override val psi: KtCallElement? by cached { annotation.psi as? KtCallElement }

    override val classId: ClassId? by cached {
        containingDeclaration.withFirByType(ResolveType.AnnotationType) {
            annotation.fullyExpandedClassId(useSiteSession)
        }
    }

    override val useSiteTarget: AnnotationUseSiteTarget? get() = withValidityAssertion { annotation.useSiteTarget }

    override val arguments: List<KtNamedConstantValue> by containingDeclaration.withFirAndCache(ResolveType.AnnotationsArguments) {
        KtFirConstantValueConverter.toNamedConstantValue(
            mapAnnotationParameters(annotation, useSiteSession),
            useSiteSession,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KtFirAnnotationApplicationForDeclaration) return false
        if (this.token != other.token) return false
        return annotation == other.annotation
    }

    override fun hashCode(): Int {
        return token.hashCode() * 31 + annotation.hashCode()
    }
}