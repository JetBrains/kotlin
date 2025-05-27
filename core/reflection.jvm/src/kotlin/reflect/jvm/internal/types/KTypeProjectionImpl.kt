/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import kotlin.reflect.KTypeProjection

@JvmInline
value class KTypeProjectionImpl(val value: KTypeProjection) : TypeArgumentMarker
