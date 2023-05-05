/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus

class FirResolvedDeclarationStatusImpl(
    visibility: Visibility,
    modality: Modality,
    override val effectiveVisibility: EffectiveVisibility
) : FirDeclarationStatusImpl(visibility, modality), FirResolvedDeclarationStatus {

    companion object {
        val DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            EffectiveVisibility.Public
        )
        val DEFAULT_STATUS_FOR_SUSPEND_FUNCTION_EXPRESSION = FirResolvedDeclarationStatusImpl(
            Visibilities.Local,
            Modality.FINAL,
            EffectiveVisibility.Public
        ).apply { isSuspend = true }
    }

    internal constructor(
        visibility: Visibility,
        modality: Modality,
        effectiveVisibility: EffectiveVisibility,
        flags: Int
    ) : this(visibility, modality, effectiveVisibility) {
        this.flags = flags
    }

    override val visibility: Visibility
        get() = super.visibility

    override val modality: Modality
        get() = super.modality!!
}
