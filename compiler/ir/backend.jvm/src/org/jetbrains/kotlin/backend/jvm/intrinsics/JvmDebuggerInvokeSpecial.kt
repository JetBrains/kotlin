/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.ir.getBooleanConstArgument
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.org.objectweb.asm.Type

// This intrinsic enables IR lowerings to force the generation of a particular
// invokeSpecial instruction in the resulting JVM bytecode.
//
// The need for this is to coerce the IR codegen backend to generate an
// otherwise illegal invokeSpecial for the express purpose of being
// _interpreted_ by eval4j in the fragment evaluator and not actually run on
// the JVM. This allows the "evaluate expression" functionality of the Kotlin
// JVM Debugger Plug-in in IntelliJ to simulate the invocation of `super` calls
// in the context of a breakpoint.
//
// It uses the "trick" of encoding the desired operands as constants passed as
// arguments to the intrinsic, opening a direct line from the producing
// lowering straight through to JVM codegen without interference from
// lowerings in between.
object JvmDebuggerInvokeSpecial : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        val owner = expression.getStringConstArgument(0)
        val name = expression.getStringConstArgument(1)
        val descriptor = expression.getStringConstArgument(2)
        val isInterface = expression.getBooleanConstArgument(3)

        expression.dispatchReceiver!!.accept(codegen, data).materialize()
        codegen.mv.invokespecial(owner, name, descriptor, isInterface)

        return MaterialValue(codegen, Type.getReturnType(descriptor), expression.type)
    }
}
