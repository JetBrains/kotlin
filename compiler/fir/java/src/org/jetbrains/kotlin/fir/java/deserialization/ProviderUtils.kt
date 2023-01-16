/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

/**
 * Any top level declarations in core/builtins/src are also available from FirBuiltinSymbolProvider (or FirIdeBuiltinSymbolProvider) for IDE
 * so we filter them out to avoid providing the "same" symbols twice.
 */
val KotlinBuiltins = setOf("kotlin/ArrayIntrinsicsKt", "kotlin/internal/ProgressionUtilKt")