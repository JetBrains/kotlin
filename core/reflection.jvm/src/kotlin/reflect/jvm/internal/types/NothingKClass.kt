/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.KTypeParameterOwnerImpl

/**
 * A [kotlin.reflect.KClass] implementation for `kotlin.Nothing`.
 *
 * Currently, this class is only used in the type checker implementation for kotlin-reflect,
 * but one day it should probably be used to implement KT-15518.
 */
data object NothingKClass : KClass<Void> by Void::class, TypeConstructorMarker, KTypeParameterOwnerImpl {
    override val simpleName: String
        get() = "Nothing"

    override val qualifiedName: String
        get() = "kotlin.Nothing"
}
