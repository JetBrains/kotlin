/*
 * Copyright 2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.inlineDeclaration
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.INLINE_FUN_VAR_SUFFIX
import org.jetbrains.kotlin.codegen.inline.addScopeInfo
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.inline.getInlineScopeInfo
import org.jetbrains.kotlin.config.JVMConfigurationKeys.USE_INLINE_SCOPES_NUMBERS
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Adds fake locals to identify the range of inlined functions and lambdas, into already inlined blocks.
 */
@PhaseDescription(name = "FakeLocalVariablesForIrInlinerLowering")
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
        if (!context.config.enableIrInliner) return

        irFile.accept(this, null)
        if (context.configuration.getBoolean(USE_INLINE_SCOPES_NUMBERS)) {
            irFile.acceptVoid(ScopeNumberVariableProcessor())
        } else {
            irFile.accept(LocalVariablesProcessor(), LocalVariablesProcessor.Data(processingOriginalDeclarations = false))
            irFile.acceptVoid(FunctionParametersProcessor())
        }
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

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
        when {
            inlinedBlock.isFunctionInlining() -> handleInlineFunction(inlinedBlock)
            inlinedBlock.isLambdaInlining() -> handleInlineLambda(inlinedBlock)
            else -> super.visitInlinedFunctionBlock(inlinedBlock)
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
            val position = this@addFakeLocalVariable.getTmpVariablesForArguments().size
            this@addFakeLocalVariable.statements.add(position, tmpVar)
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

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: Data) {
        if (inlinedBlock.isLambdaInlining()) {
            val argument = inlinedBlock.inlinedElement as IrAttributeContainer
            val callee = inlinedStack.extractDeclarationWhereGivenElementWasInlined(argument)
            if (callee == null || callee != inlinedStack.lastOrNull()) return
        }

        super.visitInlinedFunctionBlock(inlinedBlock, data)

        inlinedBlock.insertInStackAndProcess {
            getOriginalStatementsFromInlinedBlock().forEach {
                it.accept(this@LocalVariablesProcessor, data.copy(processingOriginalDeclarations = true))
            }
        }
    }

    override fun visitVariable(declaration: IrVariable, data: Data) {
        if (!data.processingOriginalDeclarations) return super.visitVariable(declaration, data)

        val varName = declaration.name.asString()
        val varSuffix = when {
            inlinedStack.size == 1 && !varName.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) ->
                INLINE_FUN_VAR_SUFFIX
            else ->
                ""
        }
        val newName = when {
            varSuffix.isNotEmpty() && varName == SpecialNames.THIS.asStringStripSpecialMarkers() ->
                AsmUtil.INLINE_DECLARATION_SITE_THIS
            else ->
                varName
        }
        declaration.name = Name.identifier(newName + varSuffix)
        super.visitVariable(declaration, data)
    }
}

private class FunctionParametersProcessor : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
        super.visitInlinedFunctionBlock(inlinedBlock)
        inlinedBlock.getTmpVariablesForArguments().forEach {
            it.processFunctionParameter(inlinedBlock)
        }
    }

    private fun IrStatement.processFunctionParameter(inlinedBlock: IrInlinedFunctionBlock) {
        if (this !is IrVariable || !this.isTmpForInline) return

        this.name = Name.identifier(calculateNewName(inlinedBlock) + INLINE_FUN_VAR_SUFFIX)
        this.origin = IrDeclarationOrigin.DEFINED
    }
}

private class ScopeNumberVariableProcessor : IrElementVisitorVoid {
    private val inlinedStack = mutableListOf<Pair<IrInlinedFunctionBlock, Int>>()
    private var lastInlineScopeNumber = 0

    private inline fun IrInlinedFunctionBlock.insertInStackAndProcess(block: IrInlinedFunctionBlock.() -> Unit) {
        lastInlineScopeNumber += 1
        inlinedStack += Pair(this, lastInlineScopeNumber)
        block()
        inlinedStack.removeLast()
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        val processor = ScopeNumberVariableProcessor()
        declaration.acceptChildrenVoid(processor)
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
        inlinedBlock.insertInStackAndProcess {
            super.visitInlinedFunctionBlock(inlinedBlock)
        }
    }

