/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.metadata.Modality

internal abstract class ReflectKCallableImpl<out R>(
    override val overriddenStorage: KCallableOverriddenStorage,
) : ReflectKCallable<R> {
    private val _absentArguments = ReflectProperties.lazySoft(::computeAbsentArguments)

    override fun getAbsentArguments(): Array<Any?> = _absentArguments().clone()

    final override val isFinal: Boolean
        get() = modality == Modality.FINAL

    final override val isOpen: Boolean
        get() = modality == Modality.OPEN

    final override val isAbstract: Boolean
        get() = modality == Modality.ABSTRACT
}
