/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

/**
 * [PrivateForInline] used for cases when there is a var property with mutable set and corresponding
 *   inline function which mutates this var
 */
@RequiresOptIn
annotation class PrivateForInline
