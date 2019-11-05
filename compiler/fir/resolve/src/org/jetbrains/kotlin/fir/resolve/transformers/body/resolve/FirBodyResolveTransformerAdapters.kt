/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

class FirDesignatedBodyResolveTransformer(
    private val designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession = ScopeSession(),
    implicitTypeOnly: Boolean = true
) : FirBodyResolveTransformer(
    session,
    phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = implicitTypeOnly,
    scopeSession = scopeSession
) {
    override fun <E : FirElement> transformElement(element: E, data: ResolutionMode): CompositeTransformResult<E> {
        if (designation.hasNext()) {
            designation.next().visitNoTransform(this, data)
            return element.compose()
        }
        return super.transformElement(element, data)
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        return components.withContainer(declaration) {
            declaration.replaceResolvePhase(transformerPhase)
            transformElement(declaration, data)
        }
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirImplicitTypeBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    private val scopeSession = ScopeSession()

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val transformer = FirBodyResolveTransformer(
            file.session,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            implicitTypeOnly = true,
            scopeSession = scopeSession
        )
        return file.transform(transformer, ResolutionMode.ContextDependent)
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    private val scopeSession = ScopeSession()

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        // Despite of real phase is EXPRESSIONS, we state IMPLICIT_TYPES here, because DECLARATIONS previous phase is OK for us
        val transformer = FirBodyResolveTransformer(
            file.session,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            implicitTypeOnly = false,
            scopeSession = scopeSession
        )
        return file.transform(transformer, ResolutionMode.ContextDependent)
    }
}