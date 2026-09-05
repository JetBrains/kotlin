/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.mapping.IrCallableMethod
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
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
        (val node, val smap = classSMAP) = codegen.classCodegen.generateMethodNode(function)
        val argsSize = argumentsSize(callableMethod.asmMethod.descriptor, function.isStatic)
        val scopesGenerator = codegen.inlineScopesGenerator
        val scopeNumberOffset = scopesGenerator?.inlinedScopes ?: 0
        var copiedScopes = 0
        val mv = object : MethodBodyVisitor(codegen.visitor) {
            override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label, end: Label, index: Int) {
                // We only copy LVT entries for local variables, since we already generated entries for the method parameters.
                if (index < argsSize) return
                if (scopesGenerator == null) {
                    super.visitLocalVariable(name, desc, signature, start, end, index)
                    return
                }
                val info = name.getInlineScopeInfo()
                if (info == null) {
                    // The bare marker of the implementation method is copied as is. It doesn't
                    // start at the beginning of the stub, so InlineScopesGenerator must recognize
                    // it by the absence of scope info, not by its position (KT-88995).
                    super.visitLocalVariable(name, desc, signature, start, end, index)
                    return
                }
                // Scope numbering restarts in every generated method, so the scope numbers copied
                // from the implementation method would clash with the numbers already given out
                // while inlining default argument values. Shift them to keep the numbering of the
                // stub consecutive.
                if (JvmAbi.isFakeLocalVariableForInline(name)) {
                    copiedScopes += 1
                }
                super.visitLocalVariable(name.shiftScopeNumbers(info, scopeNumberOffset), desc, signature, start, end, index)
            }
        }
        node.accept(SourceMapCopyingMethodVisitor(codegen.smap, smap, mv))
        scopesGenerator?.apply { inlinedScopes += copiedScopes }
    }

    private fun String.shiftScopeNumbers(info: InlineScopeInfo, offset: Int): String {
        if (offset == 0) return this
        val result = dropInlineScopeInfo().addScopeInfo(info.scopeNumber + offset)
        val callSiteLineNumber = info.callSiteLineNumber ?: return result
        val surroundingScopeNumber = info.surroundingScopeNumber
            ?: return result.addScopeInfo(callSiteLineNumber)
        // A surrounding scope number of 0 denotes the enclosing function itself, not a numbered scope.
        val newSurroundingScopeNumber = if (surroundingScopeNumber == 0) 0 else surroundingScopeNumber + offset
        return result.addScopeInfo(callSiteLineNumber).addScopeInfo(newSurroundingScopeNumber)
    }
}
