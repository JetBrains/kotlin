/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirAnnotationCall
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol

internal fun mapAnnotationParameters(annotationCall: FirAnnotationCall, session: FirSession): Map<String, FirExpression> {

    val annotationCone = annotationCall.annotationTypeRef.coneType as? ConeClassLikeType ?: return emptyMap()

    val annotationPrimaryCtor = (annotationCone.lookupTag.toSymbol(session)?.fir as? FirRegularClass)?.getPrimaryConstructorIfAny()
    val annotationCtorParameterNames = annotationPrimaryCtor?.valueParameters?.map { it.name }

    val resultSet = mutableMapOf<String, FirExpression>()

    val namesSequence = annotationCtorParameterNames?.asSequence()?.iterator()

    for (argument in annotationCall.argumentList.arguments.filterIsInstance<FirNamedArgumentExpression>()) {
        resultSet[argument.name.asString()] = argument.expression
    }

    for (argument in annotationCall.argumentList.arguments) {
        if (argument is FirNamedArgumentExpression) continue

        while (namesSequence != null && namesSequence.hasNext()) {
            val name = namesSequence.next().asString()
            if (!resultSet.contains(name)) {
                resultSet[name] = argument
                break
            }
        }
    }

    return resultSet
}

internal fun FirExpression.convertConstantExpression(): KtConstantValue =
    when (this) {
        is FirConstExpression<*> -> KtSimpleConstantValue(value)
        else -> KtUnsupportedConstantValue
    }

private fun ConeClassLikeType.expandTypeAliasIfNeeded(session: FirSession): ConeClassLikeType {
    val firTypeAlias = lookupTag.toSymbol(session) as? FirTypeAliasSymbol ?: return this
    val expandedType = firTypeAlias.fir.expandedTypeRef.coneType
    return expandedType.fullyExpandedType(session) as? ConeClassLikeType
        ?: return this
}

internal fun convertAnnotation(
    annotationCall: FirAnnotationCall,
    session: FirSession
): KtFirAnnotationCall? {

    val declaredCone = annotationCall.annotationTypeRef.coneType as? ConeClassLikeType ?: return null

    val classId = declaredCone.expandTypeAliasIfNeeded(session).classId ?: return null

    val resultList = mapAnnotationParameters(annotationCall, session).map {
        KtNamedConstantValue(it.key, it.value.convertConstantExpression())
    }

    return KtFirAnnotationCall(
        classId = classId,
        useSiteTarget = annotationCall.useSiteTarget,
        psi = annotationCall.psi as? KtCallElement,
        arguments = resultList
    )
}

internal fun convertAnnotation(declaration: FirAnnotatedDeclaration): List<KtFirAnnotationCall> =
    declaration.annotations.mapNotNull {
        convertAnnotation(it, declaration.session)
    }