/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedJavaCollectionStubMethod

/**
 * A type parameter list of [SymbolLightMethodForMappedJavaCollectionStubMethod].
 *
 * It mirrors the type parameters of the overridden Java method, but the stub method is its owner,
 * so the parent chain and the containing file lead to the Kotlin light class rather than to the Java declaration.
 */
internal class SymbolLightTypeParameterListForMappedJavaCollectionStubMethod(
    owner: SymbolLightMethodForMappedJavaCollectionStubMethod,
    javaTypeParameters: Array<PsiTypeParameter>,
) : SymbolLightTypeParameterListBase<SymbolLightMethodForMappedJavaCollectionStubMethod>(owner) {
    override val typeParametersCollection: List<PsiTypeParameter> =
        javaTypeParameters.mapIndexed { index, javaTypeParameter ->
            SymbolLightTypeParameterForMappedJavaCollectionStubMethod(javaTypeParameter, parent = this, index = index)
        }

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightTypeParameterListForMappedJavaCollectionStubMethod &&
            owner == other.owner

    override fun hashCode(): Int = owner.hashCode()
}
