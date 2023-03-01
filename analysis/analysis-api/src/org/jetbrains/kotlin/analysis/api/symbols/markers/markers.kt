/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.name.Name

public interface KtPossiblyNamedSymbol : KtSymbol {
    public val name: Name?
}

public interface KtNamedSymbol : KtPossiblyNamedSymbol {
    override val name: Name
}

public interface KtSymbolWithTypeParameters : KtSymbol {
    public val typeParameters: List<KtTypeParameterSymbol>

    /**
     * The names of [typeParameters]. Using this property should be preferred if only the names of type parameters are needed, because some
     * symbol implementations may avoid building the full list of type parameters.
     */
    public val typeParameterNames: List<Name>
        get() = withValidityAssertion { typeParameters.map { it.name } }
}