/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclarationStatus
import org.jetbrains.kotlin.resolve.ReturnValueStatus

open class FirCallableDeclarationStatusImpl(
    visibility: Visibility,
    modality: Modality?
) : FirDeclarationStatusImpl(visibility, modality), FirCallableDeclarationStatus {
    override var isOverride: Boolean
        get() = this[OVERRIDE]
        set(value) {
            this[OVERRIDE] = value
        }

    override var isOperator: Boolean
        get() = this[OPERATOR]
        set(value) {
            this[OPERATOR] = value
        }

    override var isInfix: Boolean
        get() = this[INFIX]
        set(value) {
            this[INFIX] = value
        }

    override var isInline: Boolean
        get() = this[INLINE]
        set(value) {
            this[INLINE] = value
        }

    override var isTailRec: Boolean
        get() = this[TAILREC]
        set(value) {
            this[TAILREC] = value
        }

    override var isConst: Boolean
        get() = this[CONST]
        set(value) {
            this[CONST] = value
        }

    override var isLateInit: Boolean
        get() = this[LATEINIT]
        set(value) {
            this[LATEINIT] = value
        }

    override var isSuspend: Boolean
        get() = this[SUSPEND]
        set(value) {
            this[SUSPEND] = value
        }

    override var isStatic: Boolean
        get() = this[STATIC]
        set(value) {
            this[STATIC] = value
        }

    override var hasStableParameterNames: Boolean
        get() = this[HAS_STABLE_PARAMETER_NAMES]
        set(value) {
            this[HAS_STABLE_PARAMETER_NAMES] = value
        }

    override var returnValueStatus: ReturnValueStatus
        get() = ReturnValueStatus.fromBitFlags(this[HAS_MUST_USE_RETURN_VALUE], this[HAS_IGNORABLE_RETURN_VALUE])
        set(value) {
            flags = flags and (DeclarationStatusModifier.HAS_MUST_USE_RETURN_VALUE.mask or DeclarationStatusModifier.HAS_IGNORABLE_RETURN_VALUE.mask).inv()
            when (value) {
                MustUse ->
                    this[HAS_MUST_USE_RETURN_VALUE] = true
                ExplicitlyIgnorable ->
                    this[HAS_IGNORABLE_RETURN_VALUE] = true
                else -> Unit // flags are already cleared
            }
        }

    override fun resolved(
        visibility: Visibility,
        modality: Modality,
        effectiveVisibility: EffectiveVisibility
    ): FirResolvedCallableDeclarationStatusImpl {
        return FirResolvedCallableDeclarationStatusImpl(visibility, modality, effectiveVisibility, flags)
    }
}

class FirCallableDeclarationStatusWithAlteredDefaults(
    visibility: Visibility,
    modality: Modality?,
    override val defaultVisibility: Visibility,
    override val defaultModality: Modality,
) : FirCallableDeclarationStatusImpl(visibility, modality) {
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
    ): FirResolvedCallableDeclarationStatusWithAlteredDefaults {
        return FirResolvedCallableDeclarationStatusWithAlteredDefaults(
            visibility,
            modality,
            defaultVisibility,
            defaultModality,
            effectiveVisibility,
            flags
        )
    }
}
