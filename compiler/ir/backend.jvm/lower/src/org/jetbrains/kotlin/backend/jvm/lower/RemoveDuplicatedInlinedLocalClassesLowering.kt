/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.getDefaultAdditionalStatementsFromInlinedBlock
import org.jetbrains.kotlin.backend.common.ir.getNonDefaultAdditionalStatementsFromInlinedBlock
import org.jetbrains.kotlin.backend.common.ir.getOriginalStatementsFromInlinedBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.functionInliningPhase
import org.jetbrains.kotlin.backend.jvm.localDeclarationsPhase
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

internal val removeDuplicatedInlinedLocalClasses = makeIrFilePhase(
    ::RemoveDuplicatedInlinedLocalClassesLowering,
    name = "RemoveDuplicatedInlinedLocalClasses",
    description = "Drop excess local classes that were copied by ir inliner",
    prerequisite = setOf(functionInliningPhase, localDeclarationsPhase)
)

// There are three types of inlined local classes:
// 1. MUST BE regenerated according to set of rules in AnonymousObjectTransformationInfo.
// They all have `attributeOwnerIdBeforeInline != null`.
// 2. MUST NOT BE regenerated and MUST BE CREATED only once because they are copied from call site.
// This lambda will not exist after inline, so we copy declaration into new temporary inline call `singleArgumentInlineFunction`.
// 3. MUST NOT BE created at all because will be created at callee site.
// This lowering drops declarations that correspond to second and third type.
class RemoveDuplicatedInlinedLocalClassesLowering(val context: JvmBackendContext) : IrElementTransformer<Boolean>, FileLoweringPass {
    private var insideInlineBlock = false
    private val visited = mutableSetOf<IrElement>()

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, false)
    }

    override fun visitCall(expression: IrCall, data: Boolean): IrElement {
        if (expression.symbol == context.ir.symbols.singleArgumentInlineFunction) return expression
        return super.visitCall(expression, data)
    }

    override fun visitBlock(expression: IrBlock, data: Boolean): IrExpression {
        if (expression is IrInlinedFunctionBlock) {
            val oldInsideInlineBlock = insideInlineBlock
            insideInlineBlock = true
            expression.getNonDefaultAdditionalStatementsFromInlinedBlock().forEach { it.transform(this, false) }
            expression.getDefaultAdditionalStatementsFromInlinedBlock().forEach { it.transform(this, true) }
            expression.getOriginalStatementsFromInlinedBlock().forEach { it.transform(this, true) }
            insideInlineBlock = oldInsideInlineBlock
            return expression
        }

        return super.visitBlock(expression, data)
    }

    // Basically we want to remove all anonymous classes after inline. Exceptions are:
    // 1. classes that must be regenerated (declaration.attributeOwnerIdBeforeInline != null)
    // 2. classes that are originally declared on call site or are default lambdas (data == true)
    override fun visitClass(declaration: IrClass, data: Boolean): IrStatement {
        if (!insideInlineBlock || declaration.attributeOwnerIdBeforeInline != null || !data) {
            return super.visitClass(declaration, data)
        }

        // TODO big note. Here we drop anonymous class declaration but there still present a constructor call that has reference to this class.
        //  Everything works fine because somewhere in code we still have class declaration with the same name. So this doesn't ruin code
        //  generation and will not drop on runtime, but this is something to be aware of.
        return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: Boolean): IrElement {
        if (!visited.add(expression.symbol.owner)) return expression
        expression.symbol.owner.accept(this, data)
        return super.visitFunctionReference(expression, data)
    }
}