    override fun visitVariable(declaration: IrVariable) {
        if (inlinedStack.isEmpty()) {
            return super.visitVariable(declaration)
        }

        val (inlinedBlock, scopeNumber) = inlinedStack.last()
        val newName = declaration.calculateNewName(inlinedBlock)
        declaration.name = Name.identifier(addInlineScopeInfo(newName, scopeNumber))
        super.visitVariable(declaration)
    }
}

private fun IrVariable.calculateNewName(inlinedBlock: IrInlinedFunctionBlock): String {
    val varName = name.asString()
    return when {
        this.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER ->
            inlinedBlock.getReceiverParameterName()
        varName == SpecialNames.THIS.asStringStripSpecialMarkers() ->
            AsmUtil.INLINE_DECLARATION_SITE_THIS
        else ->
            varName
    }
}

private fun addInlineScopeInfo(name: String, scopeNumber: Int): String {
    val nameWithScopeNumber = name.addScopeInfo(scopeNumber)
    if (JvmAbi.isFakeLocalVariableForInline(name)) {
        // During IR inlining we can't fetch call site line numbers because the line number mapping
        // has not been calculated yet. To keep the inline scope info format consistent, we will add
        // a mock call site line number instead, which will be replaced with the real one during the
        // expression codegen phase.
        val nameWithCallSiteLineNumber = nameWithScopeNumber.addScopeInfo(0)
        if (name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)) {
            val surroundingScopeNumber = name.getInlineScopeInfo()?.surroundingScopeNumber ?: 0
            return nameWithCallSiteLineNumber.addScopeInfo(surroundingScopeNumber)
        }
        return nameWithCallSiteLineNumber
    }
    return nameWithScopeNumber
}

private fun IrInlinedFunctionBlock.getReceiverParameterName(): String {
    val functionName = (inlineDeclaration as? IrDeclarationWithName)?.name
    return functionName?.let { "this$$it" } ?: AsmUtil.RECEIVER_PARAMETER_NAME
}

private fun List<IrInlinedFunctionBlock>.extractDeclarationWhereGivenElementWasInlined(inlinedElement: IrElement): IrDeclaration? {
    fun IrAttributeContainer.unwrapInlineLambdaIfAny(): IrAttributeContainer = when (this) {
        is IrBlock -> (statements.lastOrNull() as? IrAttributeContainer)?.unwrapInlineLambdaIfAny() ?: this
        is IrFunctionReference -> takeIf { it.origin == IrStatementOrigin.INLINE_LAMBDA } ?: this
        else -> this
    }

    val originalInlinedElement = ((inlinedElement as? IrAttributeContainer)?.attributeOwnerId ?: inlinedElement)
    for (block in this.filter { it.isFunctionInlining() }) {
        block.inlineCall!!.getAllArgumentsWithIr().forEach {
            // pretty messed up thing, this is needed to get the original expression that was inlined
            // it was changed a couple of times after all lowerings, so we must get `attributeOwnerId` to ensure that this is original
            val actualArg = if (it.second == null) {
                val blockWithClass = it.first.defaultValue?.expression?.attributeOwnerId as? IrBlock
                blockWithClass?.statements?.firstOrNull() as? IrClass
            } else {
                it.second?.unwrapInlineLambdaIfAny()
            }

            val originalActualArg = actualArg?.attributeOwnerId as? IrExpression
            val extractedAnonymousFunction = if (originalActualArg?.isAdaptedFunctionReference() == true) {
                (originalActualArg as IrBlock).statements.last() as IrFunctionReference
            } else {
                originalActualArg
            }

            if (extractedAnonymousFunction?.attributeOwnerId == originalInlinedElement) {
                return block.inlineDeclaration
            }
        }
    }

    return null
}
