/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

class FirJvmEffectiveVisibilityResolver(session: FirSession) : FirEffectiveVisibilityResolverImpl(session) {
    override fun computeEffectiveVisibility(visibility: Visibility, containerSymbol: FirClassLikeSymbol<*>?): FirEffectiveVisibility {
        return when (visibility) {
            JavaVisibilities.PackageVisibility -> FirEffectiveVisibilityImpl.PackagePrivate
            else -> super.computeEffectiveVisibility(visibility, containerSymbol)
        }
    }
}
