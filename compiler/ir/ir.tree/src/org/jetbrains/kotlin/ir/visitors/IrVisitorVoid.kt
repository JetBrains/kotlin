/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

/**
 * This class is a replacement for the deprecated [IrElementVisitorVoid] (see [KT-61746](https://youtrack.jetbrains.com/issue/KT-61746)).
 *
 * Once we migrate all usages of [IrElementVisitorVoid] to [IrVisitorVoid],
 * it will be made auto-generated, and [IrElementVisitorVoid] will be deleted.
 */
@Suppress("DEPRECATED_COMPILER_API")
abstract class IrVisitorVoid : IrVisitor<Unit, Nothing?>(), IrElementVisitorVoid
