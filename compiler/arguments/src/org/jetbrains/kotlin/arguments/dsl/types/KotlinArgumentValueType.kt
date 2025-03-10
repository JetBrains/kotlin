/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.jetbrains.kotlin.arguments.dsl.base.ReleaseDependent

sealed interface KotlinArgumentValueType<T : Any> {
    val isNullable: ReleaseDependent<Boolean>
    val defaultValue: ReleaseDependent<T?>
}

object AllKotlinArgumentTypes
