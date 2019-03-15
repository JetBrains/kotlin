/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus

class FirResolvedDeclarationStatusImpl(
    session: FirSession,
    visibility: Visibility,
    modality: Modality
) : FirDeclarationStatusImpl(session, visibility, modality), FirResolvedDeclarationStatus {

    internal constructor(
        session: FirSession,
        visibility: Visibility,
        modality: Modality,
        flags: Int
    ) : this(session, visibility, modality) {
        this.flags = flags
    }

    override val visibility: Visibility
        get() = super.visibility

    override val modality: Modality
        get() = super.modality!!
}