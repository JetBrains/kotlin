/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan

internal val currentCompilerVersion: CompilerVersion =
    CompilerVersionImpl(
        MetaVersion.DEV, 1, 4,
        30, -1, -1)

val CompilerVersion.Companion.CURRENT: CompilerVersion
    get() = currentCompilerVersion
