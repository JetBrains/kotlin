/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl.Modifier.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

open class FirDeclarationStatusImpl(
    override val visibility: Visibility,
    override val modality: Modality?
) : FirPureAbstractElement(), FirDeclarationStatus {
    override val source: KtSourceElement? get() = null
    protected var flags: Int = HAS_STABLE_PARAMETER_NAMES.mask

    operator fun get(modifier: Modifier): Boolean = (flags and modifier.mask) != 0

    operator fun set(modifier: Modifier, value: Boolean) {
        flags = if (value) {
            flags or modifier.mask
        } else {
            flags and modifier.mask.inv()
        }
    }

    override var isExpect: Boolean
        get() = this[EXPECT]
        set(value) {
            this[EXPECT] = value
        }

    override var isActual: Boolean
        get() = this[ACTUAL]
        set(value) {
            this[ACTUAL] = value
        }

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

    override var isExternal: Boolean
        get() = this[EXTERNAL]
        set(value) {
            this[EXTERNAL] = value
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

    override var isFromSealedClass: Boolean
        get() = this[FROM_SEALED]
        set(value) {
            this[FROM_SEALED] = value
        }

    override var isFromEnumClass: Boolean
        get() = this[FROM_ENUM]
        set(value) {
            this[FROM_ENUM] = value
        }

    override var isFun: Boolean
        get() = this[FUN]
        set(value) {
            this[FUN] = value
        }

    override var hasStableParameterNames: Boolean
        get() = this[HAS_STABLE_PARAMETER_NAMES]
        set(value) {
            this[HAS_STABLE_PARAMETER_NAMES] = value
        }

    enum class Modifier(val mask: Int) {
        EXPECT(0x1),
        ACTUAL(0x2),
        OVERRIDE(0x4),
        OPERATOR(0x8),
        INFIX(0x10),
        INLINE(0x20),
        TAILREC(0x40),
        EXTERNAL(0x80),
        CONST(0x100),
        LATEINIT(0x200),
        INNER(0x400),
        COMPANION(0x800),
        DATA(0x1000),
        SUSPEND(0x2000),
        STATIC(0x4000),
        FROM_SEALED(0x8000),
        FROM_ENUM(0x10000),
        FUN(0x20000),
        HAS_STABLE_PARAMETER_NAMES(0x40000),
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDeclarationStatusImpl {
        return this
    }

    fun resolved(
        visibility: Visibility,
        modality: Modality,
        effectiveVisibility: EffectiveVisibility
    ): FirResolvedDeclarationStatusImpl {
        return FirResolvedDeclarationStatusImpl(visibility, modality, effectiveVisibility, flags)
    }
}
