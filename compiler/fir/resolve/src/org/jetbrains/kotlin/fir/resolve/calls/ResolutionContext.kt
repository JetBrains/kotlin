/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext

class ResolutionContext(
    val session: FirSession,
    val bodyResolveComponents: BodyResolveComponents,
    val bodyResolveContext: BodyResolveContext
) {
    val inferenceComponents: InferenceComponents
        get() = session.inferenceComponents

    val returnTypeCalculator: ReturnTypeCalculator
        get() = bodyResolveComponents.returnTypeCalculator
}
