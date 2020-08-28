/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirVisibilityChecker
import org.jetbrains.kotlin.fir.JavaVisibilities
import org.jetbrains.kotlin.fir.Visibility
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.name.ClassId

object FirJavaVisibilityChecker : FirVisibilityChecker() {
    override fun platformVisibilityCheck(
        declarationVisibility: Visibility,
        symbol: AbstractFirBasedSymbol<*>,
        useSiteFile: FirFile,
        ownerId: ClassId?,
        containingDeclarations: List<FirDeclaration>,
        candidate: Candidate,
        session: FirSession
    ): Boolean {
        return when (declarationVisibility) {
            JavaVisibilities.ProtectedAndPackage, JavaVisibilities.ProtectedStaticVisibility -> {
                if (symbol.packageFqName() == useSiteFile.packageFqName) {
                    true
                } else {
                    ownerId != null && canSeeProtectedMemberOf(containingDeclarations, candidate.dispatchReceiverValue, ownerId, session)
                }
            }

            JavaVisibilities.PackageVisibility -> {
                symbol.packageFqName() == useSiteFile.packageFqName
            }

            else -> true
        }
    }
}
