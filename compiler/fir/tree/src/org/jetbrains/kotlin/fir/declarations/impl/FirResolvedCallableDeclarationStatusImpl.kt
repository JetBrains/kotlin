/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.declarations.FirResolvedCallableDeclarationStatus

open class FirResolvedCallableDeclarationStatusImpl(
    visibility: Visibility,
    modality: Modality,
    override val effectiveVisibility: EffectiveVisibility
) : FirCallableDeclarationStatusImpl(visibility, modality), FirResolvedCallableDeclarationStatus {

    companion object {
        val DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS: FirResolvedCallableDeclarationStatus = FirResolvedCallableDeclarationStatusImpl(
            visibility = Visibilities.Public,
            modality = Modality.FINAL,
            effectiveVisibility = EffectiveVisibility.Public
        )
    }

    internal constructor(
        visibility: Visibility,
        modality: Modality,
        effectiveVisibility: EffectiveVisibility,
        flags: Int
    ) : this(visibility, modality, effectiveVisibility) {
        this.flags = flags
    }

    override val modality: Modality
        get() = super.modality!!
}

class FirResolvedCallableDeclarationStatusWithAlteredDefaults(
    visibility: Visibility,
    modality: Modality,
    override val defaultVisibility: Visibility,
    override val defaultModality: Modality,
    effectiveVisibility: EffectiveVisibility,
) : FirResolvedCallableDeclarationStatusImpl(visibility, modality, effectiveVisibility) {
    internal constructor(
        visibility: Visibility,
        modality: Modality,
        defaultVisibility: Visibility,
        defaultModality: Modality,
        effectiveVisibility: EffectiveVisibility,
        flags: Int
    ) : this(visibility, modality, defaultVisibility, defaultModality, effectiveVisibility) {
        this.flags = flags
    }
}

class FirResolvedCallableDeclarationStatusWithLazyEffectiveVisibility(
    visibility: Visibility,
    modality: Modality,
    @property:FirImplementationDetail
    val lazyEffectiveVisibility: Lazy<EffectiveVisibility>,
) : FirCallableDeclarationStatusImpl(visibility, modality), FirResolvedCallableDeclarationStatus {
    @OptIn(FirImplementationDetail::class)
    override val effectiveVisibility: EffectiveVisibility
        get() = lazyEffectiveVisibility.value

    override val modality: Modality
        get() = super.modality!!
}
