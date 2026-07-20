/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

// whether some read of the variable can be executed many times (it is inside a loop, a function or a class nested in the declaration scope)
internal var IrValueDeclaration.isReadInRepeatedContext: Boolean? by irAttribute(true)

// the number of places the variable is read at statically in the code
internal var IrValueDeclaration.readCounter: Int? by irAttribute(true)

internal class ReusedSequenceMarker(val context: JvmBackendContext) : IrVisitorVoid() {
    // sequence variables with the number of loops, functions and classes they are declared in
    val sequences = mutableMapOf<IrVariable, Int>()
    private var loopOrFunctionDepth = 0

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitLoop(loop: IrLoop) {
        loopOrFunctionDepth++
        super.visitLoop(loop)
        loopOrFunctionDepth--
    }

    override fun visitFunction(declaration: IrFunction) {
        loopOrFunctionDepth++
        super.visitFunction(declaration)
        loopOrFunctionDepth--
    }

    // e.g. property initializers and `init` blocks of a local class are executed for every instance
    override fun visitClass(declaration: IrClass) {
        loopOrFunctionDepth++
        super.visitClass(declaration)
        loopOrFunctionDepth--
    }

    override fun visitGetValue(expression: IrGetValue) {
        val declarationDepth = sequences[expression.symbol.owner]
        if (declarationDepth != null) {
            // a usage inside a loop or a lambda can be executed many times
            val isRepeatedHere = loopOrFunctionDepth > declarationDepth
            expression.symbol.owner.isReadInRepeatedContext = isRepeatedHere || (expression.symbol.owner.isReadInRepeatedContext ?: false)
            expression.symbol.owner.readCounter = (expression.symbol.owner.readCounter ?: 0) + 1
        }
        super.visitGetValue(expression)
    }

    override fun visitVariable(declaration: IrVariable) {
        if (declaration.initializer != null && isSequenceType(context, declaration.initializer!!)) {
            sequences[declaration] = loopOrFunctionDepth
        }
        super.visitVariable(declaration)
    }
}
