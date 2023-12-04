/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.comparators

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction

object FirCallableDeclarationComparator : Comparator<FirCallableDeclaration> {

    override fun compare(lhs: FirCallableDeclaration, rhs: FirCallableDeclaration): Int {
        FirMemberDeclarationComparator.TypeAndNameComparator.compareInternal(lhs, rhs)?.let { return it }

        val lhsExtensionReceiver = lhs.receiverParameter
        val rhsExtensionReceiver = rhs.receiverParameter
        if (lhsExtensionReceiver != null && rhsExtensionReceiver != null) {
            ifRendersNotEqual(lhsExtensionReceiver.typeRef, rhsExtensionReceiver.typeRef) { return it }
        }

        if (lhs is FirFunction && rhs is FirFunction) {
            for ((lhsParam, rhsParam) in lhs.valueParameters.zip(rhs.valueParameters)) {
                ifRendersNotEqual(lhsParam.returnTypeRef, rhsParam.returnTypeRef) { return it }
            }
            ifNotEqual(lhs.valueParameters.size, rhs.valueParameters.size) { return it }
        }

        for ((lhsTypeParam, rhsTypeParam) in lhs.typeParameters.zip(rhs.typeParameters)) {
            val lhsBounds = lhsTypeParam.symbol.fir.bounds
            val rhsBounds = rhsTypeParam.symbol.fir.bounds
            ifNotEqual(lhsBounds.size, rhsBounds.size) { return it }
            for ((lhsBound, rhsBound) in lhsBounds.zip(rhsBounds)) {
                ifRendersNotEqual(lhsBound, rhsBound) { return it }
            }
        }
        ifNotEqual(lhs.typeParameters.size, rhs.typeParameters.size) { return it }

        ifRendersNotEqual(lhs, rhs) { return it }
        return lhs.moduleData.name.compareTo(rhs.moduleData.name)
    }
}
