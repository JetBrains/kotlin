/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.metadata.Modality
import kotlin.reflect.KTypeParameter
import kotlin.reflect.jvm.internal.types.KTypeSubstitutor

internal data class KCallableOverriddenStorage(
    private val classTypeParametersSubstitutor: KTypeSubstitutor,
    val modality: Modality?,
    val isStatic: Boolean?,
    val originalContainerIfFakeOverride: KDeclarationContainerImpl?,
    private val originalCallableTypeParameters: List<KTypeParameter>,

    val forceIsExternal: Boolean,
    val forceIsOperator: Boolean,
    val forceIsInfix: Boolean,
    val forceIsInline: Boolean,
) {
    companion object {
        @JvmField
        val EMPTY = KCallableOverriddenStorage(
            classTypeParametersSubstitutor = KTypeSubstitutor.EMPTY,
            modality = null,
            isStatic = null,
            originalContainerIfFakeOverride = null,
            originalCallableTypeParameters = emptyList(),
            forceIsExternal = false,
            forceIsOperator = false,
            forceIsInfix = false,
            forceIsInline = false,
        )
    }

    val isFakeOverride: Boolean get() = originalContainerIfFakeOverride != null

    fun withChainedClassTypeParametersSubstitutor(substitutor: KTypeSubstitutor): KCallableOverriddenStorage =
        copy(classTypeParametersSubstitutor = classTypeParametersSubstitutor.chainedWith(substitutor))

    fun getTypeSubstitutor(callableTypeParameters: List<KTypeParameter>, memberNameForDebug: String): KTypeSubstitutor =
        originalCallableTypeParameters.substitutedWith(callableTypeParameters)
            ?.disjointSumWith(classTypeParametersSubstitutor, memberNameForDebug)
            ?: classTypeParametersSubstitutor
}
