/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration.crv

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeReturnsResultOfDeclaration
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.mustUseReturnValueStatusComponent
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.functionTypeKind
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.type

context(context: CheckerContext)
internal fun ConeKotlinType.isIgnorable(): Boolean {
    return context.session.mustUseReturnValueStatusComponent.isIgnorableType(this)
}

@OptIn(SymbolInternals::class)
internal fun FirCallableSymbol<*>.indicesOfPropagatingFunctionalParameters(): List<Int> {
    val declaration = fir as? FirContractDescriptionOwner ?: return emptyList()
    val contractDescription = declaration.contractDescription as? FirResolvedContractDescription ?: return emptyList()
    return buildList {
        for (effectDeclaration in contractDescription.effects) {
            val effect = effectDeclaration.effect
            if (effect is ConeReturnsResultOfDeclaration) {
                add(effect.valueParameterReference.parameterIndex)
            }
        }
    }
}

internal fun ConeKotlinType.isFunctionalTypeThatReturnsUnit(session: FirSession): Boolean =
    functionTypeKind(session) != null && typeArguments.last().type?.isUnit == true
