/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

private fun FirAnnotationCall.toAnnotationLookupTag(): ConeClassLikeLookupTag =
    ((annotationTypeRef as FirResolvedTypeRef).type as ConeClassLikeType).lookupTag

internal fun FirAnnotationCall.toAnnotationClassId(): ClassId =
    toAnnotationLookupTag().classId

internal fun FirAnnotationCall.toAnnotationClass(session: FirSession): FirRegularClass? =
    toAnnotationLookupTag().toSymbol(session)?.fir as? FirRegularClass

fun FirAnnotationContainer.nonSourceAnnotations(session: FirSession): List<FirAnnotationCall> =
    annotations.filter { annotation ->
        val firAnnotationClass = annotation.toAnnotationClass(session)
        firAnnotationClass != null && firAnnotationClass.annotations.none { meta ->
            val firMetaAnnotationClassId = meta.toAnnotationClassId()
            firMetaAnnotationClassId.asString() == "kotlin/annotation/Retention" &&
                    // TODO: this is temporary solution, we need something better
                    (meta.argumentList.arguments.singleOrNull() as? FirQualifiedAccessExpression)?.let {
                        val callableSymbol = (it.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirCallableSymbol<*>
                        callableSymbol?.callableId?.callableName
                    } == Name.identifier("SOURCE")
        }
    }

