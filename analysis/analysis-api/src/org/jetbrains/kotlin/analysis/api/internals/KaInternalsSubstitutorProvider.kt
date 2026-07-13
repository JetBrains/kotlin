/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaUnificationSubstitutorPolicy

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
@OptIn(KaExperimentalApi::class)
public interface KaInternalsSubstitutorProvider {
    public fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor

    public fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor?

    @KaIdeApi
    public fun createSubtypingUnificationSubstitutor(
        leftTypesToRightTypes: List<Pair<KaType, KaType>>,
        isFreeTypeParameter: (KaTypeParameterSymbol) -> Boolean,
    ): KaSubstitutor?

    @KaIdeApi
    public fun createSubtypingUnificationSubstitutor(
        leftType: KaType,
        rightType: KaType,
        constructionPolicy: KaUnificationSubstitutorPolicy,
    ): KaSubstitutor?

    @KaIdeApi
    public fun createSubtypingUnificationSubstitutor(
        leftTypesToRightTypes: List<Pair<KaType, KaType>>,
        constructionPolicy: KaUnificationSubstitutorPolicy,
    ): KaSubstitutor?
}
