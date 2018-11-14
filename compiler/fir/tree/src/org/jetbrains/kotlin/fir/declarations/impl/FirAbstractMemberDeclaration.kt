/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberPlatformStatus
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractMemberDeclaration(
    session: FirSession,
    psi: PsiElement?,
    name: Name,
    final override val visibility: Visibility,
    override val modality: Modality?,
    override val platformStatus: FirMemberPlatformStatus
) : FirAbstractNamedAnnotatedDeclaration(session, psi, name), FirMemberDeclaration {
    final override val typeParameters = mutableListOf<FirTypeParameter>()
}