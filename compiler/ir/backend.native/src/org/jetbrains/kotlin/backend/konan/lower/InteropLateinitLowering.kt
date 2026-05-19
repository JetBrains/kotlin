/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.PreSerializationNativeSymbols
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Cleans up certain local variables (marked with certain annotations like `@kotlinx.cinterop.StackAlloc`)
 * so they can flow through the rest of the pipeline as plain uninitialized non-nullable declarations.
 *
 * The frontend forces such a variable (with no initializer) to be declared as `lateinit`.
 * FE doesn't know that it will be initialized at codegen, so this lowering strips the `lateinit` modifier
 * and initializes it with null to preserve the IR in consistent state.
 */
class InteropLateinitLowering(context: LoweringContext) : FileLoweringPass {
    private val stackAllocSymbol = (context.symbols as PreSerializationNativeSymbols).interopStackAlloc
    private val cudaSharedSymbol = (context.symbols as PreSerializationNativeSymbols).cudaShared

    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitVariable(declaration: IrVariable) {
                super.visitVariable(declaration)

                if (!declaration.hasAnnotation(stackAllocSymbol) && !declaration.hasAnnotation(cudaSharedSymbol)) return
                declaration.isLateinit = false
                declaration.isVar = false
                // Unused, needs only to keep the IR in consistent state.
                declaration.initializer = IrConstImpl(
                    declaration.startOffset, declaration.endOffset, declaration.type.makeNullable(), IrConstKind.Null, null
                )
            }
        })
    }
}
