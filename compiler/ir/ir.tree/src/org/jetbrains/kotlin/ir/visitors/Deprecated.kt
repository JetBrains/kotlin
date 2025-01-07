/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.ir.visitors

@Deprecated(
    "Use the IrVisitorVoid abstract class instead",
    ReplaceWith("IrVisitorVoid", "org.jetbrains.kotlin.ir.visitors.IrVisitorVoid"),
    DeprecationLevel.ERROR,
)
typealias IrElementVisitorVoid = IrVisitorVoid

@Deprecated(
    "Use the IrTransformer abstract class instead",
    ReplaceWith("IrTransformer<D>", "org.jetbrains.kotlin.ir.visitors.IrTransformer"),
    DeprecationLevel.ERROR,
)
typealias IrElementTransformer<D> = IrTransformer<D>
