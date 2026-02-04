/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers

// Marker annotations to group related checkers together. Can be passed to -Xdisable-ir-checkers instead of individual checkers.

@Target(AnnotationTarget.CLASS)
annotation class IrVarargCheckers

@Target(AnnotationTarget.CLASS)
annotation class IrTypeCheckers