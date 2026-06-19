/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.isInlineClass
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.StandardClassIds


fun FirTypeRef.isInlineClassThatRequiresMangling(session: FirSession): Boolean {
    val symbol = this.coneType.toRegularClassSymbol(session) ?: return false
    return symbol.isInlineClassThatRequiresMangling()
}

fun FirRegularClassSymbol.isInlineClassThatRequiresMangling(): Boolean =
    isInlineClass && classId != StandardClassIds.Result
