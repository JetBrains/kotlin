/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

const val K1_DEPRECATION_WARNING = "This declaration is part of K1 API which is going to be removed in future releases."

@RequiresOptIn(
    message = "This declaration is part of K1 API, which is planned to be deprecated and reworked.",
    level = RequiresOptIn.Level.ERROR,
)
annotation class K1Deprecation
