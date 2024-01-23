/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

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

class TemporaryBirDynamicProperty<E : BirElement, T>(
    override val elementType: BirElementType<E>,
    internal val manager: BirDynamicPropertiesManager,
    internal val validInPhase: BirPhase,
) : BirDynamicPropertyAccessToken<E, T>(), BirDynamicPropertyKey<E, T> {
    override val key: BirDynamicPropertyKey<E, T>
        get() = this

    val isValid: Boolean
        get() = manager.currentPhase === validInPhase
}