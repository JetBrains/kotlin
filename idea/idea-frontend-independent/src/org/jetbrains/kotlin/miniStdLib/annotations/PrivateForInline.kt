/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.miniStdLib.annotations

/**
 * This annotation should be used when the declaration is semantically inline
 * but cannot be marked as such because it should be used in some inline function
 */
@RequiresOptIn
annotation class PrivateForInline
