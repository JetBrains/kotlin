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
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberPlatformStatus
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
    platformStatus: FirMemberPlatformStatus,
    final override val classKind: ClassKind,
    final override val isInner: Boolean,
    final override val isCompanion: Boolean,
    final override val isData: Boolean,
    override val isInline: Boolean
) : FirAbstractMemberDeclaration(session, psi, name, visibility, modality, platformStatus), FirClass {

    init {
        symbol.bind(this)
    }

    override val modality: Modality
        get() = super.modality ?: if (classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL

    override val superTypes = mutableListOf<FirType>()

    override val declarations = mutableListOf<FirDeclaration>()


    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirClass {
        superTypes.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)

        return super<FirAbstractMemberDeclaration>.transformChildren(transformer, data) as FirClass
    }
}