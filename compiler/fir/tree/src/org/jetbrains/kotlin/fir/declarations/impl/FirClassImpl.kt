/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.FirTransformer

open class FirClassImpl(
    session: FirSession,
    psi: PsiElement?,
    final override val symbol: FirClassSymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    isExpect: Boolean,
    isActual: Boolean,
    final override val classKind: ClassKind,
    isInner: Boolean,
    isCompanion: Boolean,
    isData: Boolean,
    isInline: Boolean
) : FirAbstractMemberDeclaration(session, psi, name, visibility, modality, isExpect, isActual), FirRegularClass, FirModifiableClass {

    init {
        symbol.bind(this)
        status.isInner = isInner
        status.isCompanion = isCompanion
        status.isData = isData
        status.isInline = isInline
    }

    override val superTypes = mutableListOf<FirType>()

    override val declarations = mutableListOf<FirDeclaration>()


    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirRegularClass {
        superTypes.transformInplace(transformer, data)
        val result = super<FirAbstractMemberDeclaration>.transformChildren(transformer, data) as FirRegularClass

        // Transform declarations in last turn
        declarations.transformInplace(transformer, data)
        return result
    }
}