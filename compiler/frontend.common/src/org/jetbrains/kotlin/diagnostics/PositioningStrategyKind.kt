/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

/**
 * Identifies a parse-tree agnostic positioning strategy of a diagnostic.
 * Its implementation for the concrete parse tree is provided by [org.jetbrains.kotlin.syntax.ParseTreeKind.positioningStrategy].
 */
abstract class PositioningStrategyKind
