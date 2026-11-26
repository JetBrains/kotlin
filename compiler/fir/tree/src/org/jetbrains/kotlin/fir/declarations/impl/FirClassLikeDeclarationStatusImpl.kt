/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclarationStatus

open class FirClassLikeDeclarationStatusImpl(
    visibility: Visibility,
    modality: Modality?
) : FirDeclarationStatusBaseImpl(visibility, modality), FirClassLikeDeclarationStatus {
    override var isInline: Boolean
        get() = this[INLINE]
        set(value) {
            this[INLINE] = value
        }

    override var isValue: Boolean
        get() = this[VALUE]
        set(value) {
            this[VALUE] = value
        }

    override var isInner: Boolean
        get() = this[INNER]
        set(value) {
            this[INNER] = value
        }

    override var isCompanion: Boolean
        get() = this[COMPANION]
        set(value) {
            this[COMPANION] = value
        }

    override var isData: Boolean
        get() = this[DATA]
        set(value) {
            this[DATA] = value
        }

    override var isFun: Boolean
        get() = this[FUN]
        set(value) {
            this[FUN] = value
        }

    override var isRichError: Boolean
        get() = this[RICH_ERROR]
        set(value) {
            this[RICH_ERROR] = value
        }

    override fun resolved(
        visibility: Visibility,
        modality: Modality,
        effectiveVisibility: EffectiveVisibility
    ): FirResolvedClassLikeDeclarationStatusImpl {
        return FirResolvedClassLikeDeclarationStatusImpl(visibility, modality, effectiveVisibility, flags)
    }
}

class FirClassLikeDeclarationStatusWithAlteredDefaults(
    visibility: Visibility,
    modality: Modality?,
    override val defaultVisibility: Visibility,
    override val defaultModality: Modality,
) : FirClassLikeDeclarationStatusImpl(visibility, modality) {
    internal constructor(
        visibility: Visibility,
        modality: Modality?,
        defaultVisibility: Visibility,
        defaultModality: Modality,
        flags: Int
    ) : this(visibility, modality, defaultVisibility, defaultModality) {
        this.flags = flags
    }

    override fun resolved(
        visibility: Visibility,
        modality: Modality,
        effectiveVisibility: EffectiveVisibility
    ): FirResolvedClassLikeDeclarationStatusWithAlteredDefaults {
        return FirResolvedClassLikeDeclarationStatusWithAlteredDefaults(
            visibility,
            modality,
            defaultVisibility,
            defaultModality,
            effectiveVisibility,
            flags
        )
    }
}
