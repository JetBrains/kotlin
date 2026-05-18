/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.AnnotationMarker

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.annotation]
 */
abstract class IrAnnotation : IrConstructorCall(), AnnotationMarker {
    abstract val classSymbol: IrClassSymbol

    abstract val argumentMapping: Map<Name, IrExpression?>

    @DeprecatedCompilerApi(deprecatedSince = org.jetbrains.kotlin.CompilerVersionOfApiDeprecation._2_4_20)
    abstract override var symbol: IrConstructorSymbol

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitAnnotation(this, data)
}
