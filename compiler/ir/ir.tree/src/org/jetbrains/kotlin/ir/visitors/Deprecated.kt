/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.ir.visitors

@Deprecated(
    "Use the IrVisitor abstract class instead",
    ReplaceWith("IrVisitor<R, D>", "org.jetbrains.kotlin.ir.visitors.IrVisitor"),
    DeprecationLevel.ERROR,
)
typealias IrElementVisitor<R, D> = IrVisitor<R, D>

@Deprecated(
    "Use the IrVisitorVoid abstract class instead",
    ReplaceWith("IrVisitorVoid", "org.jetbrains.kotlin.ir.visitors.IrVisitorVoid"),
    DeprecationLevel.ERROR,
)
typealias IrElementVisitorVoid = IrVisitorVoid

/**
 * See [KT-75353](https://youtrack.jetbrains.com/issue/KT-75353) for an explanation
 * why this is a marker interface and not a type alias to [IrTransformer].
 */
@Deprecated(
    "Use the IrTransformer abstract class instead",
    ReplaceWith("IrTransformer<D>", "org.jetbrains.kotlin.ir.visitors.IrTransformer"),
    DeprecationLevel.ERROR,
)
interface IrElementTransformer<in D>
