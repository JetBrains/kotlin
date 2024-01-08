/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

/**
 * - [MppCheckerKind.Common] means that this checker should run from the same
 *   session to which corresponding declaration belongs
 * - [MppCheckerKind.Platform] means that in case of MPP compilation this
 *   checker should run with session of leaf platform module for sources
 *   of all modules
 *
 *  For more information see the doc: compiler/fir/checkers/module.md
 */
enum class MppCheckerKind {
    Common, Platform
}

interface FirCheckerWithMppKind {
    val mppKind: MppCheckerKind
}
