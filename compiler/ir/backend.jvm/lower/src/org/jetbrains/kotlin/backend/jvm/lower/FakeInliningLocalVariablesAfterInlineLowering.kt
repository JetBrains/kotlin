/*
 * Copyright 2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineOnly
import org.jetbrains.kotlin.backend.jvm.irInlinerIsEnabled
import org.jetbrains.kotlin.codegen.inline.INLINE_FUN_VAR_SUFFIX
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal val fakeInliningLocalVariablesAfterInlineLowering = makeIrFilePhase<JvmBackendContext>(
    { context ->
        if (!context.irInlinerIsEnabled()) return@makeIrFilePhase FileLoweringPass.Empty
        FakeInliningLocalVariablesAfterInlineLowering(context)
    },
    name = "FakeInliningLocalVariablesAfterInlineLowering",
    description = """Add fake locals to identify the range of inlined functions and lambdas. 
        |This lowering adds fake locals into already inlined blocks.""".trimMargin()
)

// TODO extract common code with FakeInliningLocalVariablesLowering
internal class FakeInliningLocalVariablesAfterInlineLowering(val context: JvmBackendContext) : IrElementVisitor<Unit, IrDeclaration?>, FileLoweringPass {
    private val inlinedStack = mutableListOf<IrInlinedFunctionBlock>()

    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitElement(element: IrElement, data: IrDeclaration?) {
        val newData = if (element is IrDeclaration && element !is IrVariable) element else data
        element.acceptChildren(this, newData)
    }

    override fun visitBlock(expression: IrBlock, data: IrDeclaration?) {
        when {
            expression is IrInlinedFunctionBlock && expression.isFunctionInlining() -> handleInlineFunction(expression, data)
            expression is IrInlinedFunctionBlock && expression.isLambdaInlining() -> handleInlineLambda(expression, data)
            else -> super.visitBlock(expression, data)
        }
    }

    private fun handleInlineFunction(expression: IrInlinedFunctionBlock, data: IrDeclaration?) {
        val declaration = expression.inlineDeclaration

        inlinedStack += expression
        super.visitBlock(expression, data)
        inlinedStack.removeLast()

        if (declaration is IrFunction && declaration.isInline && !declaration.origin.isSynthetic && declaration.body != null && !declaration.isInlineOnly()) {
            val currentFunctionName = context.defaultMethodSignatureMapper.mapFunctionName(declaration)
            val localName = "${JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION}$currentFunctionName"
            //declaration.addFakeLocalVariable(localName)
            with(context.createIrBuilder(data!!.symbol)) {
                val tmpVar =
                    scope.createTmpVariable(irInt(0), localName.removeSuffix(FOR_INLINE_SUFFIX), origin = IrDeclarationOrigin.DEFINED)
                // TODO maybe add in front of inline block
                expression.putStatementsInFrontOfInlinedFunction(listOf(tmpVar))
            }
        }

        expression.processLocalDeclarations()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleInlineLambda(expression: IrInlinedFunctionBlock, data: IrDeclaration?) {
        // TODO
    }

    private fun IrInlinedFunctionBlock.processLocalDeclarations() {
        this.getAdditionalStatementsFromInlinedBlock().forEach {
            if (it is IrVariable && it.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE) {
                val varName = it.name.asString().substringAfterLast("_")
                it.name = Name.identifier((if (varName == SpecialNames.THIS.asString()) "this_" else varName) + INLINE_FUN_VAR_SUFFIX)
                it.origin = IrDeclarationOrigin.DEFINED
            }
        }

        this.getOriginalStatementsFromInlinedBlock().forEach {
            it.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitVariable(declaration: IrVariable) {
                    val varName = declaration.name.asString()
                    declaration.name = when {
                        varName == SpecialNames.THIS.asString() -> {
                            Name.identifier("this_$INLINE_FUN_VAR_SUFFIX")
                        }

                        !varName.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) -> {
                            Name.identifier(varName + INLINE_FUN_VAR_SUFFIX)
                        }

                        else -> declaration.name
                    }
                    super.visitVariable(declaration)
                }
            })
        }
    }
}
