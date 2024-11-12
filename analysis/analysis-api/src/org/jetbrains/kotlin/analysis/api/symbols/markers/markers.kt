/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.name.Name

/**
 * @see org.jetbrains.kotlin.analysis.api.symbols.name
 */
public interface KaNamedSymbol : KaSymbol {
    public val name: Name
}

/**
 * Shouldn't be used as a type.
 * Consider using [typeParameters] directly from required class or [org.jetbrains.kotlin.analysis.api.symbols.typeParameters]
 *
 * @see org.jetbrains.kotlin.analysis.api.symbols.typeParameters
 */
@KaImplementationDetail
public interface KaTypeParameterOwnerSymbol : KaSymbol {
    public val typeParameters: List<KaTypeParameterSymbol>
}
