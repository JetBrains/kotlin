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
import org.jetbrains.kotlin.backend.jvm.irInlinerIsEnabled
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.INLINE_FUN_VAR_SUFFIX
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.util.inlineDeclaration
import org.jetbrains.kotlin.ir.util.isFunctionInlining
import org.jetbrains.kotlin.ir.util.isLambdaInlining
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal val fakeLocalVariablesForIrInlinerLowering = makeIrFilePhase<JvmBackendContext>(
    { context ->
        if (!context.irInlinerIsEnabled()) return@makeIrFilePhase FileLoweringPass.Empty
        FakeLocalVariablesForIrInlinerLowering(context)
    },
    name = "FakeLocalVariablesForIrInlinerLowering",
    description = """Add fake locals to identify the range of inlined functions and lambdas. 
        |This lowering adds fake locals into already inlined blocks.""".trimMargin()
)

internal class FakeLocalVariablesForIrInlinerLowering(
    override val context: JvmBackendContext
) : IrElementVisitorVoid, FakeInliningLocalVariables<IrInlinedFunctionBlock>, FileLoweringPass {
    private val inlinedStack = mutableListOf<IrInlinedFunctionBlock>()
    private var container: IrDeclaration? = null

    private fun IrInlinedFunctionBlock.insertInStackAndProcess() {
        inlinedStack += this
        this.acceptChildren(this@FakeLocalVariablesForIrInlinerLowering, null)
        inlinedStack.removeLast()
    }

    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
        irFile.acceptVoid(FunctionParametersProcessor())
        irFile.accept(LocalVariablesProcessor(), LocalVariablesProcessor.Data(processingOriginalDeclarations = false))
    }

    override fun visitElement(element: IrElement) {
        val oldContainer = container
        try {
            container = if (element is IrDeclaration && element !is IrVariable) element else container
            element.acceptChildren(this, null)
        } finally {
            container = oldContainer
        }
    }

    override fun visitBlock(expression: IrBlock) {
        when {
            expression is IrInlinedFunctionBlock && expression.isFunctionInlining() -> handleInlineFunction(expression)
            expression is IrInlinedFunctionBlock && expression.isLambdaInlining() -> handleInlineLambda(expression)
            else -> super.visitBlock(expression)
        }
    }

    private fun handleInlineFunction(expression: IrInlinedFunctionBlock) {
        expression.insertInStackAndProcess()
        val declaration = expression.inlineDeclaration
        expression.addFakeLocalVariableForFun(declaration)
    }

    private fun handleInlineLambda(expression: IrInlinedFunctionBlock) {
        expression.insertInStackAndProcess()
        // `inlinedElement` here can be either `IrFunctionExpression` or `IrFunctionReference`, so cast must be safe
        val argument = expression.inlinedElement as IrAttributeContainer
        val callee = inlinedStack.extractDeclarationWhereGivenElementWasInlined(argument) as? IrFunction ?: return
        expression.addFakeLocalVariableForLambda(argument, callee)
    }

    override fun IrInlinedFunctionBlock.addFakeLocalVariable(name: String) {
        with(context.createIrBuilder(container!!.symbol)) {
            val tmpVar = scope.createTmpVariable(
                irInt(0), name.removeSuffix(FOR_INLINE_SUFFIX), origin = IrDeclarationOrigin.DEFINED
            )
            this@addFakeLocalVariable.putStatementsInFrontOfInlinedFunction(listOf(tmpVar))
        }
    }
}

private class LocalVariablesProcessor : IrElementVisitor<Unit, LocalVariablesProcessor.Data> {
    data class Data(val processingOriginalDeclarations: Boolean)

    private val inlinedStack = mutableListOf<IrInlinedFunctionBlock>()

    private inline fun IrInlinedFunctionBlock.insertInStackAndProcess(block: IrInlinedFunctionBlock.() -> Unit) {
        inlinedStack += this
        block()
        inlinedStack.removeLast()
    }

    override fun visitElement(element: IrElement, data: Data) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: Data) {
        if (declaration.originalBeforeInline != null) {
            // Don't take into account regenerated classes
            return super.visitClass(declaration, data.copy(processingOriginalDeclarations = false))
        }
        super.visitClass(declaration, data)
    }

    override fun visitBlock(expression: IrBlock, data: Data) {
        if (expression !is IrInlinedFunctionBlock) {
            return super.visitBlock(expression, data)
        }

        if (expression.isLambdaInlining()) {
            val argument = expression.inlinedElement as IrAttributeContainer
            val callee = inlinedStack.extractDeclarationWhereGivenElementWasInlined(argument)
            if (callee == null || callee != inlinedStack.lastOrNull()) return
        }

        super.visitBlock(expression, data)

        expression.insertInStackAndProcess {
            getOriginalStatementsFromInlinedBlock().forEach {
                it.accept(this@LocalVariablesProcessor, data.copy(processingOriginalDeclarations = true))
            }
        }
    }

    override fun visitVariable(declaration: IrVariable, data: Data) {
        if (!data.processingOriginalDeclarations) return super.visitVariable(declaration, data)

        val varName = declaration.name.asString()
        val varSuffix = when {
            inlinedStack.size == 1 && !varName.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) -> INLINE_FUN_VAR_SUFFIX
            else -> ""
        }
        val newName = when {
            varSuffix.isNotEmpty() && varName == SpecialNames.THIS.asStringStripSpecialMarkers() -> "this_"
            else -> varName
        }
        declaration.name = Name.identifier(newName + varSuffix)
        super.visitVariable(declaration, data)
    }
}

private class FunctionParametersProcessor : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitBlock(expression: IrBlock) {
        if (expression !is IrInlinedFunctionBlock) {
            return super.visitBlock(expression)
        }

        super.visitBlock(expression)
        expression.getAdditionalStatementsFromInlinedBlock().forEach {
            it.processFunctionParameter(expression)
        }
    }

    private fun IrStatement.processFunctionParameter(inlinedBlock: IrInlinedFunctionBlock) {
        if (this !is IrVariable || !this.isTmpForInline) return

        val varName = this.name.asString().substringAfterLast("_")
        val varNewName = when {
            this.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER -> {
                val functionName = (inlinedBlock.inlineDeclaration as? IrDeclarationWithName)?.name
                functionName?.let { name -> "\$this$$name" } ?: AsmUtil.RECEIVER_PARAMETER_NAME
            }
            varName == SpecialNames.THIS.asStringStripSpecialMarkers() -> "this_"
            else -> varName
        }
        this.name = Name.identifier(varNewName + INLINE_FUN_VAR_SUFFIX)
        this.origin = IrDeclarationOrigin.DEFINED
    }
}
