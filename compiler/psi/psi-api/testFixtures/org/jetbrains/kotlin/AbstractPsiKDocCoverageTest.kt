/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.name.FqName

abstract class AbstractPsiKDocCoverageTest : AbstractKDocCoverageTest() {
    override val ignoredPackages: List<FqName> = listOf(
        FqName("org.jetbrains.kotlin.idea"),
        FqName("org.jetbrains.kotlin.lexer"),
        FqName("org.jetbrains.kotlin.parsing"),
        FqName("org.jetbrains.kotlin.kdoc.lexer"),
        FqName("org.jetbrains.kotlin.kdoc.parser"),
    )
}