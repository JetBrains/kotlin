/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl

import org.jetbrains.kotlin.arguments.dsl.base.asReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.PathType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType

val BooleanType.Companion.defaultFalse: BooleanType
    get() = BooleanType(
        isNullable = false.asReleaseDependent(),
        defaultValue = false.asReleaseDependent()
    )

val BooleanType.Companion.defaultTrue: BooleanType
    get() = BooleanType(
        isNullable = false.asReleaseDependent(),
        defaultValue = true.asReleaseDependent()
    )

val BooleanType.Companion.defaultNull: BooleanType
    get() = BooleanType(
        isNullable = true.asReleaseDependent(),
        defaultValue = null.asReleaseDependent()
    )

val StringType.Companion.defaultNull: StringType
    get() = StringType()

val StringArrayType.Companion.defaultNull: StringArrayType
    get() = StringArrayType()

val IntType.Companion.defaultOne: IntType
    get() = IntType(
        defaultValue = 1.asReleaseDependent(),
    )

val PathType.Companion.defaultNull: PathType
    get() = PathType()
