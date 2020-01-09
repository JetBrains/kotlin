/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.Name

abstract class FirTypeParameterScope : FirScope() {
    abstract val typeParameters: Map<Name, List<FirTypeParameter>>

    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> Unit
    ) {
        val matchedTypeParameters = typeParameters[name] ?: return

        matchedTypeParameters.forEach { processor(it.symbol) }
    }
}
