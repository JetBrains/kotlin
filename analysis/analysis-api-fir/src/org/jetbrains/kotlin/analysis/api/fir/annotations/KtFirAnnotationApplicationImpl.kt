/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedConstantValue
import org.jetbrains.kotlin.analysis.api.fir.evaluate.KtFirConstantValueConverter
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

internal class KtFirAnnotationApplicationImpl(
    private val annotation: FirAnnotation,
    private val useSiteSession: FirSession,
    override val token: ValidityToken,
) : KtAnnotationApplication() {

    override val psi: KtCallElement? by cached { annotation.psi as? KtCallElement }

    override val classId: ClassId?
        get() = withValidityAssertion {
            annotation.fullyExpandedClassId(useSiteSession)
        }

    override val useSiteTarget: AnnotationUseSiteTarget? get() = annotation.useSiteTarget

    override val arguments: List<KtNamedConstantValue> by cached {
        KtFirConstantValueConverter.toNamedConstantValue(
            mapAnnotationParameters(annotation, useSiteSession),
            useSiteSession,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KtFirAnnotationApplicationImpl) return false
        return annotation == other.annotation
    }

    override fun hashCode(): Int {
        return annotation.hashCode()
    }
}