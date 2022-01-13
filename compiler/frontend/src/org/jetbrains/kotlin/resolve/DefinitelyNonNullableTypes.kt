/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.types.DefinitelyNotNullType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

fun KotlinType.containsIncorrectExplicitDefinitelyNonNullableType(): Boolean = contains {
    it is DefinitelyNotNullType && it.original.isTypeParameter() && !it.original.isNullable()
}
