/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.StandardClassIds

fun FirRegularClass.getRetention(session: FirSession): AnnotationRetention {
    return getRetentionAnnotation(session)?.getRetention() ?: AnnotationRetention.RUNTIME
}

fun FirRegularClassSymbol.getRetention(session: FirSession): AnnotationRetention {
    return getRetentionAnnotation(session)?.getRetention() ?: AnnotationRetention.RUNTIME
}

fun FirAnnotation.getRetention(): AnnotationRetention? {
    val propertyAccess = findArgumentByName(StandardClassIds.Annotations.ParameterNames.retentionValue) as? FirQualifiedAccessExpression
    val callableId = propertyAccess?.calleeReference?.toResolvedEnumEntrySymbol()?.callableId ?: return null

    if (callableId.classId != StandardClassIds.AnnotationRetention) {
        return null
    }

    return AnnotationRetention.entries.firstOrNull { it.name == callableId.callableName.asString() }
}

fun FirDeclaration.getRetentionAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(StandardClassIds.Annotations.Retention, session)
}

private fun FirRegularClassSymbol.getRetentionAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(StandardClassIds.Annotations.Retention, session)
}

fun FirClassLikeSymbol<*>.getExplicitAnnotationRetention(session: FirSession): AnnotationRetention? {
    return getAnnotationByClassId(StandardClassIds.Annotations.Retention, session)?.getRetention()
}

fun FirClassLikeSymbol<*>.getAnnotationRetention(session: FirSession): AnnotationRetention {
    return getExplicitAnnotationRetention(session) ?: AnnotationRetention.RUNTIME
}
