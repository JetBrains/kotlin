/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.mapping.IrCallableMethod
import org.jetbrains.kotlin.codegen.inline.MethodBodyVisitor
import org.jetbrains.kotlin.codegen.inline.SourceMapCopier
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

/**
 * A specialization of IrInlineCodegen for calls to the underlying method in a $default handler.
 * Such calls are inlined verbatim in the JVM backend (see InlineCodegenForDefaultBody.kt).
 * For compatibility we have to do the same thing in the JVM IR backend.
 */
object IrInlineDefaultCodegen : IrInlineCallGenerator {
    override fun genValueAndPut(
        irValueParameter: IrValueParameter,
        argumentExpression: IrExpression,
        parameterType: Type,
        codegen: ExpressionCodegen,
        blockInfo: BlockInfo
    ) {
        // This codegen is only used for calls to the underlying function in a $default stub.
        // For such calls we know that we are passing along the value parameters and reusing the same indices.
        // There is no need to generate any code.
        assert(argumentExpression is IrGetValue || argumentExpression is IrTypeOperatorCall && argumentExpression.argument is IrGetValue)
    }

    override fun genInlineCall(
        callableMethod: IrCallableMethod,
        codegen: ExpressionCodegen,
        expression: IrFunctionAccessExpression,
        isInsideIfCondition: Boolean
    ) {
        val function = expression.symbol.owner
        val nodeAndSmap = codegen.classCodegen.generateMethodNode(function)
        val childSourceMapper = SourceMapCopier(codegen.smap, nodeAndSmap.classSMAP)

        val argsSize =
            (Type.getArgumentsAndReturnSizes(callableMethod.asmMethod.descriptor) ushr 2) - if (function.isStatic) 1 else 0
        nodeAndSmap.node.accept(object : MethodBodyVisitor(codegen.visitor) {
            override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label, end: Label, index: Int) {
                // We only copy LVT entries for local variables, since we already generated entries for the method parameters,
                if (index >= argsSize) super.visitLocalVariable(name, desc, signature, start, end, index)
            }

            override fun visitLineNumber(line: Int, start: Label?) {
                super.visitLineNumber(childSourceMapper.mapLineNumber(line), start)
            }
        })
    }
}
