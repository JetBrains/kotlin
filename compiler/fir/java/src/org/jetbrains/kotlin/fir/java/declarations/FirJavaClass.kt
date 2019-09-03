/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirAbstractMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirModifiableClass
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

class FirJavaClass internal constructor(
    session: FirSession,
    psi: PsiElement?,
    override val symbol: FirClassSymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    override val classKind: ClassKind,
    isTopLevel: Boolean,
    isStatic: Boolean,
    internal val javaTypeParameterStack: JavaTypeParameterStack
) : FirAbstractMemberDeclaration(
    session, psi, name,
    visibility, modality,
    isExpect = false, isActual = false
), FirRegularClass, FirModifiableClass {
    init {
        symbol.bind(this)
        status.isInner = !isTopLevel && !isStatic
        status.isCompanion = false
        status.isData = false
        status.isInline = false
        resolvePhase = FirResolvePhase.DECLARATIONS
    }

    override val superTypeRefs = mutableListOf<FirTypeRef>()

    override val declarations = mutableListOf<FirDeclaration>()

    override val companionObject: FirRegularClass?
        get() = null

    override fun replaceSupertypes(newSupertypes: List<FirTypeRef>): FirRegularClass {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSupertypes)
        return this
    }
}
