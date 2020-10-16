/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getPrimaryConstructorIfAny
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirAnnotationCall
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedConstantValue
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSimpleConstantValue
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtUnsupportedConstantValue
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement

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

private fun convertAnnotation(annotationCall: FirAnnotationCall, session: FirSession): KtFirAnnotationCall? {

    val annotationCone = annotationCall.annotationTypeRef.coneType as? ConeClassLikeType ?: return null
    val classId = annotationCone.classId ?: return null

    fun FirExpression.convertConstantExpression() =
        (this as? FirConstExpression<*>)?.value?.let { KtSimpleConstantValue(it) }
            ?: KtUnsupportedConstantValue

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

internal fun convertAnnotation(declaration: FirAnnotatedDeclaration) = declaration.annotations.mapNotNull {
    convertAnnotation(it, declaration.session)
}