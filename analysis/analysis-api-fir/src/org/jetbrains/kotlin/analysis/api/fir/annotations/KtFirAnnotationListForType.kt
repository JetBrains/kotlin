/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.getKtAnnotationApplicationForExtensionFunctionType
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClassId
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.isExtensionFunctionType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ExtensionFunctionType

internal class KtFirAnnotationListForType private constructor(
    val coneType: ConeKotlinType,
    private val useSiteSession: FirSession,
    override val token: KtLifetimeToken,
) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion {
            coneType.customAnnotations.map {
                it.toKtAnnotationApplication(useSiteSession)
            } + listOfNotNull(
                if (coneType.isExtensionFunctionType)
                    getKtAnnotationApplicationForExtensionFunctionType()
                else null
            )
        }

    override fun hasAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        coneType.customAnnotations.any {
            it.fullyExpandedClassId(useSiteSession) == classId
        } || isExtensionFunctionType(classId)
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = withValidityAssertion {
        coneType.customAnnotations.mapNotNull { annotation ->
            if (annotation.fullyExpandedClassId(useSiteSession) != classId) return@mapNotNull null
            annotation.toKtAnnotationApplication(useSiteSession)
        } + listOfNotNull(
            if (isExtensionFunctionType(classId))
                getKtAnnotationApplicationForExtensionFunctionType()
            else null
        )
    }

    private fun isExtensionFunctionType(classId: ClassId): Boolean =
        classId == ExtensionFunctionType && coneType.isExtensionFunctionType

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            coneType.customAnnotations.mapNotNull {
                it.fullyExpandedClassId(useSiteSession)
            } + listOfNotNull(
                if (coneType.isExtensionFunctionType) ExtensionFunctionType else null
            )
        }

    companion object {
        fun create(
            coneType: ConeKotlinType,
            useSiteSession: FirSession,
            token: KtLifetimeToken,
        ): KtAnnotationsList {
            return if (coneType.customAnnotations.isEmpty() && !coneType.isExtensionFunctionType) {
                KtEmptyAnnotationsList(token)
            } else {
                KtFirAnnotationListForType(coneType, useSiteSession, token)
            }
        }
    }
}

