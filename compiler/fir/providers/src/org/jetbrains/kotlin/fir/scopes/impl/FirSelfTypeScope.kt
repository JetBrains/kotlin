/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.builtins.StandardNames.SELF_TYPE_NAME
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames.SELF_TYPE

class FirSelfTypeScope(val memberDeclaration: FirMemberDeclaration) : FirScope() {

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        if (name.asString() == SELF_TYPE_NAME) {
            val selfSymbol = memberDeclaration.typeParameters.find { it.symbol.name == SELF_TYPE }?.symbol
            if (selfSymbol != null) {
                return processor(selfSymbol, ConeSubstitutor.Empty)
            }
        }
    }
}