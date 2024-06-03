/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForType private constructor(
    val coneType: ConeKotlinType,
    private val builder: KaSymbolByFirBuilder,
) : AbstractList<KaAnnotationApplication>(), KaAnnotationList {
    private val backingAnnotations: List<KaAnnotationApplication> by lazy {
        coneType.customAnnotations.mapIndexed { index, firAnnotation ->
            val containingSymbol = (firAnnotation as? FirAnnotationCall)?.containingDeclarationSymbol

            // Resolve annotation types
            containingSymbol?.lazyResolveToPhase(FirResolvePhase.TYPES)

            firAnnotation.toKtAnnotationApplication(builder, index) {
                if (containingSymbol != null && firAnnotation.arguments.isNotEmpty()) {
                    // Resolve annotation arguments
                    containingSymbol.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
                }

                FirAnnotationValueConverter.toNamedConstantValue(builder.analysisSession, mapAnnotationParameters(firAnnotation), builder)
            }
        }
    }

    override val token: KaLifetimeToken
        get() = builder.token

    private val useSiteSession: FirSession
        get() = builder.rootSession

    override fun isEmpty(): Boolean = withValidityAssertion {
        return coneType.customAnnotations.isEmpty()
    }

    override val size: Int
        get() = withValidityAssertion { coneType.customAnnotations.size }

    override fun iterator(): Iterator<KaAnnotationApplication> = withValidityAssertion {
        return backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotationApplication = withValidityAssertion {
        return backingAnnotations[index]
    }

    override fun hasAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        return backingAnnotations.any { it.classId == classId }
    }

    override fun annotationsByClassId(classId: ClassId): List<KaAnnotationApplication> = withValidityAssertion {
        return backingAnnotations.filter { it.classId == classId }
    }

    override val classIds: Collection<ClassId>
        get() = withValidityAssertion {
            return backingAnnotations.mapNotNull { it.classId }
        }

    companion object {
        fun create(coneType: ConeKotlinType, builder: KaSymbolByFirBuilder): KaAnnotationList {
            return if (coneType.customAnnotations.isEmpty()) {
                KaEmptyAnnotationList(builder.token)
            } else {
                KaFirAnnotationListForType(coneType, builder)
            }
        }
    }
}