/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatusBase
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatusBase
import org.jetbrains.kotlin.fir.declarations.impl.DeclarationStatusModifier.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirDeclarationStatusBaseImpl(
    override val visibility: Visibility,
    override val modality: Modality?
) : FirPureAbstractElement(), FirDeclarationStatusBase {
    override val source: KtSourceElement? get() = null
    protected var flags: Int = HAS_STABLE_PARAMETER_NAMES.mask

    @FirImplementationDetail
    internal val rawFlags get() = flags

    operator fun get(modifier: DeclarationStatusModifier): Boolean = (flags and modifier.mask) != 0

    operator fun set(modifier: DeclarationStatusModifier, value: Boolean) {
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

    override var isExternal: Boolean
        get() = this[EXTERNAL]
        set(value) {
            this[EXTERNAL] = value
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

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDeclarationStatusBaseImpl {
        return this
    }

    override val defaultModality: Modality
        get() = Modality.FINAL

    override val defaultVisibility: Visibility
        get() = Visibilities.DEFAULT_VISIBILITY

    abstract fun resolved(
        visibility: Visibility,
        modality: Modality,
        effectiveVisibility: EffectiveVisibility
    ): FirResolvedDeclarationStatusBase
}

@OptIn(FirImplementationDetail::class)
val FirDeclarationStatusBase.modifiersRepresentation: Int
    get() = when (this) {
        is FirDeclarationStatusBaseImpl -> rawFlags
        else -> error("Generating modifier representations for ${this::class.simpleName} is not supported")
    }
