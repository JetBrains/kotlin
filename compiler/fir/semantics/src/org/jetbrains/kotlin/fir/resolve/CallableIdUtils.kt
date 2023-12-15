/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.providers.getSymbolByTypeRef
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

fun CallableId.isInvoke(): Boolean =
    isKFunctionInvoke()
            || callableName.asString() == "invoke"
            && className?.asString()?.startsWith("Function") == true
            && packageName == StandardClassIds.BASE_KOTLIN_PACKAGE

fun CallableId.isKFunctionInvoke(): Boolean =
    callableName.asString() == "invoke"
            && className?.asString()?.startsWith("KFunction") == true
            && packageName.asString() == "kotlin.reflect"

fun CallableId.isIteratorNext(): Boolean =
    callableName.asString() == "next" && className?.asString()?.endsWith("Iterator") == true
            && packageName.asString() == "kotlin.collections"

fun CallableId.isIteratorHasNext(): Boolean =
    callableName.asString() == "hasNext" && className?.asString()?.endsWith("Iterator") == true
            && packageName.asString() == "kotlin.collections"

fun CallableId.isIterator(): Boolean =
    callableName.asString() == "iterator" && packageName.asString() in arrayOf("kotlin", "kotlin.collections", "kotlin.ranges")

fun FirAnnotation.fqName(session: FirSession): FqName? {
    val symbol = session.symbolProvider.getSymbolByTypeRef<FirRegularClassSymbol>(annotationTypeRef) ?: return null
    return symbol.classId.asSingleFqName()
}
