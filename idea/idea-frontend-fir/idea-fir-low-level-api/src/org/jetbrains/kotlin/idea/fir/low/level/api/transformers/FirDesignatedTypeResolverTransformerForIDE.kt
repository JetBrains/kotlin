/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.ensurePhase

class FirDesignatedTypeResolverTransformerForIDE(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : FirLazyTransformerForIDE, FirTypeResolveTransformer(session, scopeSession) {

    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    @Suppress("NAME_SHADOWING")
    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
        return ideDeclarationTransformer.transformDeclarationContent(this, regularClass, data) {
            super.transformRegularClass(regularClass, data) as FirRegularClass
        }
    }

    @Suppress("NAME_SHADOWING")
    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        return ideDeclarationTransformer.transformDeclarationContent(this, anonymousObject, data) {
            super.transformAnonymousObject(anonymousObject, data) as FirAnonymousObject
        }
    }

    override fun transformDeclaration() {
        designation.ensurePhase(FirResolvePhase.TYPES, exceptTarget = true)
        designation.firFile.transform<FirFile, Any?>(this, null)
    }
}