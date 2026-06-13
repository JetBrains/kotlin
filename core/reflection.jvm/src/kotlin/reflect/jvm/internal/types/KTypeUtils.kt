/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf

internal fun areEqualKTypes(a: KType, b: KType): Boolean =
    a.isSubtypeOf(b) && b.isSubtypeOf(a)
