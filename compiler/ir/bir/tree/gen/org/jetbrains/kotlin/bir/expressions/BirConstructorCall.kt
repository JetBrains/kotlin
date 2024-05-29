/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirConstructorSymbol
import org.jetbrains.kotlin.bir.util.BirImplementationDetail
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.types.model.AnnotationMarker

abstract class BirConstructorCall() : BirFunctionAccessExpression(), AnnotationMarker {
    abstract override var symbol: BirConstructorSymbol

    abstract var source: SourceElement

    abstract var constructorTypeArgumentsCount: Int

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        dispatchReceiver?.accept(data, visitor)
        extensionReceiver?.accept(data, visitor)
        valueArguments.acceptChildren(visitor, data)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirConstructorCall

    companion object : BirElementClass<BirConstructorCall>(BirConstructorCall::class.java, 21, true)
}
