/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.getPrimaryConstructorSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.CustomAnnotationTypeAttribute
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import kotlin.collections.ifEmpty

/**
 * Builds [ConeAttributes] with annotations constructed the given [annotationClassIds].
 * If some class id corresponds to non-annotation class or to some annotation class
 * requiring arguments, this id is ignored.
 */
internal fun constructAttributesForNonArgsAnnotations(annotationClassIds: List<ClassId>, session: KaFirSession): ConeAttributes =
    ConeAttributes.create(constructAttributeListForNonArgsAnnotations(annotationClassIds, session))

private fun constructAttributeListForNonArgsAnnotations(
    annotationClassIds: List<ClassId>,
    session: KaFirSession
): List<ConeAttribute<*>> {
    if (annotationClassIds.isEmpty()) {
        return emptyList()
    }

    val firAnnotations = annotationClassIds.mapNotNull {
        constructFirAnnotationWithoutArguments(it, session)
    }.ifEmpty { return emptyList() }

    val customAttribute = CustomAnnotationTypeAttribute(firAnnotations)

    return listOf(customAttribute)
}

/**
 * Builds [FirAnnotation] from the given [classId].
 * If the given [classId] corresponds to non-annotation class or to some annotation class
 * requiring arguments, returns `null`.
 */
internal fun constructFirAnnotationWithoutArguments(classId: ClassId, session: KaFirSession): FirAnnotation? {
    val firSession = session.firSession
    val classSymbol = findAnnotationClassSymbol(classId, session) ?: return null

    val primaryConstructor = classSymbol.getPrimaryConstructorSymbol(firSession, firSession.getScopeSession()) ?: return null

    if (primaryConstructor.valueParameterSymbols.isNotEmpty()) {
        return null
    }

    return buildFirAnnotation(classSymbol, FirEmptyAnnotationArgumentMapping)
}

/**
 * Returns the symbol of the class denoted by [classId] if it is an annotation class, and `null` otherwise.
 */
internal fun findAnnotationClassSymbol(classId: ClassId, session: KaFirSession): FirClassLikeSymbol<*>? {
    val classSymbol = session.firSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
    return classSymbol.takeIf { it.classKind?.isAnnotationClass == true }
}

/**
 * Builds a source-less [FirAnnotation] of the [classSymbol] annotation class with the given [argumentMapping].
 */
internal fun buildFirAnnotation(classSymbol: FirClassLikeSymbol<*>, argumentMapping: FirAnnotationArgumentMapping): FirAnnotation =
    buildAnnotation {
        annotationTypeRef = buildResolvedTypeRef {
            this.coneType = classSymbol.defaultType()
        }

        this.argumentMapping = argumentMapping
    }
