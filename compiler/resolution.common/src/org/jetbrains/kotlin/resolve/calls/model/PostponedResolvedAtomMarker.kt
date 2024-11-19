/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.types.model.KotlinTypeMarker

interface PostponedResolvedAtomMarker {
    /**
     * Generally, it's a collection of types that need to be "proper" to start the analysis of the atom (unless PCLA).
     * Used mostly to define if the atom is ready and to define the order among other atoms.
     *
     * Usually, it's just a list of receiver/value parameter types, but might be an expected type variable.
     * (see [org.jetbrains.kotlin.fir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom])
     */
    val inputTypes: Collection<KotlinTypeMarker>

    /**
     * Type that might be refined after analysis of the given atom, i.e., some new type variable constraints found.
     * Currently, used to define dependencies between variables.
     * (see TypeVariableDependencyInformationProvider.computePostponeArgumentsEdges)
     *
     * Usually, it's a return type of lambda/reference.
     * Might be `null` if the return type is unknown or irrelevant.
     */
    val outputType: KotlinTypeMarker?
    val expectedType: KotlinTypeMarker?
    val analyzed: Boolean
}

interface PostponedAtomWithRevisableExpectedType : PostponedResolvedAtomMarker {
    val revisedExpectedType: KotlinTypeMarker?

    fun reviseExpectedType(expectedType: KotlinTypeMarker)
}

interface PostponedCallableReferenceMarker : PostponedAtomWithRevisableExpectedType

interface LambdaWithTypeVariableAsExpectedTypeMarker : PostponedAtomWithRevisableExpectedType {
    val parameterTypesFromDeclaration: List<KotlinTypeMarker?>?

    fun updateParameterTypesFromDeclaration(types: List<KotlinTypeMarker?>?)
}
