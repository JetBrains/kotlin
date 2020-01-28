/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirBodyResolveTransformerAdapter(private val scopeSession: ScopeSession) : FirTransformer<Nothing?>() {

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val transformer = FirBodyResolveTransformer(
            file.session,
            phase = FirResolvePhase.BODY_RESOLVE,
            implicitTypeOnly = false,
            scopeSession = scopeSession
        )
        return file.transform(transformer, ResolutionMode.ContextIndependent)
    }
}
