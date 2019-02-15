/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirAbstractMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirModifiableClass
import org.jetbrains.kotlin.fir.java.scopes.JavaClassEnhancementScope
import org.jetbrains.kotlin.fir.java.scopes.JavaClassUseSiteScope
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

class FirJavaClass(
    session: FirSession,
    override val symbol: FirClassSymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    override val classKind: ClassKind,
    isTopLevel: Boolean,
    isStatic: Boolean
) : FirAbstractMemberDeclaration(
    session, psi = null, name = name,
    visibility = visibility, modality = modality,
    isExpect = false, isActual = false
), FirRegularClass, FirModifiableClass {
    init {
        symbol.bind(this)
        status.isInner = !isTopLevel && !isStatic
        status.isCompanion = false
        status.isData = false
        status.isInline = false
    }

    override val superTypeRefs = mutableListOf<FirTypeRef>()

    val useSiteScope: JavaClassUseSiteScope by lazy { buildUseSiteScope() }

    override val declarations = mutableListOf<FirDeclaration>()

    private fun FirRegularClass.buildUseSiteScope(useSiteSession: FirSession = session): JavaClassUseSiteScope {
        val superTypeEnhancementScope = FirCompositeScope(mutableListOf())
        val declaredScope = FirClassDeclaredMemberScope(this, useSiteSession)
        lookupSuperTypes(this, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
            .mapNotNullTo(superTypeEnhancementScope.scopes) { useSiteSuperType ->
                if (useSiteSuperType is ConeClassErrorType) return@mapNotNullTo null
                val symbol = useSiteSuperType.lookupTag.toSymbol(useSiteSession)
                if (symbol is FirClassSymbol) {
                    JavaClassEnhancementScope(useSiteSession, symbol.fir.buildUseSiteScope(useSiteSession))
                } else {
                    null
                }
            }
        return JavaClassUseSiteScope(this, useSiteSession, superTypeEnhancementScope, declaredScope)
    }

    override fun replaceSupertypes(newSupertypes: List<FirTypeRef>): FirRegularClass {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSupertypes)
        return this
    }
}