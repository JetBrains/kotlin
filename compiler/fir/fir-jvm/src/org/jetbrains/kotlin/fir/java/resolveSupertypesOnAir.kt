/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeResolverVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

/**
 * The supertype refs of this class, resolved on air unless they are resolved already. A class with no containing file
 * in the [session]'s `FirProvider` gets its refs back as they are, so callers must cope with unresolved refs.
 */
fun FirRegularClass.resolveSupertypesOnAir(session: FirSession): List<FirTypeRef> {
    if (superTypeRefs.all { it is FirResolvedTypeRef }) return superTypeRefs
    val containingFile = session.firProvider.getFirClassifierContainerFileIfAny(symbol) ?: return superTypeRefs
    val visitor = FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession())
    return visitor.withFile(containingFile) {
        visitor.resolveSpecificClassLikeSupertypes(this, superTypeRefs, resolveRecursively = true)
    }
}
