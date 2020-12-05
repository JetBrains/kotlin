/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.types.model.KotlinTypeMarker

interface PostponedResolvedAtomMarker {
    val inputTypes: Collection<KotlinTypeMarker>
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