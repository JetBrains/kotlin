/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.resolve.calls.inference.model.MultiLambdaBuilderInferenceRestriction
import org.jetbrains.kotlin.types.model.TypeParameterMarker

class AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction(
    anonymous: FirAnonymousFunction,
    typeParameter: TypeParameterMarker
) : MultiLambdaBuilderInferenceRestriction<FirAnonymousFunction>(anonymous, typeParameter)
