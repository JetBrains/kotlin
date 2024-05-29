/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import java.lang.ref.WeakReference

interface BirDynamicPropertyKey<E : BirElement, T> {
    val elementType: BirElementType<E>
}

sealed class BirDynamicPropertyAccessToken<E : BirElement, T> {
    abstract val key: BirDynamicPropertyKey<E, T>
}


class GlobalBirDynamicProperty<E : BirElement, T>(
    override val elementType: BirElementType<E>
) : BirDynamicPropertyAccessToken<E, T>(), BirDynamicPropertyKey<E, T> {
    override val key: BirDynamicPropertyKey<E, T>
        get() = this
}

class PhaseLocalBirDynamicProperty<E : BirElement, T>(
    override val elementType: BirElementType<E>,
    private val manager: BirDynamicPropertiesManager,
    validInPhase: BirPhase,
) : BirDynamicPropertyAccessToken<E, T>(), BirDynamicPropertyKey<E, T> {
    override val key: BirDynamicPropertyKey<E, T>
        get() = this

    internal val validInPhase = WeakReference(validInPhase)

    val isValid: Boolean
        get() = manager.currentPhase === validInPhase.get()
}