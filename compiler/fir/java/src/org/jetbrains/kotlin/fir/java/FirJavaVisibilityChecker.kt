/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.calls.ReceiverValue
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.FqName

@NoMutableState
object FirJavaVisibilityChecker : FirVisibilityChecker() {
    override fun platformVisibilityCheck(
        declarationVisibility: Visibility,
        symbol: FirBasedSymbol<*>,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
        dispatchReceiver: ReceiverValue?,
        session: FirSession,
        isCallToPropertySetter: Boolean,
        supertypeSupplier: SupertypeSupplier
    ): Boolean {
        return when (declarationVisibility) {
            JavaVisibilities.ProtectedAndPackage, JavaVisibilities.ProtectedStaticVisibility -> {
                if (symbol.packageFqName() == useSiteFile.packageFqName) {
                    true
                } else {
                    val ownerLookupTag = symbol.getOwnerLookupTag() ?: return false
                    if (canSeeProtectedMemberOf(
                            symbol,
                            containingDeclarations,
                            // Note: dispatch receiver isn't relevant for Java protected static
                            // See e.g. diagnostics/tests/visibility/packagePrivateStatic.kt
                            dispatchReceiver.takeUnless { symbol is FirCallableSymbol && symbol.isStatic },
                            ownerLookupTag,
                            session,
                            isVariableOrNamedFunction = symbol.isVariableOrNamedFunction(),
                            isSyntheticProperty = symbol.fir is FirSyntheticPropertyAccessor,
                            supertypeSupplier
                        )
                    ) return true

                    // FE1.0 allows calling public setters with property assignment syntax if the getter is protected.
                    if (!isCallToPropertySetter || symbol !is FirSimpleSyntheticPropertySymbol) return false
                    symbol.setterSymbol?.visibility == Visibilities.Public && symbol.isCalledFromSubclass(containingDeclarations, session)
                }
            }

            JavaVisibilities.PackageVisibility -> symbol.isInPackage(useSiteFile.packageFqName)
            else -> true
        }
    }

    private fun FirSimpleSyntheticPropertySymbol.isCalledFromSubclass(
        containingDeclarations: List<FirDeclaration>,
        session: FirSession
    ): Boolean {
        val containingClassLookupTag = this.containingClassLookupTag() ?: return false
        return containingDeclarations.any { it is FirClass && it.isSubclassOf(containingClassLookupTag, session, false)  }
    }

    override fun platformOverrideVisibilityCheck(
        candidateInDerivedClass: FirBasedSymbol<*>,
        symbolInBaseClass: FirBasedSymbol<*>,
        visibilityInBaseClass: Visibility,
    ): Boolean = when (visibilityInBaseClass) {
        JavaVisibilities.ProtectedAndPackage, JavaVisibilities.ProtectedStaticVisibility -> true
        JavaVisibilities.PackageVisibility -> symbolInBaseClass.isInPackage(candidateInDerivedClass.packageFqName())
        else -> true
    }

    private fun FirBasedSymbol<*>.isInPackage(expected: FqName): Boolean =
        packageFqName() == expected || (fir is FirSyntheticPropertyAccessor && getOwnerLookupTag()?.classId?.packageFqName == expected)
}
