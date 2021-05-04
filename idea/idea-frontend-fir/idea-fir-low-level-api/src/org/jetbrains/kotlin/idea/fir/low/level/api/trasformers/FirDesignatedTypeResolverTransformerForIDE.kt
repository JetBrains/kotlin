/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirLazyTransformerForIDE.Companion.ensurePhase

class FirDesignatedTypeResolverTransformerForIDE(
    private val originalFile: FirFile,
    private val designation: FirDeclarationDesignation,
    session: FirSession,
    scopeSession: ScopeSession,
): FirLazyTransformerForIDE, FirTypeResolveTransformer(session, scopeSession) {

    private val ideDeclarationTransformer = IDEDeclarationTransformer(FirDesignationIterator(designation.fullDesignation))

    @Suppress("NAME_SHADOWING")
    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
        return ideDeclarationTransformer.transformDeclarationContent(this, regularClass, data) { klass, data ->
            super.transformRegularClass(klass, data) as FirRegularClass
        }
    }

    @Suppress("NAME_SHADOWING")
    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        return ideDeclarationTransformer.transformDeclarationContent(this, anonymousObject, data) { anonymousObject, data ->
            super.transformAnonymousObject(anonymousObject, data) as FirAnonymousObject
        }
    }

    override fun transformDeclaration() {
        designation.ensurePhase(FirResolvePhase.TYPES, exceptLast = true)
        originalFile.transform<FirFile, Any?>(this, null)
    }
}