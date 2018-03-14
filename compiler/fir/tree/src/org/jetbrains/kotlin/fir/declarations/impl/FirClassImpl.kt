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
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.name.Name

class FirClassImpl(
    session: FirSession,
    psi: PsiElement?,
    name: Name,
    visibility: Visibility,
    modality: Modality,
    override val classKind: ClassKind,
    override val isCompanion: Boolean,
    override val isData: Boolean
) : FirAbstractMemberDeclaration(session, psi, name, visibility, modality), FirClass {
    override val superTypes = mutableListOf<FirType>()

    override val declarations = mutableListOf<FirDeclaration>()
}